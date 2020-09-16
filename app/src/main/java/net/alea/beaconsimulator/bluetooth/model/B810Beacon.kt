/****************************************************************************************
 * Copyright (c) 2016, 2017, 2019 Vincent Hiribarren                                    *
 * *
 * This program is free software; you can redistribute it and/or modify it under        *
 * the terms of the GNU General Public License as published by the Free Software        *
 * Foundation; either version 3 of the License, or (at your option) any later           *
 * version.                                                                             *
 * *
 * Linking Beacon Simulator statically or dynamically with other modules is making      *
 * a combined work based on Beacon Simulator. Thus, the terms and conditions of         *
 * the GNU General Public License cover the whole combination.                          *
 * *
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
 * *
 * The intent of this license exception and interface is to allow Bluetooth low energy  *
 * closed or proprietary advertise data packet structures and contents to be sensibly   *
 * kept closed, while ensuring the GPL is applied. This is done by using an interface   *
 * which only purpose is to generate android.bluetooth.le.AdvertiseData objects.        *
 * *
 * This exception is an additional permission under section 7 of the GNU General        *
 * Public License, version 3 (“GPLv3”).                                                 *
 * *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY      *
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A      *
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.             *
 * *
 * You should have received a copy of the GNU General Public License along with         *
 * this program.  If not, see <http:></http:>//www.gnu.org/licenses/>.                           *
 */
package net.alea.beaconsimulator.bluetooth.model

import android.bluetooth.*
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.ScanRecord
import android.os.Handler
import android.os.Looper
import android.os.Parcel
import android.os.Parcelable
import android.util.Log
import net.alea.beaconsimulator.bluetooth.AdvertiseDataGenerator
import net.alea.beaconsimulator.bluetooth.ByteTools
import net.alea.beaconsimulator.bluetooth.GattUtils
import net.alea.beaconsimulator.util.MathUtils
import java.nio.ByteBuffer
import java.util.*

class B810Beacon : AdvertiseDataGenerator, Parcelable {
    private var manufacturerId = 0
    var beaconNamespace: UUID? = null
    private var major = 0
    private var minor = 0
    private var manufacturerReserved: String? = null
    var power: Byte = 0
    var serial: String = ""

