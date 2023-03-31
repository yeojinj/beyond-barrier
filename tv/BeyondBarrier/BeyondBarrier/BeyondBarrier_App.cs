using Tizen;
using Tizen.Applications;
using Tizen.Network.Bluetooth;
using Tizen.System;
using System;
using System.IO;
using System.Threading;
using System.Threading.Tasks;
using System.Collections.Generic;
using System.Drawing;
using System.Text;
using System.Net.Http;
using System.Text.Json;
using Amazon.S3;
using Amazon.S3.Transfer;
using Amazon.S3.Model;
using Amazon.Runtime;
using System.Linq;
using System.Transactions;

namespace BeyondBarrier
{
    class App : ServiceApplication
    {

        protected override void OnCreate()
        {
            base.OnCreate();
            Log.Info("BB_check", "BeyondBarrier Service Created");

            // if Bluetooth is turned on, Make BLE server object
            if (BluetoothAdapter.IsBluetoothEnabled)
            {
                BeyondBarrierBleServer BBBServer = new BeyondBarrierBleServer();
            }

        }

        protected override async void OnAppControlReceived(AppControlReceivedEventArgs e)
        {
            base.OnAppControlReceived(e);
            // If Bluetooth is off, Terminate
            
            while (BluetoothAdapter.IsBluetoothEnabled)
            {
                await Task.Delay(1000);
            }
            this.Exit();

            // AppControl을 다시 Receive할 경우가 생긴다면 위 반복문을 지워야 할 것 같습니다.
        }

