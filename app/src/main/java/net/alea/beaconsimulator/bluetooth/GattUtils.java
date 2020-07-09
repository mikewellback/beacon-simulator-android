package net.alea.beaconsimulator.bluetooth;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;

import java.util.UUID;

public class GattUtils {

    public static final String              DESCRIPTOR_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";

    public static BluetoothGattService createPrimaryService(String uuid) {
        return new BluetoothGattService(UUID.fromString(uuid),
                BluetoothGattService.SERVICE_TYPE_PRIMARY);
    }

    public static void addReadCharacteristic(String uuid, BluetoothGattService service) {
        service.addCharacteristic(
                new BluetoothGattCharacteristic(UUID.fromString(uuid),
                        BluetoothGattCharacteristic.PROPERTY_READ,
                        BluetoothGattCharacteristic.PERMISSION_READ)
        );
    }

    public static void addNotifyCharacteristic(String uuid, BluetoothGattService service) {
        BluetoothGattCharacteristic chara = new BluetoothGattCharacteristic(UUID.fromString(uuid),
                BluetoothGattCharacteristic.PROPERTY_NOTIFY
                        | BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PERMISSION_READ);
        chara.addDescriptor(new BluetoothGattDescriptor(UUID.fromString(DESCRIPTOR_CONFIG),
                BluetoothGattDescriptor.PERMISSION_WRITE));
        service.addCharacteristic(chara);
    }

    public static void addWriteCharacteristic(String uuid, BluetoothGattService service) {
        service.addCharacteristic(
                new BluetoothGattCharacteristic(UUID.fromString(uuid),
                        BluetoothGattCharacteristic.PROPERTY_WRITE
                                | BluetoothGattCharacteristic.PROPERTY_READ,
                        BluetoothGattCharacteristic.PERMISSION_WRITE
                                | BluetoothGattCharacteristic.PERMISSION_READ)
        );
    }
}
