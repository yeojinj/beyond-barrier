using Tizen.Applications;
using Tizen;
using Tizen.Network.Bluetooth;
using System.Threading.Tasks;
using System.Collections.Generic;
using System.Diagnostics;
using System;
using System.Text;
using System.Reflection.PortableExecutable;
using System.IO;
using System.Net.Http;
using System.Text.Json;
using System.Security.Cryptography.X509Certificates;

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

            
        }

        protected override void OnAppControlReceived(AppControlReceivedEventArgs e)
        {
            base.OnAppControlReceived(e);
            BeyondBarrierBleServer BBBServer = new BeyondBarrierBleServer();
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
            const string CaptioningServiceUuid = "00005555-0000-1000-8000-00805f9b34fb";
            const string CaptioningCharacteristicUuid = "00001234-0000-1000-8000-00805f9b34fb";
            const string CaptioningCharacteristicValue = "initValue";
            const string CaptioningDescriptorUuid = "00005678-0000-1000-8000-00805f9b34fb";
            public string CaptionResultString = "empty result";
            public BluetoothGattServer GattServer;
            public BluetoothGattService CaptioningService;
            public BluetoothGattCharacteristic CaptioningCharacteristic;
            public BluetoothGattDescriptor CaptioningDescriptor;
            public BluetoothLeAdvertiser BleAdvertiser;
            public BluetoothLeAdvertiseData BleAdvertiseData;
            public string ClientAddress = null;

            public BeyondBarrierBleServer()
            {
                // Init GattServer, Creating Service and Characteristic
                GattServer = BluetoothGattServer.CreateServer();
               
                CaptioningService = new BluetoothGattService(CaptioningServiceUuid, BluetoothGattServiceType.Primary);
                CaptioningCharacteristic = new BluetoothGattCharacteristic(CaptioningCharacteristicUuid,
                    BluetoothGattPermission.Read,
                    BluetoothGattProperty.Read
                    | BluetoothGattProperty.Notify,
                    Encoding.Default.GetBytes(CaptioningCharacteristicValue));
                CaptioningDescriptor = new BluetoothGattDescriptor(
                    CaptioningDescriptorUuid,
                    BluetoothGattPermission.Read,
                    new byte[1] { 0x01 });

                CaptioningCharacteristic.AddDescriptor(CaptioningDescriptor);
                CaptioningService.AddCharacteristic(CaptioningCharacteristic);
                GattServer.RegisterGattService(CaptioningService);

                // adding Callback functions
                CaptioningDescriptor.ReadRequested += DescriptorReadRequestedCB;
                CaptioningDescriptor.WriteRequested += DescriptorWriteRequestedCB;
                CaptioningCharacteristic.ReadRequested += ReadRequestedCB;
                CaptioningCharacteristic.WriteRequested += WriteRequestedCB;
                CaptioningCharacteristic.ValueChanged += CharacteristicValueChangedCB;
                CaptioningCharacteristic.NotificationStateChanged += NotificationStateChangedCB;
                GattServer.NotificationSent += NotificationSentCB;

               

                // setting Advertising options
                BleAdvertiser = BluetoothAdapter.GetBluetoothLeAdvertiser();
                
                BleAdvertiseData = new BluetoothLeAdvertiseData();
                BleAdvertiseData.AdvertisingConnectable = true;
                BleAdvertiseData.IncludeDeviceName = true;
                BleAdvertiseData.AdvertisingMode = BluetoothLeAdvertisingMode.BluetoothLeAdvertisingBalancedMode;
                BleAdvertiseData.IncludeTxPowerLevel = false;

                BleAdvertiseData.AddAdvertisingServiceUuid(BluetoothLePacketType.BluetoothLeScanResponsePacket, CaptioningServiceUuid);
                BleAdvertiser.AdvertisingStateChanged += AdvertisingStateChangedCB;

                GattServer.Start();
                Log.Info("BB_check", "Bluetooth GATT Server Started");

                BleAdvertiser.StartAdvertising(BleAdvertiseData);
                Log.Info("BB_check", "Bluetooth Advertise Started");
                
            }

            public async void ReadRequestedCB (object sender, ReadRequestedEventArgs e)
            {
                Log.Info("BB_check", "character read requested");
                Log.Info("BB_check", e.ClientAddress);
                e.Server.SendResponse(
                    e.RequestId,
                    BluetoothGattRequestType.Read,
                    0,
                    e.Server.GetService(CaptioningServiceUuid).GetCharacteristic(CaptioningCharacteristicUuid).Value,
                    e.Offset);

                //Manual Test
                // await HttpRequestTest();

                //Manual Test
                // await NotiTest(e);

                // Change Characteristic's Value
                // Send Notification to connected Device
            }

            public void WriteRequestedCB (object sender, WriteRequestedEventArgs e)
            {
                Log.Info("BB_check", "characteristic write requested");
                CaptioningCharacteristic.SetValue(e.Value.ToString());
                Log.Debug("BB_check", "Characteristic Value : " + CaptioningCharacteristic.Value);
                e.Server.SendResponse(
                    e.RequestId,
                    BluetoothGattRequestType.Write,
                    0,
                    e.Server.GetService(CaptioningServiceUuid).GetCharacteristic(CaptioningCharacteristicUuid).Value,
                    e.Offset);
            }

            public void DescriptorReadRequestedCB (object sender, ReadRequestedEventArgs e)
            {
                Log.Info("BB_check", "Descriptor read requested");
                Log.Debug("BB_check", "client address : " + e.ClientAddress);
                
                e.Server.SendResponse(
                    e.RequestId,
                    BluetoothGattRequestType.Read,
                    0,
                    e.Server.GetService(CaptioningServiceUuid)
                    .GetCharacteristic(CaptioningCharacteristicUuid)
                    .GetDescriptor(CaptioningDescriptorUuid)
                    .Value,
                    e.Offset);

            }

            public void DescriptorWriteRequestedCB(object sender, WriteRequestedEventArgs e)
            {
                Log.Info("BB_check", "Descriptor write requested");
                CaptioningDescriptor.SetValue(e.Value.ToString());
                Log.Debug("BB_check", "Descriptor Value : " + CaptioningDescriptor.Value);
                e.Server.SendResponse(
                    e.RequestId,
                    BluetoothGattRequestType.Write,
                    0,
                    e.Server.GetService(CaptioningServiceUuid)
                    .GetCharacteristic(CaptioningCharacteristicUuid)
                    .GetDescriptor(CaptioningDescriptorUuid)
                    .Value,
                    e.Offset);
            }

            public void NotificationSentCB(object sender, NotificationSentEventArg e)
            {
                Log.Info("BB_check", "Notification sent from Gatt server");
            }

            public void NotificationStateChangedCB(object sender, NotificationStateChangedEventArg e)
            {
                Log.Info("BB_check", "notification state changed");
            }

            public void AdvertisingStateChangedCB(object sender, AdvertisingStateChangedEventArgs e)
            {
                Log.Info("BB_check", "advertise state changed");
            }

            public void CharacteristicValueChangedCB(object sender, ValueChangedEventArgs e)
            {
                Log.Info("BB_check", "Characteristic Value changed");
            }

            public void Dispose()
            {
                Log.Info("BB_check", "Gatt Server Terminating");
                BleAdvertiser.StopAdvertising(BleAdvertiseData);
                GattServer.UnregisterGattService(CaptioningService);
                GattServer.Dispose();
            }

            public async Task ImageCaptionRequestTest(string imgPath)
            {
                // Create an instance of HttpClient
                HttpClient client = new HttpClient();

                // Create a JSON object to represent the request body
                object requestBody = new
                {
                    deviceId = "Tizen",
                    imgPath = imgPath,
                    captureTime = "2023-03-09T13:46:11",
                };

                // Serialize the JSON object to a string
                string requestBodyJson = JsonSerializer.Serialize(requestBody);

                // Create a new StringContent object to represent the request body
                StringContent requestBodyContent = new StringContent(requestBodyJson, Encoding.UTF8, "application/json");

                // Send a POST request to the specified URL with the request body
                var response = await client.PostAsync("http://18.191.139.106:5000/api/caption", requestBodyContent);

                // Read the response content as a string
                string content = await response.Content.ReadAsStringAsync();

                CaptionResultData data = JsonSerializer.Deserialize<CaptionResultData>(content);

                CaptionResultString = data.result;

                // Dispose of the HttpClient instance
                client.Dispose();
            }

            public static void ResponseDataHandler(string res)
            {
                // divide by 20 byte length, then send notification
            }

            public async Task HttpRequestTest()
            {
                string imgPath = "http://www.econovill.com/news/photo/201807/342619_212106_2248.jpg";
                await ImageCaptionRequestTest(imgPath);
                Log.Info("BB_check", "Request completed");
                if (CaptionResultString == null)
                    Log.Debug("BB_check", "Request Response ERROR");
                else Log.Debug("BB_check", "Response Value : " + CaptionResultString);
            }

            public async Task NotiTest(ReadRequestedEventArgs e)
            {
                CaptioningCharacteristic.SetValue("Test_valueChanged");
                await Task.Delay(1000);

                e.Server.SendNotification(CaptioningCharacteristic, e.ClientAddress.ToString());

                await Task.Delay(1000);
                CaptioningCharacteristic.SetValue("initValue");
            }

            public class CaptionResultData
            {
                public string result { get; set; }
            }
        }
    }
}
