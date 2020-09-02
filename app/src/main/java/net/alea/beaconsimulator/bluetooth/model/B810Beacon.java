/****************************************************************************************
 * Copyright (c) 2016, 2017, 2019 Vincent Hiribarren                                    *
 *                                                                                      *
 * This program is free software; you can redistribute it and/or modify it under        *
 * the terms of the GNU General Public License as published by the Free Software        *
 * Foundation; either version 3 of the License, or (at your option) any later           *
 * version.                                                                             *
 *                                                                                      *
 * Linking Beacon Simulator statically or dynamically with other modules is making      *
 * a combined work based on Beacon Simulator. Thus, the terms and conditions of         *
 * the GNU General Public License cover the whole combination.                          *
 *                                                                                      *
 * As a special exception, the copyright holders of Beacon Simulator give you           *
 * permission to combine Beacon Simulator program with free software programs           *
 * or libraries that are released under the GNU LGPL and with independent               *
 * modules that communicate with Beacon Simulator solely through the                    *
 * net.alea.beaconsimulator.bluetooth.AdvertiseDataGenerator and the                    *
 * net.alea.beaconsimulator.bluetooth.AdvertiseDataParser interfaces. You may           *
 * copy and distribute such a system following the terms of the GNU GPL for             *
 * Beacon Simulator and the licenses of the other code concerned, provided that         *
 * you include the source code of that other code when and as the GNU GPL               *
 * requires distribution of source code and provided that you do not modify the         *
 * net.alea.beaconsimulator.bluetooth.AdvertiseDataGenerator and the                    *
 * net.alea.beaconsimulator.bluetooth.AdvertiseDataParser interfaces.                   *
 *                                                                                      *
 * The intent of this license exception and interface is to allow Bluetooth low energy  *
 * closed or proprietary advertise data packet structures and contents to be sensibly   *
 * kept closed, while ensuring the GPL is applied. This is done by using an interface   *
 * which only purpose is to generate android.bluetooth.le.AdvertiseData objects.        *
 *                                                                                      *
 * This exception is an additional permission under section 7 of the GNU General        *
 * Public License, version 3 (“GPLv3”).                                                 *
 *                                                                                      *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY      *
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A      *
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.             *
 *                                                                                      *
 * You should have received a copy of the GNU General Public License along with         *
 * this program.  If not, see <http://www.gnu.org/licenses/>.                           *
 ****************************************************************************************/

package net.alea.beaconsimulator.bluetooth.model;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.ScanRecord;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.app.INotificationSideChannel;
import android.util.Base64;
import android.util.Log;
import android.util.SparseArray;

import net.alea.beaconsimulator.bluetooth.AdvertiseDataGenerator;
import net.alea.beaconsimulator.bluetooth.ByteTools;
import net.alea.beaconsimulator.bluetooth.GattUtils;
import net.alea.beaconsimulator.util.MathUtils;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Random;
import java.util.TreeMap;
import java.util.UUID;

public class B810Beacon implements AdvertiseDataGenerator, Parcelable {

