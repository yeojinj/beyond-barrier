using Tizen;
using Tizen.Applications;
using Tizen.Network.Bluetooth;
using Tizen.System;
using System;
using System.IO;
using System.Threading.Tasks;
using System.Collections.Generic;
using System.Text;
using System.Net.Http;
using System.Text.Json;
using Amazon.S3;
using Amazon.S3.Transfer;
using Amazon.S3.Model;
using Amazon.Runtime;
using System.Linq;

namespace BeyondBarrier
{
    class App : ServiceApplication
    {
        protected override void OnCreate()
        {
            base.OnCreate();
            Log.Info("BB_check", "BeyondBarrier Service Created");

            // If Bluetooth is off, Terminate
            bool BluetoothEnabled = false;
            BluetoothEnabled = BluetoothAdapter.IsBluetoothEnabled;
            if (!BluetoothEnabled) {
                Log.Info("BB_check", "Bluetooth is not turned on. Terminating.");
                this.Exit();
            }

            BeyondBarrierBleServer BBBServer = new BeyondBarrierBleServer();
        }

        protected override void OnAppControlReceived(AppControlReceivedEventArgs e)
        {
            base.OnAppControlReceived(e);
        }

        protected override void OnTerminate()
        {
            Log.Info("BB_check", "BeyondBarrier is Terminating");
            base.OnTerminate();
        }

        static void Main(string[] args)
        {
            App app = new App();
            app.Run(args);
            app.Dispose();
            Log.Info("BB_check", "BeyondBarrier Dispose");
        }

        class BeyondBarrierBleServer : IDisposable
        {
            const string BBGattServiceUuid = "00005555-0000-1000-8000-555555555555";
            const string CaptionRequestUuid = "00ca58e9-0000-1000-8000-555555555555";
            const string CaptionValueUuid = "00ca55a1-0000-1000-8000-555555555555";
            const string ProgramRequestUuid = "00d158e9-0000-1000-8000-555555555555";
            const string ProgramValueUuid = "00d155a1-0000-1000-8000-555555555555";
            public BluetoothGattServer GattServer;
            public BluetoothGattService BBGattService;
            public BluetoothGattCharacteristic CaptionRequest;
            public BluetoothGattCharacteristic CaptionValue;
            public BluetoothGattCharacteristic ProgramRequest;
            public BluetoothGattCharacteristic ProgramValue;
            public BluetoothLeAdvertiser BleAdvertiser;
            public BluetoothLeAdvertiseData BleAdvertiseData;
            public string S3AccessKey;
            public string S3SecretKey;

            public BeyondBarrierBleServer()
            {
                //ready for S3 keys
                string json = File.ReadAllText("path/to/your/file.json");
                JsonDocument doc = JsonDocument.Parse(json);
                JsonElement root = doc.RootElement;
                S3AccessKey = root.GetProperty("AccessKey").GetString();
                S3SecretKey = root.GetProperty("SecretKey").GetString();

                if(S3AccessKey == null || S3SecretKey == null)
                {
                    Log.Error("BB_check", "There is a problem with S3 Keys. Terminating");
                    return;
                }

                // Init GattServer, Creating Service and Characteristic
                GattServer = BluetoothGattServer.CreateServer();
               
                BBGattService = new BluetoothGattService(BBGattServiceUuid, BluetoothGattServiceType.Primary);
                CaptionRequest = new BluetoothGattCharacteristic(
                    CaptionRequestUuid,
                    BluetoothGattPermission.Read,
                    BluetoothGattProperty.Read,
                    Encoding.Default.GetBytes("0"));
                CaptionValue = new BluetoothGattCharacteristic(
                    CaptionValueUuid,
                    BluetoothGattPermission.Read,
                    BluetoothGattProperty.Read,
                    Encoding.Default.GetBytes("-1"));
                ProgramRequest = new BluetoothGattCharacteristic(
                    ProgramRequestUuid,
                    BluetoothGattPermission.Read,
                    BluetoothGattProperty.Read,
                   Encoding.Default.GetBytes("0"));
                ProgramValue = new BluetoothGattCharacteristic(
                    ProgramValueUuid,
                    BluetoothGattPermission.Read,
                    BluetoothGattProperty.Read,
                    Encoding.Default.GetBytes("-1"));

                BBGattService.AddCharacteristic(CaptionRequest);
                BBGattService.AddCharacteristic(CaptionValue);
                BBGattService.AddCharacteristic(ProgramRequest);
                BBGattService.AddCharacteristic(ProgramValue);
                GattServer.RegisterGattService(BBGattService);

                // adding Callback functions
                CaptionRequest.ReadRequested += CaptionSignalRequestedCB;
                CaptionValue.ReadRequested += CaptionValueRequestedCB;
                ProgramRequest.ReadRequested += ProgramSignalRequestedCB;
                ProgramValue.ReadRequested += ProgramValueRequestedCB;

                // setting Advertising options 
                BleAdvertiser = BluetoothAdapter.GetBluetoothLeAdvertiser();
                
                BleAdvertiseData = new BluetoothLeAdvertiseData();
                BleAdvertiseData.AdvertisingConnectable = true;
                BleAdvertiseData.IncludeDeviceName = true;
                BleAdvertiseData.AdvertisingMode = BluetoothLeAdvertisingMode.BluetoothLeAdvertisingBalancedMode;
                BleAdvertiseData.IncludeTxPowerLevel = false;

                BleAdvertiseData.AddAdvertisingServiceUuid(BluetoothLePacketType.BluetoothLeScanResponsePacket, BBGattServiceUuid);
                BleAdvertiser.AdvertisingStateChanged += AdvertisingStateChangedCB;

                GattServer.Start();
                Log.Info("BB_check", "Bluetooth GATT Server Started");

                BleAdvertiser.StartAdvertising(BleAdvertiseData);
                Log.Info("BB_check", "Bluetooth Advertise Started");            
            }

