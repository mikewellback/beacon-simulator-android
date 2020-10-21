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
package net.alea.beaconsimulator.component

import android.content.Context
import android.content.SharedPreferences
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.Toast
import kotlinx.android.synthetic.main.card_beacon_b810beacon_edit.view.*
import net.alea.beaconsimulator.R
import net.alea.beaconsimulator.bluetooth.model.B810Beacon
import net.alea.beaconsimulator.bluetooth.model.B810Beacon.Companion.sendAcceleration
import net.alea.beaconsimulator.bluetooth.model.B810Beacon.Companion.sendCrash
import net.alea.beaconsimulator.bluetooth.model.B810Beacon.Companion.sendParking
import net.alea.beaconsimulator.bluetooth.model.BeaconModel
import net.alea.beaconsimulator.util.onProgressChange
import java.util.*
import kotlin.math.max
import kotlin.math.min

class ViewEditB810Beacon(context: Context) : FrameLayout(context), BeaconModelEditor {
    private val pref: SharedPreferences = context.getSharedPreferences("crash_pref", Context.MODE_PRIVATE)
    private val edit: SharedPreferences.Editor = pref.edit()


    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = inflater.inflate(R.layout.card_beacon_b810beacon_edit, this)
        cardb810beacon_textinput_uuid.addTextChangedListener(object : SimplifiedTextWatcher() {
            override fun afterTextChanged(s: Editable) {
                checkUuidValue()
            }
        })
        cardb810beacon_textinput_major.addTextChangedListener(object : SimplifiedTextWatcher() {
            override fun afterTextChanged(s: Editable) {
                calculateSerial()
            }
        })
        cardb810beacon_textinput_minor.addTextChangedListener(object : SimplifiedTextWatcher() {
            override fun afterTextChanged(s: Editable) {
                calculateSerial()
            }
        })
        cardb810beacon_textinput_serial.addTextChangedListener(object : SimplifiedTextWatcher() {
            override fun afterTextChanged(s: Editable) {
                checkSerialValue()
            }
        })
        cardb810beacon_textinput_power.addTextChangedListener(object : SimplifiedTextWatcher() {
            override fun afterTextChanged(s: Editable) {
                checkPowerValue()
            }
        })
        cardb810beacon_textinput_manufacturerid.addTextChangedListener(object : SimplifiedTextWatcher() {
            override fun afterTextChanged(s: Editable) {
                checkManufacturerIdValue()
            }
        })


        firmwareVersion_texxtinput.setText("02.16.00")
        cardb810beacon_button_resetuuid.setOnClickListener { cardb810beacon_textinput_uuid.setText(UUID.fromString("B810736B-11FC-85C3-1762-80DF658F0B31").toString()) }
        start_acceleration.setOnClickListener {
            var time = 0
            if (timeAccelerationEt.text.toString() != "") {
                time = timeAccelerationEt.text.toString().toInt()
            }
            sendAcceleration(time)
        }
        stop.setOnClickListener { sendParking() }
        crash.setOnClickListener {
            sendCrash()
        }

        addCrash.setOnClickListener {
            val count = min(pref.getInt("crashCountTxt", 0) + 1, 8)
            edit.putInt("crashCountTxt", count).commit()
            crashCountTxt.text = "$count"
        }

        calibManual.setOnCheckedChangeListener { _, checked ->
            B810Beacon.manualCalib = checked
            enableManualCalib(if (checked) VISIBLE else GONE)
        }

        calib_seek.onProgressChange {
            B810Beacon.calib_x = it
            calib_value.text = "$it"
        }
        calib_seekY.onProgressChange {
            B810Beacon.calib_y = it
            calib_valueY.text = "$it"
        }
        calib_seekZ.onProgressChange {
            B810Beacon.calib_z = it
            calib_valueZ.text = "$it"
        }
        removeCrash.setOnClickListener {
            val count = max(pref.getInt("crashCountTxt", 0) - 1, 0)
            edit.putInt("crashCountTxt", count).commit()
            crashCountTxt.text = "$count"
        }

        crashCountTxt.text = "${pref.getInt("crashCountTxt", 0)}"

        B810Beacon.crashProgressCallback = {
            findViewById<ProgressBar>(R.id.progressCrash).progress = it
            if (it > 600) {
                val count = max(pref.getInt("crashCountTxt", 0) - 1, 0)
                edit.putInt("crashCountTxt", count).commit()
                crashCountTxt.text = "$count"
                Toast.makeText(context, "sending crash completed", Toast.LENGTH_LONG).show()
                if (pref.getInt("crashCountTxt", 0) > 0) {
                    Handler().postDelayed({
                        sendCrash()
                    }, 1000)
                }
            }

        }