    public static final String CHARACTERISTIC_DEVICE_NAME = "00002a00-0000-1000-8000-00805f9b34fb"; // SERVICE_GENERIC
    public static final String CHARACTERISTIC_BATTERY_LEVEL = "00002a19-0000-1000-8000-00805f9b34fb"; // SERVICE_BATTERY
    public static final String CHARACTERISTIC_SERIAL = "00002a25-0000-1000-8000-00805f9b34fb"; // SERVICE_INFORMATION
    public static final String CHARACTERISTIC_FIRMWARE = "00002a26-0000-1000-8000-00805f9b34fb"; // SERVICE_INFORMATION
    public static final String CHARACTERISTIC_HARDWARE = "00002a27-0000-1000-8000-00805f9b34fb"; // SERVICE_INFORMATION
    public static final String CHARACTERISTIC_ACCELERATION = "43ba4000-4cf5-46d8-a887-a846d9de712d"; // SERVICE_MEMS
    public static final String CHARACTERISTIC_CALIBRATION = "43ba4002-4cf5-46d8-a887-a846d9de712d"; // SERVICE_MEMS
    public static final String CHARACTERISTIC_STATUS = "43ba4200-4cf5-46d8-a887-a846d9de712d"; // SERVICE_CONFIG
    public static final String CHARACTERISTIC_SPEED = "43ba4202-4cf5-46d8-a887-a846d9de712d"; // SERVICE_CONFIG
    public static final String CHARACTERISTIC_TIMESTAMP = "43ba4203-4cf5-46d8-a887-a846d9de712d"; // SERVICE_CONFIG
    public static final String CHARACTERISTIC_PIN = "43ba4204-4cf5-46d8-a887-a846d9de712d"; // SERVICE_CONFIG
    public static final String CHARACTERISTIC_CRASH_THRESHOLD = "43ba4302-4cf5-46d8-a887-a846d9de712d"; // SERVICE_DRIVING
    public static final String CHARACTERISTIC_CRASH_BUFFER = "43ba4400-4cf5-46d8-a887-a846d9de712d"; // SERVICE_MEMORY
    public static final String CHARACTERISTIC_CRASH_DOWNLOAD = "43ba4401-4cf5-46d8-a887-a846d9de712d"; // SERVICE_MEMORY
    public static final String CHARACTERISTIC_EVENT_DOWNLOAD = "43ba4403-4cf5-46d8-a887-a846d9de712d"; // SERVICE_MEMORY
    public static final String CHARACTERISTIC_ANOMALIES = "43ba4404-4cf5-46d8-a887-a846d9de712d"; // SERVICE_MEMORY

    public static final String SERVICE_GENERIC = "00001800-0000-1000-8000-00805f9b34fb";
    public static final String SERVICE_INFORMATION = "0000180a-0000-1000-8000-00805f9b34fb";
    public static final String SERVICE_BATTERY = "0000180f-0000-1000-8000-00805f9b34fb";
    public static final String SERVICE_MEMS = "43ba1a00-4cf5-46d8-a887-a846d9de712d";
    public static final String SERVICE_CONFIG = "43ba1a02-4cf5-46d8-a887-a846d9de712d";
    public static final String SERVICE_DRIVING = "43ba1a03-4cf5-46d8-a887-a846d9de712d";
    public static final String SERVICE_MEMORY = "43ba1a04-4cf5-46d8-a887-a846d9de712d";

    public final static short BEACON_CODE = (short) 0x215;
    public final static int MANUFACTURER_PACKET_SIZE = 23;
    public static double k_G_TO_MS2 = 0.00980665;
    public static boolean stop = false;


    private int manufacturerId;
    private UUID beaconNamespace;
    private int major;
    private int minor;
    private String manufacturerReserved;
    private byte power;
    private String serial;

