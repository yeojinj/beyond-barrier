using Tizen.Applications;
using Tizen;
using Tizen.Network.Bluetooth;
using System.Threading.Tasks;
using System.Collections.Generic;
using System.Diagnostics;
using System;
using System.Text;
using System.Reflection.PortableExecutable;

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
            const string ServiceUuid = "00005555-0000-1000-8000-00805f9b34fb";
            const string CharacteristicUuid = "00001234-0000-1000-8000-00805f9b34fb";
            const string CharacteristicValue = "Hello BLE!";
            public BluetoothGattServer GattServer;
            public BluetoothGattService GattService;
            public BluetoothGattCharacteristic GattCharacteristic;
            public BluetoothLeAdvertiser BleAdvertiser;
            public BluetoothLeAdvertiseData BleAdvertiseData;

            public BeyondBarrierBleServer()
            {
                // Init GattServer, Creating Service and Characteristic
                GattServer = BluetoothGattServer.CreateServer();
                GattService = new BluetoothGattService(ServiceUuid, BluetoothGattServiceType.Primary);
                GattCharacteristic = new BluetoothGattCharacteristic(CharacteristicUuid,
                    BluetoothGattPermission.Read | BluetoothGattPermission.Write,
                    BluetoothGattProperty.Read | BluetoothGattProperty.Write | BluetoothGattProperty.Notify,
                    Encoding.Default.GetBytes(CharacteristicValue));

                GattService.AddCharacteristic(GattCharacteristic);
                GattServer.RegisterGattService(GattService);

                // adding Callback functions
                GattCharacteristic.ReadRequested += ReadRequestedCB;
                GattCharacteristic.WriteRequested += WriteRequestedCB;
                GattCharacteristic.ValueChanged += CharacteristicValueChangedCB;
                GattServer.NotificationSent += NotificationSentCB;

                GattServer.Start();
                Log.Info("BB_check", "Bluetooth GATT Server Started");

                // setting Advertising options
                BleAdvertiser = BluetoothAdapter.GetBluetoothLeAdvertiser();
                BleAdvertiser.AdvertisingStateChanged += AdvertisingStateChangedCB;
                BleAdvertiseData = new BluetoothLeAdvertiseData();
                BleAdvertiseData.AdvertisingConnectable = true;
                BleAdvertiseData.IncludeDeviceName = true;
                BleAdvertiseData.AdvertisingMode = BluetoothLeAdvertisingMode.BluetoothLeAdvertisingBalancedMode;

                BleAdvertiseData.AddAdvertisingServiceUuid(BluetoothLePacketType.BluetoothLeScanResponsePacket, ServiceUuid);

                BleAdvertiser.StartAdvertising(BleAdvertiseData);
                Log.Info("BB_check", "Bluetooth Advertise Started");
            }

            public void CharacteristicValueChangedCB (object sender, ValueChangedEventArgs e)
            {
                Log.Info("BB_check", "Character value changed");
                Log.Info("BB_check", e.Value.ToString());
            }

            public void ReadRequestedCB (object sender, ReadRequestedEventArgs e)
            {
                Log.Info("BB_check", "character read requested");
                Log.Info("BB_check", e.ClientAddress.ToString());
                e.Server.SendResponse(
                    e.RequestId,
                    BluetoothGattRequestType.Read,
                    0,
                    e.Server.GetService(ServiceUuid).GetCharacteristic(CharacteristicUuid).Value,
                    e.Offset);
            }

            public void WriteRequestedCB (object sender, WriteRequestedEventArgs e)
            {
                Log.Info("BB_check", "characteristic write requested");
                GattCharacteristic.Value = e.Value;
                e.Server.SendResponse(
                    e.RequestId,
                    BluetoothGattRequestType.Write,
                    0,
                    e.Server.GetService(ServiceUuid).GetCharacteristic(CharacteristicUuid).Value,
                    e.Offset);
            }

            public void NotificationSentCB(object sender, NotificationSentEventArg e)
            {
                Log.Info("BB_check", e.ClientAddress);
                Log.Info("BB_check", e.Server.ToString());
            }

            public void NotificationStateChangedCB(object sender, NotificationStateChangedEventArg e)
            {
                Log.Info("BB_check", "notification state changed");
            }

            public void AdvertisingStateChangedCB(object sender, AdvertisingStateChangedEventArgs e)
            {
                Log.Info("BB_check", "advertise state changed");
            }
            public void Dispose()
            {
                Log.Info("BB_check", "Gatt Server Terminating");
                BleAdvertiser.StopAdvertising(BleAdvertiseData);
                GattServer.UnregisterGattService(GattService);
                GattServer.Dispose();
            }
        }
    }
}