        protected override void OnTerminate()
        {
            base.OnTerminate();
            Log.Info("BB_check", "BeyondBarrier is Terminating");
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
            public string UniqueS3FilePrefix;
            public int UniqueS3FileSuffix;

            public BeyondBarrierBleServer()
            {
                //ready for S3 keys. and S3's file name prefix/suffix
                string json = File.ReadAllText("/opt/usr/globalapps/org.tizen.example.BeyondBarrier/res/accesscode.json");
                JsonDocument doc = JsonDocument.Parse(json);
                JsonElement root = doc.RootElement;
                S3AccessKey = root.GetProperty("AccessKey").GetString();
                S3SecretKey = root.GetProperty("SecretKey").GetString();
                UniqueS3FilePrefix = DateTime.Now.ToString("yyyyMMddhhmmss");
                UniqueS3FileSuffix = 1;

                if (S3AccessKey == null || S3SecretKey == null)
                {
                    Log.Error("BB_check", "There is a problem with S3 Keys. Terminating");
                    return;
                }

                BluetoothAdapter.StateChanged += StateChangedCB;

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

                //start BLE Gatt Server, then starts low energy advertising
                GattServer.Start();
                Log.Info("BB_check", "Bluetooth GATT Server Started");

                BleAdvertiser.StartAdvertising(BleAdvertiseData);
                Log.Info("BB_check", "Bluetooth Advertise Started");
            }

            public async void CaptionSignalRequestedCB(object sender, ReadRequestedEventArgs e)
            {
                //explain : When Android request for Image captioning, this CallBack function is invoked
                //first, response for request received
                //capture screen's image (using 'ScreenCapture')
                //upload image to S3 (using 'S3ImageUpload')
                //http request for image captioning using S3's image address (using 'ImageCaptionRequest')
                //set characteristic's value by request's response

                Log.Info("BB_check", "Captioning Signal requested");
                Log.Debug("BB_check", "Client : "+ e.ClientAddress);
                e.Server.SendResponse(
                    e.RequestId,
                    BluetoothGattRequestType.Read,
                    0,
                    e.Server.GetService(BBGattServiceUuid).GetCharacteristic(CaptionRequestUuid).Value,
                    e.Offset);
                Log.Info("BB_check", "Captioning Signal response sent");

                CaptionValue.SetValue("-1");

                /////////////////////////////////////////
                //string CaptureImagePath = await ScreenCapture("capture");
                string CaptureImagePath = @"/opt/usr/globalapps/org.tizen.example.BeyondBarrier/res/savedPics/capture.jpg";
                //ScreenCapture 함수 제작하시고 나서 위 줄은 지워주시고,
                //string CaptureImagePath = await ScreenCapture("capture");의 주석을 해제해주세요.
                /////////////////////////////////////////

                //upload Image file to S3
                Log.Info("BB_check", "S3 upload function started");

                string S3ImagePath = await S3ImageUpload(CaptureImagePath);
                if (S3ImagePath.Equals("error"))
                {
                    Log.Error("BB_check", "Image upload failed");
                    return;
                }

                Log.Info("BB_check", "Image caption function started");
                string captionResult = await ImageCaptionRequest(S3ImagePath);
                Log.Info("BB_check", "Image Caption Request completed");
                if (captionResult.Equals("error") || captionResult == null)
                {
                    Log.Error("BB_check", "Caption Request Response ERROR");
                    return;
                }

                Log.Debug("BB_check", "Response Value : " + captionResult);
                CaptionValue.SetValue(captionResult);

            }

            public void CaptionValueRequestedCB(object sender, ReadRequestedEventArgs e)
            {
                //explain : When Android requests for image captioning, then it periodically request for value
                //this Callback function send response for characteristic's value
                //value changes from -1, to a positive number that means Database's index

                Log.Info("BB_check", "Captioning Index Value requested");
                Log.Debug("BB_check", "Client : " + e.ClientAddress);
                e.Server.SendResponse(
                    e.RequestId,
                    BluetoothGattRequestType.Read,
                    0,
                    CaptionValue.Value,
                    e.Offset);
            }

            public async void ProgramSignalRequestedCB(object sender, ReadRequestedEventArgs e)
            {
                //explain : When Android request for Program info by logo detection, this CallBack function is invoked
                //first, response for request received
                //capture screen's image (using 'ScreenCapture')
                //upload image to S3 (using 'S3ImageUpload')
                //http request for Program info using S3's image address (using 'ProgramInfoRequest)
                //set characteristic's value by request's response

                Log.Info("BB_check", "Program Signal requested");
                Log.Debug("BB_check", "Client : " + e.ClientAddress);
                e.Server.SendResponse(
                    e.RequestId,
                    BluetoothGattRequestType.Read,
                    0,
                    ProgramRequest.Value,
                    e.Offset);

                ProgramValue.SetValue("-1");

                
                //////////////////////////////////
                string ProgramInfoImagePath = @"/opt/usr/globalapps/org.tizen.example.BeyondBarrier/res/savedPics/programInfo.png";
                //ScreenCapture 만들어 주신 후 위 줄은 삭제해주시고,
                //string ProgramInfoImagePath = await ScreenCapture("programInfo");
                //위 코드를 주석 해제해주시면 됩니다.
                //////////////////////////////////

                //upload Image file to S3
                string S3ImagePath = await S3ImageUpload(ProgramInfoImagePath);
                if (S3ImagePath.Equals("error"))
                {
                    Log.Error("BB_check", "Image upload failed");
                    return;
                }
                
                string ProgramInfoResult = await ProgramInfoRequest(S3ImagePath);
                Log.Info("BB_check", "Request completed");
                if (ProgramInfoResult.Equals("error") || ProgramInfoResult == null)
                {
                    Log.Error("BB_check", "Program Request Response ERROR");
                    return;
                }
                Log.Debug("BB_check", "Response Value : " + ProgramInfoResult);
                ProgramValue.SetValue(ProgramInfoResult);

            }

            public void ProgramValueRequestedCB(object sender, ReadRequestedEventArgs e)
            {
                //explain : When Android requests for Program info by logo detection, then it periodically request for value
                //this Callback function send response for characteristic's value
                //value changes from -1, to a positive number that means Database's index

                Log.Info("BB_check", "Program Value requested");
                Log.Debug("BB_check", "Client : " + e.ClientAddress);
                e.Server.SendResponse(
                    e.RequestId,
                    BluetoothGattRequestType.Read,
                    0,
                    ProgramValue.Value,
                    e.Offset);
            }

            public void AdvertisingStateChangedCB(object sender, AdvertisingStateChangedEventArgs e)
            {
                //callback function for LeAdvertiser
                Log.Info("BB_check", "advertise state changed");
                
            }

            public void StateChangedCB(object sender, StateChangedEventArgs e)
            {
                //Callback function for BluetoothAdapter, and terminating by status
                Log.Info("BB_check", "Bluetooth adapter state changed");
                Log.Debug("BB_check", "Bluetooth state : " + e.BTState.ToString());
                Log.Debug("BB_check", "Bluetooth error : " + e.Result.ToString());
                if(e.BTState == BluetoothState.Disabled)
                {
                    this.Dispose();
                }
            }

            private async Task<string> S3ImageUpload(string localFilePath)
            {
                //explain : methods that uploads Image file to S3 server
                //using AWSSDK for S3, make object for request and use 'PutObjectAsync' to send upload request
                //returns full S3's image path to use it for image captioning or logo detection

                var s3Client = new AmazonS3Client(S3AccessKey, S3SecretKey, Amazon.RegionEndpoint.APNortheast2);
                string[] fileNameSplit = localFilePath.Split('/').Last().Split('.');
                string fileName = fileNameSplit[0] + UniqueS3FileSuffix + "." +fileNameSplit[1];
                string objKey = "capture/" + UniqueS3FilePrefix + fileName;
                UniqueS3FileSuffix += 1;
                string bucketDataAccessPath = "https://beyondb-bucket.s3.ap-northeast-2.amazonaws.com/";
                var request = new PutObjectRequest
                {
                    BucketName = "beyondb-bucket",
                    Key = objKey,
                    FilePath = localFilePath
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

            public async Task<string> ImageCaptionRequest(string imgPath)
            {
                //explain : method that sends a http request
                //returns response content string


                HttpClient client = new HttpClient();
                
                string responseContent = "";
                object requestBody = new
                {
                    deviceId = BluetoothAdapter.Name,
                    imgPath = imgPath
                };

                string requestBodyJson = JsonSerializer.Serialize(requestBody);

                StringContent requestBodyContent = new StringContent(requestBodyJson, Encoding.UTF8, "application/json");

                Log.Info("BB_check", "Image Caption http request send started");
                var response = await client.PostAsync("http://18.191.139.106:5000/api/caption", requestBodyContent);

                Log.Info("BB_check", "Image Caption http response received");

                if (response.StatusCode == System.Net.HttpStatusCode.OK)
                {
                    responseContent = await response.Content.ReadAsStringAsync();
                }
                else
                {
                    responseContent = "error";
                }
                
                client.Dispose();

                return responseContent;
            }

            public async Task<string> ProgramInfoRequest(string imgPath)
            {
                //explain : method that sends a http request
                //returns response content string

                HttpClient client = new HttpClient();
                string responseContent = "";
                object requestBody = new
                {
                    deviceId = BluetoothAdapter.Name,
                    imgPath = imgPath
                };

                string requestBodyJson = JsonSerializer.Serialize(requestBody);
                StringContent requestBodyContent = new StringContent(requestBodyJson, Encoding.UTF8, "application/json");

                Log.Info("BB_check", "Program Info http request send started");
                var response = await client.PostAsync("http://18.191.139.106:5000/api/program", requestBodyContent);
                if (response.StatusCode == System.Net.HttpStatusCode.OK)
                {
                    responseContent = await response.Content.ReadAsStringAsync();
                }
                else
                {
                    responseContent = "error";
                }

                client.Dispose();

                return responseContent;
            }

            public void Dispose()
            {
                Log.Info("BB_check", "Gatt Server Terminating");
                CaptionRequest.ReadRequested -= CaptionSignalRequestedCB;
                CaptionValue.ReadRequested -= CaptionValueRequestedCB;
                ProgramRequest.ReadRequested -= ProgramSignalRequestedCB;
                ProgramValue.ReadRequested -= ProgramValueRequestedCB;
                BluetoothAdapter.StateChanged -= StateChangedCB;
                //BleAdvertiser.StopAdvertising(BleAdvertiseData);
                //블루투스가 꺼진다면 위 메소드를 실행 불가능. - ??
                BleAdvertiseData.Dispose();
                GattServer.UnregisterGattService(BBGattService);
                GattServer.Dispose();
            }

            public async Task<string> ScreenCapture(string fileName)
            {
                //스크린 캡쳐함수
                //사용 목적: 안드로이드로부터 요청이 왔을 시, 이미지 파일 저장
                //매개변수: string, 확장자와 경로 제외한 파일명
                //사진 파일 이름 형식: "capture" / "programInfo" 가 들어가게 될 예정
                //사진 파일 확장자: .jpg (바꾸셔도 됩니다.)
                //사진 파일 저장 위치: 앱 설치 위치 내부의 /res/savedPics/

                //capture 라면 /opt/usr/globalapps/org.tizen.example.BeyondBarrier/res/savedPics/capture.jpg
                //programInfo 라면 /opt/usr/globalapps/org.tizen.example.BeyondBarrier/res/savedPics/programInfo.jpg
                //를 저장하려 합니다.
                //TV에서 경로가 달라진다면 앱이 설치된 위치 내부의 /res/savedPics/ 로 upperDirectory를 변경해주시면 됩니다.

                //각 요청마다 매 번 같은 파일 이름으로, Write 하여 새 파일로 덮어 써 주시면 될 것 같습니다
                // - 저장공간을 아낄 수 있고, 매 번 새로운 이름 생성을 위한 연산이 필요 없는 장점이 있습니다.
                // 

                string upperDirectory = "/opt/usr/globalapps/org.tizen.example.BeyondBarrier/res/savedPics/";
                string extension = ".jpg";
                
                string filePath = 
                    upperDirectory
                    + fileName
                    + extension;

                //캡쳐 함수 구현부
                
                return filePath;
            }
        }
    }
}