    var gattCallback: BluetoothGattServerCallback = object : BluetoothGattServerCallback() {
        override fun onDescriptorWriteRequest(device: BluetoothDevice, requestId: Int, descriptor: BluetoothGattDescriptor, preparedWrite: Boolean, responseNeeded: Boolean, offset: Int, value: ByteArray) {
            super.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite, responseNeeded, offset, value)
            if (responseNeeded) {
                mGattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value)
            }
        }

        override fun onServiceAdded(status: Int, service: BluetoothGattService) {
            Log.d("GATT", "onServiceAdded(status: $status, service: $service)")
            super.onServiceAdded(status, service)
            configureGatt(mGattServer, service.uuid.toString())
        }

        override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
            Log.d("GATT", "onConnectionStateChange(device: $device, status: $status, newState: $newState)")
            super.onConnectionStateChange(device, status, newState)
            connectedDevice = device
            if (status == BluetoothGatt.STATE_CONNECTED) {
                connectedCallback(true)
                connecttionStatus = true
            } else {
                connectedCallback(false)
                connecttionStatus = false
            }
        }

        override fun onNotificationSent(device: BluetoothDevice, status: Int) {
            super.onNotificationSent(device, status)
        }

        override fun onCharacteristicReadRequest(device: BluetoothDevice, requestId: Int, offset: Int, characteristic: BluetoothGattCharacteristic) {
            Log.d("GATT", "onCharacteristicReadRequest(device: " + device + ", requestId: " + requestId + ", offset: " + offset + ", characteristic: " + characteristic.uuid + ")")
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic)
            when (characteristic.uuid.toString()) {
                CHARACTERISTIC_DEVICE_NAME -> mGattServer!!.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, "SmartTag".toByteArray())
                CHARACTERISTIC_FIRMWARE -> mGattServer!!.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, "02.16.00".toByteArray())
                CHARACTERISTIC_HARDWARE -> mGattServer!!.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, "02.00".toByteArray())
                CHARACTERISTIC_PIN -> mGattServer!!.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, byteArrayOf(0x01, 0x00))
                CHARACTERISTIC_SERIAL -> mGattServer ?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, serial.toByteArray())
                CHARACTERISTIC_EVENT_DOWNLOAD -> mGattServer ?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset,
                        byteArrayOf(0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00))
                CHARACTERISTIC_CRASH_THRESHOLD -> mGattServer ?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, byteArrayOf(
                        -0x30, 0x07, 0x03, 0x00, 0x14, 0x05, 0x03, 0x00))
                CHARACTERISTIC_ACCELERATION ->
                    calibrateAcceleration()
                else -> mGattServer ?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, byteArrayOf(0x00, 0x00))
            }
        }

        override fun onCharacteristicWriteRequest(device: BluetoothDevice, requestId: Int, characteristic: BluetoothGattCharacteristic, preparedWrite: Boolean, responseNeeded: Boolean, offset: Int, value: ByteArray) {
            Log.d("GATT", "onCharacteristicWriteRequest(device: " + device + ", requestId: " + requestId + ", characteristic: " + characteristic.uuid + ", preparedWrite: " + preparedWrite + ", responseNeeded: " + responseNeeded + ", offset: " + offset + ", value: " + Arrays.toString(value) + ")")
            super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value)
            when (characteristic.uuid.toString()) {
                CHARACTERISTIC_PIN -> {
                    if (MathUtils.getInt(value) == 1234) {
                        connectedCallback(true)
                        connecttionStatus = true
                    }
                }
                CHARACTERISTIC_CALIBRATION ->
                    if (value[0] == 1.toByte()) {
                        calibrateAcceleration()
                    }
                CHARACTERISTIC_CRASH_DOWNLOAD -> if (MathUtils.getInt(value) == 0) {
                    sendCrashBuffer()
                } else {
                    if (MathUtils.getInt(value) == 0xFFFF) {
                        Log.i("GATT", "process completed: ")
                    } else if (MathUtils.getInt(value) == 0xFFFE) {
                        Log.i("GATT", "process restarted: ")
                        sendCrash()
                    } else {
                        Log.i("GATT", "lost index: " + MathUtils.getInt(value))

                        val data = byteArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
                        if (MathUtils.getInt(value) > 599) {
                            val time = MathUtils.getBytes(System.currentTimeMillis())
                            System.arraycopy(time, 0, value, 2, 4)
                        }
                        System.arraycopy(value, 0, value, 0, 2)
                        val charaDrive = mGattServer ?.getService(UUID.fromString(SERVICE_MEMORY))
                                ?.getCharacteristic(UUID.fromString(CHARACTERISTIC_CRASH_BUFFER))
                        charaDrive?.value = data
                        if(connectedDevice==null)return
                        mGattServer ?.notifyCharacteristicChanged(connectedDevice, charaDrive, false)
                    }
                }
            }
            if (responseNeeded) {
                mGattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value)
            }
        }

        override fun onExecuteWrite(device: BluetoothDevice, requestId: Int, execute: Boolean) {
            Log.d("GATT", "onExecuteWrite(device: $device, requestId: $requestId, execute: $execute)")
            super.onExecuteWrite(device, requestId, execute)
        }
    }

    constructor() {
        setManufacturerId(0x004C)
        beaconNamespace = UUID.fromString("B810736B-11FC-85C3-1762-80DF658F0B31")
        setMajor(0)
        setMinor(0)
        power = (-50).toByte()
        serial = ""
    }

    fun getMajor(): Int {
        return major
    }

    fun setMajor(major: Int) {
        this.major = ByteTools.capToUnsignedShort(major)
    }

    fun getMinor(): Int {
        return minor
    }

    fun setMinor(minor: Int) {
        this.minor = ByteTools.capToUnsignedShort(minor)
    }

    fun getManufacturerId(): Int {
        return manufacturerId
    }

    fun setManufacturerId(manufacturerId: Int) {
        this.manufacturerId = ByteTools.capToUnsignedShort(manufacturerId)
    }

    override fun generateAdvertiseData(): AdvertiseData {
        val buffer = ByteBuffer.allocate(MANUFACTURER_PACKET_SIZE)
        buffer.putShort(BEACON_CODE)
        beaconNamespace?.let {
            buffer.putLong(it.mostSignificantBits)
            buffer.putLong(it.leastSignificantBits)
        }
        buffer.put(ByteTools.toShortInBytes_BE(getMajor()))
        buffer.put(ByteTools.toShortInBytes_BE(getMinor()))
        buffer.put(power)
        return AdvertiseData.Builder()
                .addManufacturerData(getManufacturerId(), buffer.array()) //.setIncludeDeviceName(true)
                .build()
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(manufacturerId)
        dest.writeSerializable(beaconNamespace)
        dest.writeInt(major)
        dest.writeInt(minor)
        dest.writeString(serial)
        dest.writeString(manufacturerReserved)
        dest.writeByte(power)
    }

    protected constructor(`in`: Parcel) {
        manufacturerId = `in`.readInt()
        beaconNamespace = `in`.readSerializable() as UUID
        major = `in`.readInt()
        minor = `in`.readInt()
        serial = `in`.readString()
        manufacturerReserved = `in`.readString()
        power = `in`.readByte()
    }

    companion object {
        var manualCalib = false
        var calib_x = 0
        var calib_y = 0
        var calib_z = 0
        var stopCalib = false
        const val CHARACTERISTIC_DEVICE_NAME = "00002a00-0000-1000-8000-00805f9b34fb" // SERVICE_GENERIC
        const val CHARACTERISTIC_BATTERY_LEVEL = "00002a19-0000-1000-8000-00805f9b34fb" // SERVICE_BATTERY
        const val CHARACTERISTIC_SERIAL = "00002a25-0000-1000-8000-00805f9b34fb" // SERVICE_INFORMATION
        const val CHARACTERISTIC_FIRMWARE = "00002a26-0000-1000-8000-00805f9b34fb" // SERVICE_INFORMATION
        const val CHARACTERISTIC_HARDWARE = "00002a27-0000-1000-8000-00805f9b34fb" // SERVICE_INFORMATION
        const val CHARACTERISTIC_ACCELERATION = "43ba4000-4cf5-46d8-a887-a846d9de712d" // SERVICE_MEMS
        const val CHARACTERISTIC_CALIBRATION = "43ba4002-4cf5-46d8-a887-a846d9de712d" // SERVICE_MEMS
        const val CHARACTERISTIC_STATUS = "43ba4200-4cf5-46d8-a887-a846d9de712d" // SERVICE_CONFIG
        const val CHARACTERISTIC_SPEED = "43ba4202-4cf5-46d8-a887-a846d9de712d" // SERVICE_CONFIG
        const val CHARACTERISTIC_TIMESTAMP = "43ba4203-4cf5-46d8-a887-a846d9de712d" // SERVICE_CONFIG
        const val CHARACTERISTIC_PIN = "43ba4204-4cf5-46d8-a887-a846d9de712d" // SERVICE_CONFIG
        const val CHARACTERISTIC_CRASH_THRESHOLD = "43ba4302-4cf5-46d8-a887-a846d9de712d" // SERVICE_DRIVING
        const val CHARACTERISTIC_CRASH_BUFFER = "43ba4400-4cf5-46d8-a887-a846d9de712d" // SERVICE_MEMORY
        const val CHARACTERISTIC_CRASH_DOWNLOAD = "43ba4401-4cf5-46d8-a887-a846d9de712d" // SERVICE_MEMORY
        const val CHARACTERISTIC_EVENT_DOWNLOAD = "43ba4403-4cf5-46d8-a887-a846d9de712d" // SERVICE_MEMORY
        const val CHARACTERISTIC_ANOMALIES = "43ba4404-4cf5-46d8-a887-a846d9de712d" // SERVICE_MEMORY
        const val SERVICE_GENERIC = "00001800-0000-1000-8000-00805f9b34fb"
        const val SERVICE_INFORMATION = "0000180a-0000-1000-8000-00805f9b34fb"
        const val SERVICE_BATTERY = "0000180f-0000-1000-8000-00805f9b34fb"
        const val SERVICE_MEMS = "43ba1a00-4cf5-46d8-a887-a846d9de712d"
        const val SERVICE_CONFIG = "43ba1a02-4cf5-46d8-a887-a846d9de712d"
        const val SERVICE_DRIVING = "43ba1a03-4cf5-46d8-a887-a846d9de712d"
        const val SERVICE_MEMORY = "43ba1a04-4cf5-46d8-a887-a846d9de712d"
        const val BEACON_CODE = 0x215.toShort()
        const val MANUFACTURER_PACKET_SIZE = 23
        var k_G_TO_MS2 = 0.00980665
        var stop = false
        private var mGattServer: BluetoothGattServer? = null
        private var connectedDevice: BluetoothDevice? = null
        fun configureGatt(gattServer: BluetoothGattServer?) {
            mGattServer = gattServer
            configureGatt(gattServer, null)
        }

        var crashProgressCallback: (value: Int) -> Unit = {}
        var connectedCallback: (value: Boolean) -> Unit = {}
        var calibrateCallback: () -> Unit = {}
        var connecttionStatus = false
        var calibrateStatus = false


        private fun configureGatt(gattServer: BluetoothGattServer?, addedService: String?) {
            val service_uuids = arrayOf( //SERVICE_GENERIC,
                    SERVICE_INFORMATION,
                    SERVICE_BATTERY,
                    SERVICE_MEMS,
                    SERVICE_CONFIG,
                    SERVICE_DRIVING,
                    SERVICE_MEMORY)
            var uuid: String? = null
            if (addedService == null) {
                uuid = service_uuids[0]
            } else {
                for (i in 0 until service_uuids.size - 1) {
                    if (service_uuids[i] == addedService) {
                        uuid = service_uuids[i + 1]
                        break
                    }
                }
            }
            if (uuid != null) {
                //for (String uuid: service_uuids) {
                val service = GattUtils.createPrimaryService(uuid)
                when (uuid) {
                    SERVICE_GENERIC -> GattUtils.addWriteCharacteristic(CHARACTERISTIC_DEVICE_NAME, service)
                    SERVICE_BATTERY -> GattUtils.addReadCharacteristic(CHARACTERISTIC_BATTERY_LEVEL, service)
                    SERVICE_INFORMATION -> {
                        GattUtils.addReadCharacteristic(CHARACTERISTIC_SERIAL, service)
                        GattUtils.addReadCharacteristic(CHARACTERISTIC_FIRMWARE, service)
                        GattUtils.addReadCharacteristic(CHARACTERISTIC_HARDWARE, service)
                    }
                    SERVICE_MEMS -> {
                        GattUtils.addNotifyCharacteristic(CHARACTERISTIC_ACCELERATION, service)
                        GattUtils.addWriteCharacteristic(CHARACTERISTIC_CALIBRATION, service)
                    }
                    SERVICE_CONFIG -> {
                        GattUtils.addNotifyCharacteristic(CHARACTERISTIC_STATUS, service)
                        GattUtils.addWriteCharacteristic(CHARACTERISTIC_SPEED, service)
                        GattUtils.addWriteCharacteristic(CHARACTERISTIC_TIMESTAMP, service)
                        GattUtils.addWriteCharacteristic(CHARACTERISTIC_PIN, service)
                    }
                    SERVICE_DRIVING -> GattUtils.addWriteCharacteristic(CHARACTERISTIC_CRASH_THRESHOLD, service)
                    SERVICE_MEMORY -> {
                        GattUtils.addNotifyCharacteristic(CHARACTERISTIC_CRASH_BUFFER, service)
                        GattUtils.addWriteCharacteristic(CHARACTERISTIC_CRASH_DOWNLOAD, service)
                        GattUtils.addReadCharacteristic(CHARACTERISTIC_EVENT_DOWNLOAD, service)
                        GattUtils.addWriteCharacteristic(CHARACTERISTIC_ANOMALIES, service)
                    }
                }
                gattServer?.addService(service)
            }
        }

        fun parseRecord(scanRecord: ScanRecord): B810Beacon? {
            // Check data validity
            val manufacturers = scanRecord.manufacturerSpecificData
            if (manufacturers == null || manufacturers.size() != 1) {
                return null
            }
            val data = manufacturers.valueAt(0)
            if (data.size != MANUFACTURER_PACKET_SIZE) {
                return null
            }
            val buffer = ByteBuffer.wrap(data)
            val beaconCode = buffer.short
            if (beaconCode != BEACON_CODE) {
                return null
            }
            // Parse data
            val uuidHigh = buffer.long
            val uuidLow = buffer.long
            val major = ByteTools.toIntFromShortInBytes_BE(byteArrayOf(buffer.get(), buffer.get()))
            val minor = ByteTools.toIntFromShortInBytes_BE(byteArrayOf(buffer.get(), buffer.get()))
            val power = buffer.get()
            val b810beacon = B810Beacon()
            b810beacon.beaconNamespace = UUID(uuidHigh, uuidLow)
            b810beacon.setMajor(major)
            b810beacon.setMinor(minor)
            b810beacon.power = power
            return b810beacon
        }

        fun sendAcceleration(time: Int) {
            stop = true
            val charaDrive = mGattServer?.getService(UUID.fromString(SERVICE_CONFIG))?.getCharacteristic(UUID.fromString(CHARACTERISTIC_STATUS))
            charaDrive?.value = MathUtils.getBytes(1)
            if(connectedDevice==null)return
            mGattServer?.notifyCharacteristicChanged(connectedDevice, charaDrive, false)
            val data = ByteArray(6)
            stop = false
            val h = Handler(Looper.getMainLooper())
            val i = intArrayOf(0)
            val r: Runnable = object : Runnable {
                override fun run() {
                    if (stop) {
                        h.removeCallbacks(this)
                        sendParking()
                        return
                    }
                    if (time == 0 || i[0] < time) {
                        MathUtils.copyBytes(data, 300 + Random().nextInt(20), 400 + Random().nextInt(20), 30)
                        val chara = mGattServer ?.getService(UUID.fromString(SERVICE_MEMS))?.getCharacteristic(UUID.fromString(CHARACTERISTIC_ACCELERATION))
                        chara?.value = data
                        if(connectedDevice==null)return
                        mGattServer ?.notifyCharacteristicChanged(connectedDevice, chara, false)
                        h.postDelayed(this, 1000)
                        i[0]++
                    } else {
                        sendParking()
                        h.removeCallbacks(this)
                    }
                }
            }
            h.postDelayed(r, 1000)
        }

        fun sendCornering() {
            stop = true
            val chara = mGattServer?.getService(UUID.fromString(SERVICE_CONFIG))?.getCharacteristic(UUID.fromString(CHARACTERISTIC_STATUS))
            chara?.value = MathUtils.getBytes(4)
            if(connectedDevice==null)return
            mGattServer?.notifyCharacteristicChanged(connectedDevice, chara, false)
        }

        fun sendBraking() {
            stop = true
            val chara = mGattServer?.getService(UUID.fromString(SERVICE_CONFIG))?.getCharacteristic(UUID.fromString(CHARACTERISTIC_STATUS))
            chara?.value = MathUtils.getBytes(2)
            if(connectedDevice==null)return
            mGattServer?.notifyCharacteristicChanged(connectedDevice, chara, false)
        }

        fun sendParking() {
            stop = true
            val chara = mGattServer?.getService(UUID.fromString(SERVICE_CONFIG))?.getCharacteristic(UUID.fromString(CHARACTERISTIC_STATUS))
            chara?.value = MathUtils.getBytes(0)
            if(connectedDevice==null)return
            mGattServer?.notifyCharacteristicChanged(connectedDevice, chara, false)
        }

        fun sendCrash() {
            stop = true
            val chara = mGattServer?.getService(UUID.fromString(SERVICE_CONFIG))?.getCharacteristic(UUID.fromString(CHARACTERISTIC_STATUS))
            chara?.value = MathUtils.getBytes(6)
            mGattServer?.notifyCharacteristicChanged(connectedDevice, chara, false)
        }

        val CREATOR: Parcelable.Creator<B810Beacon?> = object : Parcelable.Creator<B810Beacon?> {
            override fun createFromParcel(source: Parcel): B810Beacon? {
                return B810Beacon(source)
            }

            override fun newArray(size: Int): Array<B810Beacon?> {
                return arrayOfNulls(size)
            }
        }

        fun calibrateAcceleration() {
            Log.i("GATT", "Calibrate Aceleration")
            stop = false
            calibrateStatus = true
            calibrateCallback()
            if (manualCalib) {
                manualCalibration()
            } else {
                for (i in 1..10) {
                    if (stop) {
                        return
                    }
                    sendIdleAcceleration(1000 - (i * 100), 1000 - (i * 100))
                    Thread.sleep(600)
                }
            }
        }

        fun manualCalibration() {
            val h = Handler(Looper.getMainLooper())
            lateinit var runnable: Runnable
            runnable = Runnable {
                if (!stopCalib) {
                    sendIdleAcceleration(calib_x, calib_y, calib_z)
                    h.postDelayed(runnable, 600)
                }else{
                    h.removeCallbacks(runnable)
                }
            }
            h.post(runnable)

        }

        fun sendIdleAcceleration(x: Int = 0, y: Int = 0, z: Int = 950) {
            val data = ByteArray(6)
            MathUtils.copyBytes(data, x, y, z)
            val chara = mGattServer?.getService(UUID.fromString(SERVICE_MEMS))?.getCharacteristic(UUID.fromString(CHARACTERISTIC_ACCELERATION))
            chara?.value = data
            mGattServer?.notifyCharacteristicChanged(connectedDevice, chara, false)
            Log.i("GATT: ", "send acceleration =" + Arrays.toString(data))
        }


        private fun sendCrashBuffer() {
            val index = intArrayOf(1)
            val h = Handler(Looper.getMainLooper())
            Log.i("GATT", "crash buffer start")
            val r: Runnable = object : Runnable {
                override fun run() {
                    if (index[0] <= 601) {
                        val value = byteArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
                        crashProgressCallback(index[0])
                        if (index[0] > 599) {
                            val time = MathUtils.getBytes(System.currentTimeMillis())
                            System.arraycopy(time, 0, value, 2, 4)
                        }
                        System.arraycopy(MathUtils.getBytes(index[0]), 0, value, 0, 2)
                        val charaDrive = mGattServer?.getService(UUID.fromString(SERVICE_MEMORY))
                                ?.getCharacteristic(UUID.fromString(CHARACTERISTIC_CRASH_BUFFER))
                        charaDrive?.value = value
                        if(connectedDevice==null)return
                        mGattServer?.notifyCharacteristicChanged(connectedDevice, charaDrive, false)
                        index[0]++
                        h.postDelayed(this, 50)
                        Log.i("GATT", "crash buffer index: " + index[0])
                    } else {
                        val chara = mGattServer?.getService(UUID.fromString(SERVICE_CONFIG))?.getCharacteristic(UUID.fromString(CHARACTERISTIC_STATUS))
                        chara?.value = MathUtils.getBytes(26)
                        if(connectedDevice==null)return
                        mGattServer?.notifyCharacteristicChanged(connectedDevice, chara, false)
                        h.removeCallbacks(this)
                        Log.i("GATT", "crash buffer end: " + index[0])
                    }
                }
            }
            h.postDelayed(r, 100)
        }

        fun buildCrash(value: ByteArray): ByteArray {
            val data = byteArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
            if (MathUtils.getInt(value) > 599) {
                val time = MathUtils.getBytes(System.currentTimeMillis())
                System.arraycopy(time, 0, value, 2, 4)
            }
            System.arraycopy(value, 0, value, 0, 2)
            return data
        }

        fun automaticCalibration() {

        }


    }
}