    private static BluetoothGattServer mGattServer;
    private static BluetoothDevice connectedDevice;
    public BluetoothGattServerCallback gattCallback = new BluetoothGattServerCallback() {

        @Override
        public void onDescriptorWriteRequest(BluetoothDevice device, int requestId, BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            super.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite, responseNeeded, offset, value);
            if (responseNeeded) {
                mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value);
            }
        }

        @Override
        public void onServiceAdded(int status, BluetoothGattService service) {
            Log.d("GATT", "onServiceAdded(status: " + status + ", service: " + service + ")");
            super.onServiceAdded(status, service);
            configureGatt(mGattServer, service.getUuid().toString());
        }


        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            Log.d("GATT", "onConnectionStateChange(device: " + device + ", status: " + status + ", newState: " + newState + ")");
            super.onConnectionStateChange(device, status, newState);
            if (status == 0) {
                connectedDevice = device;
            }

        }

        @Override
        public void onNotificationSent(BluetoothDevice device, int status) {
            super.onNotificationSent(device, status);
        }

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
            Log.d("GATT", "onCharacteristicReadRequest(device: " + device + ", requestId: " + requestId + ", offset: " + offset + ", characteristic: " + characteristic.getUuid() + ")");
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
            switch (characteristic.getUuid().toString()) {
                case CHARACTERISTIC_DEVICE_NAME:
                    mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, "SmartTag".getBytes());
                    break;
                case CHARACTERISTIC_FIRMWARE:
                    mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, "02.16.00".getBytes());
                    break;
                case CHARACTERISTIC_HARDWARE:
                    mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, "v2.16".getBytes());
                    break;
                case CHARACTERISTIC_PIN:
                    mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, new byte[]{0x01, 0x00});
                    break;
                case CHARACTERISTIC_SERIAL:
                    mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, getSerial().getBytes());
                    break;
                case CHARACTERISTIC_CRASH_THRESHOLD:
                    mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, new byte[]{
                            -0x30, 0x07, 0x03, 0x00, 0x14, 0x05, 0x03, 0x00});
                    break;
                default:
                    mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, new byte[]{0x00, 0x00});
            }
        }

        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            Log.d("GATT", "onCharacteristicWriteRequest(device: " + device + ", requestId: " + requestId + ", characteristic: " + characteristic.getUuid() + ", preparedWrite: " + preparedWrite + ", responseNeeded: " + responseNeeded + ", offset: " + offset + ", value: " + Arrays.toString(value) + ")");
            super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value);
            switch (characteristic.getUuid().toString()) {
                case CHARACTERISTIC_CALIBRATION:
                    if (value[0] == 1) {
                        calibrateAcceleration();
                    }
                    break;
                case CHARACTERISTIC_CRASH_DOWNLOAD:
                    if (MathUtils.getInt(value) == 0) {
                        sendCrashBuffer();
                    } else {
                        if (MathUtils.getInt(value) == 0xFFFF) {
                            Log.i("GATT", "process completed: ");

                            break;
                        } else if (MathUtils.getInt(value) == 0xFFFE) {
                            Log.i("GATT", "process restarted: ");

                            sendCrash();
                            break;
                        } else {
                            Log.i("GATT", "lost index: "+MathUtils.getInt(value));
                            byte[] data = new byte[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
                            if (MathUtils.getInt(value) > 599) {
                                byte[] time = MathUtils.getBytes(System.currentTimeMillis());
                                System.arraycopy(time, 0, value, 2, 4);
                            }
                            System.arraycopy(value, 0, value, 0, 2);
                            BluetoothGattCharacteristic charaDrive = mGattServer.getService(UUID.fromString(SERVICE_MEMORY))
                                    .getCharacteristic(UUID.fromString(CHARACTERISTIC_CRASH_BUFFER));
                            charaDrive.setValue(data);
                            mGattServer.notifyCharacteristicChanged(connectedDevice, charaDrive, false);
                        }
                    }
                    break;
            }
            if (responseNeeded) {
                mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value);
            }
        }


        @Override
        public void onExecuteWrite(BluetoothDevice device, int requestId, boolean execute) {
            Log.d("GATT", "onExecuteWrite(device: " + device + ", requestId: " + requestId + ", execute: " + execute + ")");
            super.onExecuteWrite(device, requestId, execute);
        }
    };


    public B810Beacon() {
        setManufacturerId(0x004C);
        setBeaconNamespace(UUID.fromString("B810736B-11FC-85C3-1762-80DF658F0B31"));
        setMajor(0);
        setMinor(0);
        setPower((byte) -69);
        serial = "";

    }

    public static void configureGatt(BluetoothGattServer gattServer) {
        mGattServer = gattServer;
        configureGatt(gattServer, null);
    }

    private static void configureGatt(BluetoothGattServer gattServer, String addedService) {
        String[] service_uuids = {
                //SERVICE_GENERIC,
                SERVICE_INFORMATION,
                SERVICE_BATTERY,
                SERVICE_MEMS,
                SERVICE_CONFIG,
                SERVICE_DRIVING,
                SERVICE_MEMORY,
        };
        String uuid = null;
        if (addedService == null) {
            uuid = service_uuids[0];
        } else {
            for (int i = 0; i < service_uuids.length - 1; i++) {
                if (service_uuids[i].equals(addedService)) {
                    uuid = service_uuids[i + 1];
                    break;
                }
            }
        }
        if (uuid != null) {
            //for (String uuid: service_uuids) {
            BluetoothGattService service = GattUtils.createPrimaryService(uuid);
            switch (uuid) {
                case SERVICE_GENERIC:
                    GattUtils.addWriteCharacteristic(CHARACTERISTIC_DEVICE_NAME, service);
                    break;
                case SERVICE_BATTERY:
                    GattUtils.addReadCharacteristic(CHARACTERISTIC_BATTERY_LEVEL, service);
                    break;
                case SERVICE_INFORMATION:
                    GattUtils.addReadCharacteristic(CHARACTERISTIC_SERIAL, service);
                    GattUtils.addReadCharacteristic(CHARACTERISTIC_FIRMWARE, service);
                    GattUtils.addReadCharacteristic(CHARACTERISTIC_HARDWARE, service);
                    break;
                case SERVICE_MEMS:
                    GattUtils.addNotifyCharacteristic(CHARACTERISTIC_ACCELERATION, service);
                    GattUtils.addWriteCharacteristic(CHARACTERISTIC_CALIBRATION, service);
                    break;
                case SERVICE_CONFIG:
                    GattUtils.addNotifyCharacteristic(CHARACTERISTIC_STATUS, service);
                    GattUtils.addWriteCharacteristic(CHARACTERISTIC_SPEED, service);
                    GattUtils.addWriteCharacteristic(CHARACTERISTIC_TIMESTAMP, service);
                    GattUtils.addWriteCharacteristic(CHARACTERISTIC_PIN, service);
                    break;
                case SERVICE_DRIVING:
                    GattUtils.addWriteCharacteristic(CHARACTERISTIC_CRASH_THRESHOLD, service);
                    break;
                case SERVICE_MEMORY:
                    GattUtils.addNotifyCharacteristic(CHARACTERISTIC_CRASH_BUFFER, service);
                    GattUtils.addWriteCharacteristic(CHARACTERISTIC_CRASH_DOWNLOAD, service);
                    GattUtils.addReadCharacteristic(CHARACTERISTIC_EVENT_DOWNLOAD, service);
                    GattUtils.addWriteCharacteristic(CHARACTERISTIC_ANOMALIES, service);
                    break;
            }
            gattServer.addService(service);
        }
    }

    public UUID getBeaconNamespace() {
        return beaconNamespace;
    }

    public void setBeaconNamespace(UUID beaconNamespace) {
        this.beaconNamespace = beaconNamespace;
    }

    public int getMajor() {
        return major;
    }

    public final void setMajor(int major) {
        this.major = ByteTools.capToUnsignedShort(major);
    }

    public int getMinor() {
        return minor;
    }

    public void setMinor(int minor) {
        this.minor = ByteTools.capToUnsignedShort(minor);
    }

    public int getManufacturerId() {
        return manufacturerId;
    }

    public void setManufacturerId(int manufacturerId) {
        this.manufacturerId = ByteTools.capToUnsignedShort(manufacturerId);
    }

    public byte getPower() {
        return power;
    }

    public void setPower(byte power) {
        this.power = power;
    }

    public String getSerial() {
        return serial;
    }

    public void setSerial(String serial) {
        this.serial = serial;
    }

    @Override
    public AdvertiseData generateAdvertiseData() {
        final ByteBuffer buffer = ByteBuffer.allocate(MANUFACTURER_PACKET_SIZE);
        buffer.putShort(BEACON_CODE);
        buffer.putLong(getBeaconNamespace().getMostSignificantBits());
        buffer.putLong(getBeaconNamespace().getLeastSignificantBits());
        buffer.put(ByteTools.toShortInBytes_BE(getMajor()));
        buffer.put(ByteTools.toShortInBytes_BE(getMinor()));
        buffer.put(getPower());
        return new AdvertiseData.Builder()
                .addManufacturerData(getManufacturerId(), buffer.array())
                //.setIncludeDeviceName(true)
                .build();
    }


    public static B810Beacon parseRecord(ScanRecord scanRecord) {
        // Check data validity
        final SparseArray<byte[]> manufacturers = scanRecord.getManufacturerSpecificData();
        if (manufacturers == null || manufacturers.size() != 1) {
            return null;
        }
        final byte[] data = manufacturers.valueAt(0);
        if (data.length != MANUFACTURER_PACKET_SIZE) {
            return null;
        }
        final ByteBuffer buffer = ByteBuffer.wrap(data);
        final short beaconCode = buffer.getShort();
        if (beaconCode != BEACON_CODE) {
            return null;
        }
        // Parse data
        final long uuidHigh = buffer.getLong();
        final long uuidLow = buffer.getLong();
        final int major = ByteTools.toIntFromShortInBytes_BE(new byte[]{buffer.get(), buffer.get()});
        final int minor = ByteTools.toIntFromShortInBytes_BE(new byte[]{buffer.get(), buffer.get()});
        final byte power = buffer.get();
        final B810Beacon b810beacon = new B810Beacon();
        b810beacon.setBeaconNamespace(new UUID(uuidHigh, uuidLow));
        b810beacon.setMajor(major);
        b810beacon.setMinor(minor);
        b810beacon.setPower(power);
        return b810beacon;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.manufacturerId);
        dest.writeSerializable(this.beaconNamespace);
        dest.writeInt(this.major);
        dest.writeInt(this.minor);
        dest.writeString(this.serial);
        dest.writeString(this.manufacturerReserved);
        dest.writeByte(this.power);
    }

    protected B810Beacon(Parcel in) {
        this.manufacturerId = in.readInt();
        this.beaconNamespace = (UUID) in.readSerializable();
        this.major = in.readInt();
        this.minor = in.readInt();
        this.serial = in.readString();
        this.manufacturerReserved = in.readString();
        this.power = in.readByte();
    }

    public static void sendAcceleration(final int time) {
        stop = true;
        BluetoothGattCharacteristic charaDrive = mGattServer.getService(UUID.fromString(SERVICE_CONFIG)).getCharacteristic(UUID.fromString(CHARACTERISTIC_STATUS));
        charaDrive.setValue(MathUtils.getBytes(1));
        mGattServer.notifyCharacteristicChanged(connectedDevice, charaDrive, false);
        final byte[] data = new byte[6];

        stop = false;
        final Handler h = new Handler();
        final int[] i = {0};
        Runnable r = new Runnable() {
            @Override
            public void run() {
                if (stop) {
                    h.removeCallbacks(this);
                    sendParking();
                    return;
                }

                if (time == 0 || i[0] < time) {
                    MathUtils.copyBytes(data, 300 + new Random().nextInt(20), 400 + new Random().nextInt(20), 30);
                    final BluetoothGattCharacteristic chara = mGattServer.getService(UUID.fromString(SERVICE_MEMS)).getCharacteristic(UUID.fromString(CHARACTERISTIC_ACCELERATION));
                    chara.setValue(data);
                    mGattServer.notifyCharacteristicChanged(connectedDevice, chara, false);
                    h.postDelayed(this, 1000);
                    i[0]++;
                } else {
                    sendParking();
                    h.removeCallbacks(this);
                }
            }
        };
        h.postDelayed(r, 1000);
    }


    public static void sendParking() {
        stop = true;
        BluetoothGattCharacteristic chara = mGattServer.getService(UUID.fromString(SERVICE_CONFIG)).getCharacteristic(UUID.fromString(CHARACTERISTIC_STATUS));
        chara.setValue(MathUtils.getBytes(0));
        mGattServer.notifyCharacteristicChanged(connectedDevice, chara, false);

    }

    public static void sendCrash() {
        stop = true;
        BluetoothGattCharacteristic chara = mGattServer.getService(UUID.fromString(SERVICE_CONFIG)).getCharacteristic(UUID.fromString(CHARACTERISTIC_STATUS));
        chara.setValue(MathUtils.getBytes(6));
        mGattServer.notifyCharacteristicChanged(connectedDevice, chara, false);
    }


    public static final Creator<B810Beacon> CREATOR = new Creator<B810Beacon>() {
        @Override
        public B810Beacon createFromParcel(Parcel source) {
            return new B810Beacon(source);
        }

        @Override
        public B810Beacon[] newArray(int size) {
            return new B810Beacon[size];
        }
    };


    public static void calibrateAcceleration() {
        stop = false;
        for (int i = 0; i < 5; i++) {
            if (stop) {
                return;
            }
            try {
                sendIdleAcceleration();
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static void sendIdleAcceleration() {
        byte[] data = new byte[6];
        MathUtils.copyBytes(data, 0, 0, 930);
        BluetoothGattCharacteristic chara = mGattServer.getService(UUID.fromString(SERVICE_MEMS)).getCharacteristic(UUID.fromString(CHARACTERISTIC_ACCELERATION));
        chara.setValue(data);
        mGattServer.notifyCharacteristicChanged(connectedDevice, chara, false);
        Log.i("GATT: ", "send acceleration =" + Arrays.toString(data));
    }

    public static void sendCrashBuffer() {
        final int[] index = {1};
        final Handler h = new Handler(Looper.getMainLooper());
        Log.i("GATT", "crash buffer start");
        Runnable r = new Runnable() {
            @Override
            public void run() {
                byte[] value = new byte[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
                if (index[0] <= 601) {
                    if (index[0] > 599) {
                        byte[] time = MathUtils.getBytes(System.currentTimeMillis());
                        System.arraycopy(time, 0, value, 2, 4);
                    }
                    System.arraycopy(MathUtils.getBytes(index[0]), 0, value, 0, 2);

                    BluetoothGattCharacteristic charaDrive = mGattServer.getService(UUID.fromString(SERVICE_MEMORY))
                            .getCharacteristic(UUID.fromString(CHARACTERISTIC_CRASH_BUFFER));
                    charaDrive.setValue(value);
                    mGattServer.notifyCharacteristicChanged(connectedDevice, charaDrive, false);
                    index[0]++;
                    h.postDelayed(this, 100);
                    Log.i("GATT", "crash buffer index: " + index[0]);
                } else {
                    BluetoothGattCharacteristic chara = mGattServer.getService(UUID.fromString(SERVICE_CONFIG)).getCharacteristic(UUID.fromString(CHARACTERISTIC_STATUS));
                    chara.setValue(MathUtils.getBytes(26));
                    mGattServer.notifyCharacteristicChanged(connectedDevice, chara, false);
                    h.removeCallbacks(this);
                    Log.i("GATT", "crash buffer end: " + index[0]);
                }
            }
        };
        h.postDelayed(r, 100);
    }


    public static void addBuffer(byte[] buffer, TreeMap<Integer, byte[]> buffers) {
        int indexInt = MathUtils.getInt(buffer);
        buffers.put(indexInt, buffer);
        if (buffers.size() == 1) {
            Log.i("BLE", "Starting crash download of type: ");
        }
        if (buffers.size() == 601) {
//            crashBuffer = "";
            byte[] buf601 = buffers.get(601);
            if (buf601 != null) {
//                timestamp = MathUtils.getLong(buf601, 2);
            }
            byte[] allBytes = new byte[0];
            for (byte[] b : buffers.values()) {
//                crashBuffer += Base64.encodeToString(b, Base64.NO_WRAP);
                allBytes = concatByteArrays(allBytes, b);
            }
            BluetoothGattCharacteristic charaDrive = mGattServer.getService(UUID.fromString(SERVICE_MEMORY))
                    .getCharacteristic(UUID.fromString(CHARACTERISTIC_CRASH_BUFFER));
            charaDrive.setValue(allBytes);
            mGattServer.notifyCharacteristicChanged(connectedDevice, charaDrive, false);
//            crashBuffer = Base64.encodeToString(allBytes, Base64.NO_WRAP);
        }
    }

    private static byte[] concatByteArrays(byte[] first, byte[] second) {
        byte[] destination = new byte[first.length + second.length];
        System.arraycopy(first, 0, destination, 0, first.length);
        System.arraycopy(second, 0, destination, first.length, second.length);
        return destination;
    }
}