            public async void CaptionSignalRequestedCB(object sender, ReadRequestedEventArgs e)
            {
                Log.Info("BB_check", "Captioning Signal requested");
                Log.Info("BB_check", "Client : "+ e.ClientAddress);
                e.Server.SendResponse(
                    e.RequestId,
                    BluetoothGattRequestType.Read,
                    0,
                    e.Server.GetService(BBGattServiceUuid).GetCharacteristic(CaptionRequestUuid).Value,
                    e.Offset);

                CaptionValue.SetValue("-1");

                //await Screen_Capture
                //string sampleImagePath = ScreenCapture();
                string sampleImagePath = @"/opt/usr/globalapps/org.tizen.example.BeyondBarrier/res/savedPics/ddongkae.jpg";
                
                //upload Image file to S3
                
                string S3ImagePath = await S3ImageUpload(sampleImagePath);
                if (S3ImagePath.Equals("error"))
                {
                    Log.Error("BB_check", "Image upload failed");
                    return;
                }
                                
                string captionResult = await ImageCaptionRequest(S3ImagePath, DateTime.Now.ToString("yyyy-MM-ddThh:mm:ss"));
                Log.Info("BB_check", "Request completed");
                if (captionResult.Equals("error") || captionResult == null)
                {
                    Log.Debug("BB_check", "Caption Request Response ERROR");
                    return;
                }

                Log.Debug("BB_check", "Response Value : " + captionResult);
                CaptionValue.SetValue(captionResult);

            }

            public void CaptionValueRequestedCB(object sender, ReadRequestedEventArgs e)
            {
                Log.Info("BB_check", "Captioning Index Value requested");
                Log.Info("BB_check", "Client : " + e.ClientAddress);
                e.Server.SendResponse(
                    e.RequestId,
                    BluetoothGattRequestType.Read,
                    0,
                    CaptionValue.Value,
                    e.Offset);
            }

            public async void ProgramSignalRequestedCB(object sender, ReadRequestedEventArgs e)
            {
                Log.Info("BB_check", "Program Signal requested");
                Log.Info("BB_check", "Client : " + e.ClientAddress);
                e.Server.SendResponse(
                    e.RequestId,
                    BluetoothGattRequestType.Read,
                    0,
                    ProgramRequest.Value,
                    e.Offset);

                ProgramValue.SetValue("-1");

                //await Screen_Capture
                //string sampleImagePath = ScreenCapture();

                string sampleImagePath = @"/opt/usr/globalapps/org.tizen.example.BeyondBarrier/res/savedPics/gian84.png";

                //upload Image file to S3
                string S3ImagePath = await S3ImageUpload(sampleImagePath);
                if (S3ImagePath.Equals("error"))
                {
                    Log.Error("BB_check", "Image upload failed");
                    return;
                }
                
                string ProgramInfoResult = await ProgramInfoRequest(S3ImagePath, DateTime.Now.ToString("yyyy-MM-ddThh:mm:ss"));
                Log.Info("BB_check", "Request completed");
                if (ProgramInfoResult.Equals("error") || ProgramInfoResult == null)
                {
                    Log.Debug("BB_check", "Program Request Response ERROR");
                    return;
                }
                Log.Debug("BB_check", "Response Value : " + ProgramInfoResult);
                ProgramValue.SetValue(ProgramInfoResult);

            }

