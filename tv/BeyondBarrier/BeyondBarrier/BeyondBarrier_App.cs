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

            //Server Starts when created
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
            const string CaptioningServiceUuid = "00005555-0000-1000-8000-00805f9b34fb";
            const string CaptioningCharacteristicUuid = "00001234-0000-1000-8000-00805f9b34fb";
            const string CaptioningCharacteristicValue = "captionCharacter";
            const string CaptioningDescriptorUuid = "00005678-0000-1000-8000-00805f9b34fb";
            const string CaptioningDescriptorValue = "abcde";
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
                    BluetoothGattPermission.Read | BluetoothGattPermission.Write,
                    BluetoothGattProperty.Read | BluetoothGattProperty.Write | BluetoothGattProperty.Notify,
                    Encoding.Default.GetBytes(CaptioningCharacteristicValue));
                CaptioningDescriptor = new BluetoothGattDescriptor(
                    CaptioningDescriptorUuid,
                    BluetoothGattPermission.Read | BluetoothGattPermission.Write,
                    Encoding.Default.GetBytes(CaptioningDescriptorValue));

                CaptioningCharacteristic.AddDescriptor(CaptioningDescriptor);
                CaptioningService.AddCharacteristic(CaptioningCharacteristic);
                GattServer.RegisterGattService(CaptioningService);

                // adding Callback functions
                CaptioningDescriptor.ReadRequested += DescriptorReadRequestedCB;
                CaptioningDescriptor.WriteRequested += DescriptorWriteRequestedCB;
                CaptioningCharacteristic.ReadRequested += ReadRequestedCB;
                CaptioningCharacteristic.WriteRequested += WriteRequestedCB;
                CaptioningCharacteristic.NotificationStateChanged += NotificationStateChangedCB;

                GattServer.Start();
                Log.Info("BB_check", "Bluetooth GATT Server Started");

                // setting Advertising options
                BleAdvertiser = BluetoothAdapter.GetBluetoothLeAdvertiser();
                BleAdvertiser.AdvertisingStateChanged += AdvertisingStateChangedCB;
                BleAdvertiseData = new BluetoothLeAdvertiseData();
                BleAdvertiseData.AdvertisingConnectable = true;
                BleAdvertiseData.IncludeDeviceName = true;
                BleAdvertiseData.AdvertisingMode = BluetoothLeAdvertisingMode.BluetoothLeAdvertisingBalancedMode;

                BleAdvertiseData.AddAdvertisingServiceUuid(BluetoothLePacketType.BluetoothLeScanResponsePacket, CaptioningServiceUuid);

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

                await Task.Delay(1000);
                CaptioningCharacteristic.SetValue("Test_valueChanged");
                e.Server.SendNotification(CaptioningCharacteristic, e.ClientAddress);
                await Task.Delay(1000);
                CaptioningCharacteristic.SetValue("captionCharacter");
                //ImageCaptionRequest();
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
                GattServer.NotificationSent -= NotificationSentCB;
                Log.Info("BB_check", "Notification sent from Gatt server");
                Log.Debug("BB_check", e.ClientAddress);
            }

            public async void NotificationStateChangedCB(object sender, NotificationStateChangedEventArg e)
            {
                Log.Info("BB_check", "notification state changed");
                Log.Debug("BB_check", e.Value.ToString());
                GattServer.NotificationSent += NotificationSentCB;

                CaptioningCharacteristic.SetValue("Caption Response");
                await e.Server.SendIndicationAsync(CaptioningCharacteristic, null);
                Log.Debug("BB_check", "noti sent");

                CaptioningCharacteristic.SetValue("012345678901234567890");
            }

            public void AdvertisingStateChangedCB(object sender, AdvertisingStateChangedEventArgs e)
            {
                Log.Info("BB_check", "advertise state changed");
            }
            public void Dispose()
            {
                Log.Info("BB_check", "Gatt Server Terminating");
                BleAdvertiser.StopAdvertising(BleAdvertiseData);
                GattServer.UnregisterGattService(CaptioningService);
                GattServer.Dispose();
            }

            public async void ImageCaptionRequest()
            {
                using (var client = new HttpClient())
                {
                    var content = new
                    {
                        deviceId = "Tizen",
                        imgPath = "http://www.econovill.com/news/photo/201807/342619_212106_2248.jpg",
                        captureTime = "2023-03-09T13:46:11"
                    };
                    var json = JsonSerializer.Serialize(content);
                    var httpContent = new StringContent(json, Encoding.UTF8, "application/json");
                    var response = await client.PostAsync("", httpContent);
                    var responseString = await response.Content.ReadAsStringAsync();
                    // Handle response
                    Log.Debug("BB_check", "Caption response : " + responseString);

                }
            }
        }
    }
}