        if (B810Beacon.connecttionStatus) {
            connected_indicator.setBackgroundResource(R.drawable.connected_shape)
            if (B810Beacon.calibrateStatus) {
                enableButtons(true)
            }
        } else {
            enableButtons(false)
            connected_indicator.setBackgroundResource(R.drawable.diconnected_shape)
        }
        B810Beacon.connectedCallback = { connected ->
            post {
                if (connected) {
                    connected_indicator.setBackgroundResource(R.drawable.connected_shape)
                } else {
                    enableButtons(false)
                    connected_indicator.setBackgroundResource(R.drawable.diconnected_shape)
                }
            }

        }
        cornering.setOnClickListener {
            B810Beacon.sendCornering()
        }
        braking.setOnClickListener {
            B810Beacon.sendBraking()
        }

        B810Beacon.calibrateCallback = {
            enableButtons(true)
        }

        stopCalibrBtn.setOnClickListener {
            B810Beacon.stopCalib = true
        }


//        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
//        val sensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
//
//
//        // Create a listener
//
//        // Create a listener
//        val gyroscopeSensorListener = object : SensorEventListener {
//            override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
//            }
//
//            override fun onSensorChanged(p0: SensorEvent?) {
////                Log.i("TAG","gyros: "+p0!!.accuracy)
//                Log.i("TAG","gyros: "+p0!!.values.get(0))
//
//
//            }
//
//        }

//        sensorManager.registerListener(gyroscopeSensorListener,
//                sensor, 1000);

    }



    override fun loadModelFrom(model: BeaconModel) {
        val b810beacon = model.b810beacon ?: return
        cardb810beacon_textinput_uuid.setText(b810beacon.beaconNamespace.toString())
        cardb810beacon_textinput_major.setText(String.format(Locale.ENGLISH, "%d", b810beacon.getMajor()))
        cardb810beacon_textinput_minor.setText(String.format(Locale.ENGLISH, "%d", b810beacon.getMinor()))
        firmwareVersion_texxtinput.setText(b810beacon.firmwareVersion)
        calculateSerial()
        cardb810beacon_textinput_power.setText(String.format(Locale.ENGLISH, "%d", b810beacon.power))
        cardb810beacon_textinput_manufacturerid.setText(String.format(Locale.ENGLISH, "%d", b810beacon.getManufacturerId()))
    }

    override fun saveModelTo(model: BeaconModel): Boolean {
        if (!checkAll()) {
            return false
        }
        val b810beacon = B810Beacon()
        b810beacon.firmwareVersion = firmwareVersion_texxtinput.text.toString()
        b810beacon.beaconNamespace = UUID.fromString(cardb810beacon_textinput_uuid.text.toString())
        b810beacon.setMajor(cardb810beacon_textinput_major.text.toString().toInt())
        b810beacon.setMinor(cardb810beacon_textinput_minor.text.toString().toInt())
        b810beacon.serial = cardb810beacon_textinput_serial.text.toString()
        b810beacon.power = cardb810beacon_textinput_power.text.toString().toByte()
        b810beacon.setManufacturerId(cardb810beacon_textinput_manufacturerid.text.toString().toInt())
        model.setB810Beacon(b810beacon)
        return true
    }

    override fun setEditMode(editMode: Boolean) {
        cardb810beacon_textinput_uuid.isEnabled = editMode
        cardb810beacon_textinput_major.isEnabled = editMode
        cardb810beacon_textinput_minor.isEnabled = editMode
        firmwareVersion_texxtinput.isEnabled = editMode
        cardb810beacon_textinput_serial.isEnabled = editMode
        cardb810beacon_textinput_power.isEnabled = editMode
        cardb810beacon_textinput_manufacturerid.isEnabled = editMode
        cardb810beacon_button_resetuuid.visibility = if (editMode) VISIBLE else GONE
        cardb810beacon_textview_power.visibility = if (editMode) VISIBLE else GONE
    }

    private fun checkUuidValue(): Boolean {
        try {
            val uuid = cardb810beacon_textinput_uuid.text.toString()
            require(uuid.length >= 36)
            UUID.fromString(uuid)
            cardb810beacon_textinputlayout_uuid.error = null
        } catch (e: IllegalArgumentException) {
            cardb810beacon_textinputlayout_uuid.error = resources.getString(R.string.edit_error_uuid)
            return false
        }
        return true
    }

    private fun checkPowerValue(): Boolean {
        var isValid = false
        try {
            val power = cardb810beacon_textinput_power.text.toString().toInt()
            if (power >= -128 && power <= 127) {
                isValid = true
            }
        } catch (e: NumberFormatException) {
            // not valid, isValid already false
        }
        if (isValid) {
            cardb810beacon_textinputlayout_power.error = null
        } else {
            cardb810beacon_textinputlayout_power.error = resources.getString(R.string.edit_error_signed_byte)
        }
        return isValid
    }

    private fun checkMajorValue(): Boolean {
        var isValid = false
        try {
            val major = cardb810beacon_textinput_major.text.toString().toInt()
            if (major >= 0 && major <= 65535) {
                isValid = true
            }
        } catch (e: NumberFormatException) {
            // not valid, isValid already false
        }
        if (isValid) {
            cardb810beacon_textinputlayout_major.error = null
        } else {
            cardb810beacon_textinputlayout_major.error = resources.getString(R.string.edit_error_unsigned_short)
        }
        return isValid
    }

    private fun checkMinorValue(): Boolean {
        var isValid = false
        try {
            val minor = cardb810beacon_textinput_minor.text.toString().toInt()
            if (minor >= 0 && minor <= 65535) {
                isValid = true
            }
        } catch (e: NumberFormatException) {
            // not valid, isValid already false
        }
        if (isValid) {
            cardb810beacon_textinputlayout_minor.error = null
        } else {
            cardb810beacon_textinputlayout_minor.error = resources.getString(R.string.edit_error_unsigned_short)
        }
        return isValid
    }

    private fun checkSerialValue() {
        var isValid = false
        cardb810beacon_textinput_serial.text?.let {
            if (it.length < 8) {
                cardb810beacon_textinput_serial.error = null
                return
            } else if (it.length > 8) {
                cardb810beacon_textinput_serial.error = resources.getString(R.string.edit_error_8_hex_characters)
                return
            }
            var serialMajor = 0
            var serialMinor = 0
            try {
                serialMajor = it.toString().substring(1, 4).toInt(16)
                serialMinor = it.toString().substring(4, 8).toInt(16)
                if (serialMajor >= 0 && serialMajor <= 65535 && serialMinor >= 0 && serialMinor <= 65535) {
                    isValid = true
                }
            } catch (e: NumberFormatException) {
                // not valid, isValid already false
            }
            if (isValid) {
                if (serialMajor.toString() != cardb810beacon_textinput_major.text.toString()) {
                    cardb810beacon_textinput_major.setText(serialMajor.toString())
                }
                if (serialMinor.toString() != cardb810beacon_textinput_minor.text.toString()) {
                    cardb810beacon_textinput_minor.setText(serialMinor.toString())
                }
                cardb810beacon_textinput_serial.error = null
            } else {
                cardb810beacon_textinput_serial.error = resources.getString(R.string.edit_error_8_hex_characters)
            }
        }

    }

    private fun calculateSerial() {
        if (checkMajorValue() && checkMinorValue()) {
            var isValid = false
            var serialMajor = 0
            var serialMinor = 0
            try {
                serialMajor = cardb810beacon_textinput_major.text.toString().toInt()
                serialMinor = cardb810beacon_textinput_minor.text.toString().toInt()
                if (serialMajor >= 0 && serialMajor <= 65535 && serialMinor >= 0 && serialMinor <= 65535) {
                    isValid = true
                }
            } catch (e: NumberFormatException) {
                // not valid, isValid already false
            }
            if (isValid) {
                val major = Integer.toString(serialMajor, 16)
                val minor = Integer.toString(serialMinor, 16)
                val serial = ("1" + "000$major".substring(major.length) + "0000$minor".substring(minor.length)).toUpperCase()
                if (serial != cardb810beacon_textinput_serial.text.toString()) {
                    cardb810beacon_textinput_serial.setText(serial)
                }
            }
        }
    }

    private fun checkManufacturerIdValue(): Boolean {
        var isValid = false
        try {
            val manufacturerId = cardb810beacon_textinput_manufacturerid.text.toString().toInt()
            if (manufacturerId >= 0 && manufacturerId <= 65535) {
                isValid = true
            }
        } catch (e: NumberFormatException) {
            // not valid, isValid already false
        }
        if (isValid) {
            cardb810beacon_textinputlayout_manufacturerid.error = null
        } else {
            cardb810beacon_textinputlayout_manufacturerid.error = resources.getString(R.string.edit_error_unsigned_short)
        }
        return isValid
    }

    private fun checkAll(): Boolean {
        return (checkPowerValue() and checkUuidValue() and checkMajorValue() and checkMinorValue()
                && checkManufacturerIdValue())
    }

    private abstract inner class SimplifiedTextWatcher : TextWatcher {
        override fun beforeTextChanged(s: CharSequence, start: Int,
                                       count: Int, after: Int) {
        }

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
    }

    companion object {
        private const val SIZE_HEXA_RESERVED_VALUE = 2
    }


    fun enableButtons(enable: Boolean) {
        post {
//            startBtn.isEnabled = enable
//            stop.isEnabled = enable
//            stop.isEnabled = enable
//            cornering.isEnabled = enable
//            braking.isEnabled = enable
        }
    }

    fun enableManualCalib(visibility: Int) {
        calib_seek.visibility = visibility
        calib_seekY.visibility = visibility
        calib_seekZ.visibility = visibility
        calib_value.visibility = visibility
        calib_valueY.visibility = visibility
        calib_valueZ.visibility = visibility
    }
}