            public void ProgramValueRequestedCB(object sender, ReadRequestedEventArgs e)
            {
                Log.Info("BB_check", "Program Value requested");
                Log.Info("BB_check", "Client : " + e.ClientAddress);
                e.Server.SendResponse(
                    e.RequestId,
                    BluetoothGattRequestType.Read,
                    0,
                    ProgramValue.Value,
                    e.Offset);
            }

            public void AdvertisingStateChangedCB(object sender, AdvertisingStateChangedEventArgs e)
            {
                Log.Info("BB_check", "advertise state changed");
            }

            public void Dispose()
            {
                Log.Info("BB_check", "Gatt Server Terminating");
                BleAdvertiser.StopAdvertising(BleAdvertiseData);
                GattServer.UnregisterGattService(BBGattService);
                GattServer.Dispose();
            }

            private async Task<string> S3ImageUpload(string localFilePath)
            {
                var credentials = new BasicAWSCredentials(S3AccessKey, S3SecretKey);
                var s3Client = new AmazonS3Client(credentials, Amazon.RegionEndpoint.APNortheast2);

                var filePath = localFilePath;
                var fileData = File.ReadAllBytes(filePath);
                string objKey = "capture/" + DateTime.Now.ToString("yyyyMMddhhmmss") + localFilePath.Split('/').Last();
                string bucketDataAccessPath = "https://beyondb-bucket.s3.ap-northeast-2.amazonaws.com/";
                //string s3uploadPath
                var request = new PutObjectRequest
                {
                    BucketName = "beyondb-bucket",
                    Key = objKey,
                    InputStream = new MemoryStream(fileData)
                };

                PutObjectResponse response = await s3Client.PutObjectAsync(request);

                if(response.HttpStatusCode == System.Net.HttpStatusCode.OK)
                {
                    Log.Info("BB_check", "S3 upload successful");
                    return bucketDataAccessPath + objKey;
                }

                Log.Error("BB_check", "S3 upload Error : " + response.HttpStatusCode.ToString());

                return "error";
            }

            public async Task<string> ImageCaptionRequest(string imgPath, string captureTime)
            {
                // Create an instance of HttpClient
                HttpClient client = new HttpClient();
                
                string responseContent = "";

                // Create a JSON object to represent the request body
                object requestBody = new
                {
                    deviceId = BluetoothAdapter.Name,
                    imgPath = imgPath,
                    captureTime = captureTime,
                };

                // Serialize the JSON object to a string
                string requestBodyJson = JsonSerializer.Serialize(requestBody);

                // Create a new StringContent object to represent the request body
                StringContent requestBodyContent = new StringContent(requestBodyJson, Encoding.UTF8, "application/json");

                // Send a POST request to the specified URL with the request body
                var response = await client.PostAsync("http://18.191.139.106:5000/api/caption", requestBodyContent);

                if (response.StatusCode.Equals(200))
                {
                    // Read the response content as a string
                    responseContent = await response.Content.ReadAsStringAsync();
                }
                else
                {
                    responseContent = "error";
                }
                
                // Dispose of the HttpClient instance
                client.Dispose();

                return responseContent;
            }

            public async Task<string> ProgramInfoRequest(string imgPath, string captureTime)
            {
                // Create an instance of HttpClient
                HttpClient client = new HttpClient();

                string responseContent = "";

                // Create a JSON object to represent the request body
                object requestBody = new
                {
                    deviceId = BluetoothAdapter.Name,
                    imgPath = imgPath,
                    captureTime = captureTime,
                };

                // Serialize the JSON object to a string
                string requestBodyJson = JsonSerializer.Serialize(requestBody);

                // Create a new StringContent object to represent the request body
                StringContent requestBodyContent = new StringContent(requestBodyJson, Encoding.UTF8, "application/json");

                // Send a POST request to the specified URL with the request body
                var response = await client.PostAsync("http://18.191.139.106:5000/api/program", requestBodyContent);

                if (response.StatusCode.Equals(200))
                {
                    // Read the response content as a string
                    responseContent = await response.Content.ReadAsStringAsync();
                }
                else
                {
                    responseContent = "error";
                }

                // Dispose of the HttpClient instance
                client.Dispose();

                return responseContent;
            }

            public string ScreenCapture()
            {
                //스크린 캡쳐함수
                //
                return "";
            }
        }
    }
}
