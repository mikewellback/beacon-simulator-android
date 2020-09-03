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
import android.support.design.widget.TextInputEditText
import android.support.design.widget.TextInputLayout
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import net.alea.beaconsimulator.R
import net.alea.beaconsimulator.bluetooth.model.B810Beacon
import net.alea.beaconsimulator.bluetooth.model.B810Beacon.Companion.sendAcceleration
import net.alea.beaconsimulator.bluetooth.model.B810Beacon.Companion.sendCrash
import net.alea.beaconsimulator.bluetooth.model.B810Beacon.Companion.sendParking
import net.alea.beaconsimulator.bluetooth.model.BeaconModel
import java.util.*

class ViewEditB810Beacon(context: Context) : FrameLayout(context), BeaconModelEditor {
    private val mUuidLayout: TextInputLayout
    private val mMajorLayout: TextInputLayout
    private val mMinorLayout: TextInputLayout
    private val mSerialLayout: TextInputLayout
    private val mPowerLayout: TextInputLayout
    private val mManufacturerIdLayout: TextInputLayout
    private val mUuidValue: TextInputEditText
    private val mMajorValue: TextInputEditText
    private val mMinorValue: TextInputEditText
    private val mSerialValue: TextInputEditText
    private val mPowerValue: TextInputEditText
    private val mManufacturerIdValue: TextInputEditText
    private val mTime: TextInputEditText
    private val mTxPowerInfo: TextView
    private val mUuidButton: Button
    override fun loadModelFrom(model: BeaconModel) {
        val b810beacon = model.b810beacon ?: return
        mUuidValue.setText(b810beacon.beaconNamespace.toString())
        mMajorValue.setText(String.format(Locale.ENGLISH, "%d", b810beacon.getMajor()))
        mMinorValue.setText(String.format(Locale.ENGLISH, "%d", b810beacon.getMinor()))
        calculateSerial()
        mPowerValue.setText(String.format(Locale.ENGLISH, "%d", b810beacon.power))
        mManufacturerIdValue.setText(String.format(Locale.ENGLISH, "%d", b810beacon.getManufacturerId()))
    }

    override fun saveModelTo(model: BeaconModel): Boolean {
        if (!checkAll()) {
            return false
        }
        val b810beacon = B810Beacon()
        b810beacon.beaconNamespace = UUID.fromString(mUuidValue.text.toString())
        b810beacon.setMajor(mMajorValue.text.toString().toInt())
        b810beacon.setMinor(mMinorValue.text.toString().toInt())
        b810beacon.serial = mSerialValue.text.toString()
        b810beacon.power = mPowerValue.text.toString().toByte()
        b810beacon.setManufacturerId(mManufacturerIdValue.text.toString().toInt())
        model.setB810Beacon(b810beacon)
        return true
    }

    override fun setEditMode(editMode: Boolean) {
        mUuidValue.isEnabled = editMode
        mMajorValue.isEnabled = editMode
        mMinorValue.isEnabled = editMode
        mSerialValue.isEnabled = editMode
        mPowerValue.isEnabled = editMode
        mManufacturerIdValue.isEnabled = editMode
        mUuidButton.visibility = if (editMode) View.VISIBLE else View.GONE
        mTxPowerInfo.visibility = if (editMode) View.VISIBLE else View.GONE
    }

    private fun checkUuidValue(): Boolean {
        try {
            val uuid = mUuidValue.text.toString()
            require(uuid.length >= 36)
            UUID.fromString(uuid)
            mUuidLayout.error = null
        } catch (e: IllegalArgumentException) {
            mUuidLayout.error = resources.getString(R.string.edit_error_uuid)
            return false
        }
        return true
    }

    private fun checkPowerValue(): Boolean {
        var isValid = false
        try {
            val power = mPowerValue.text.toString().toInt()
            if (power >= -128 && power <= 127) {
                isValid = true
            }
        } catch (e: NumberFormatException) {
            // not valid, isValid already false
        }
        if (isValid) {
            mPowerLayout.error = null
        } else {
            mPowerLayout.error = resources.getString(R.string.edit_error_signed_byte)
        }
        return isValid
    }

    private fun checkMajorValue(): Boolean {
        var isValid = false
        try {
            val major = mMajorValue.text.toString().toInt()
            if (major >= 0 && major <= 65535) {
                isValid = true
            }
        } catch (e: NumberFormatException) {
            // not valid, isValid already false
        }
        if (isValid) {
            mMajorLayout.error = null
        } else {
            mMajorLayout.error = resources.getString(R.string.edit_error_unsigned_short)
        }
        return isValid
    }

    private fun checkMinorValue(): Boolean {
        var isValid = false
        try {
            val minor = mMinorValue.text.toString().toInt()
            if (minor >= 0 && minor <= 65535) {
                isValid = true
            }
        } catch (e: NumberFormatException) {
            // not valid, isValid already false
        }
        if (isValid) {
            mMinorLayout.error = null
        } else {
            mMinorLayout.error = resources.getString(R.string.edit_error_unsigned_short)
        }
        return isValid
    }

    private fun checkSerialValue() {
        var isValid = false
        if (mSerialValue.text.length < 8) {
            mSerialValue.error = null
            return
        } else if (mSerialValue.text.length > 8) {
            mSerialValue.error = resources.getString(R.string.edit_error_8_hex_characters)
            return
        }
        var serialMajor = 0
        var serialMinor = 0
        try {
            serialMajor = mSerialValue.text.toString().substring(1, 4).toInt(16)
            serialMinor = mSerialValue.text.toString().substring(4, 8).toInt(16)
            if (serialMajor >= 0 && serialMajor <= 65535 && serialMinor >= 0 && serialMinor <= 65535) {
                isValid = true
            }
        } catch (e: NumberFormatException) {
            // not valid, isValid already false
        }
        if (isValid) {
            if (serialMajor.toString() != mMajorValue.text.toString()) {
                mMajorValue.setText(serialMajor.toString())
            }
            if (serialMinor.toString() != mMinorValue.text.toString()) {
                mMinorValue.setText(serialMinor.toString())
            }
            mSerialValue.error = null
        } else {
            mSerialValue.error = resources.getString(R.string.edit_error_8_hex_characters)
        }
    }

    private fun calculateSerial() {
        if (checkMajorValue() && checkMinorValue()) {
            var isValid = false
            var serialMajor = 0
            var serialMinor = 0
            try {
                serialMajor = mMajorValue.text.toString().toInt()
                serialMinor = mMinorValue.text.toString().toInt()
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
                if (serial != mSerialValue.text.toString()) {
                    mSerialValue.setText(serial)
                }
            }
        }
    }

    private fun checkManufacturerIdValue(): Boolean {
        var isValid = false
        try {
            val manufacturerId = mManufacturerIdValue.text.toString().toInt()
            if (manufacturerId >= 0 && manufacturerId <= 65535) {
                isValid = true
            }
        } catch (e: NumberFormatException) {
            // not valid, isValid already false
        }
        if (isValid) {
            mManufacturerIdLayout.error = null
        } else {
            mManufacturerIdLayout.error = resources.getString(R.string.edit_error_unsigned_short)
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

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = inflater.inflate(R.layout.card_beacon_b810beacon_edit, this)
        mTxPowerInfo = view.findViewById<View>(R.id.cardb810beacon_textview_power) as TextView
        mUuidLayout = view.findViewById<View>(R.id.cardb810beacon_textinputlayout_uuid) as TextInputLayout
        mMajorLayout = view.findViewById<View>(R.id.cardb810beacon_textinputlayout_major) as TextInputLayout
        mMinorLayout = view.findViewById<View>(R.id.cardb810beacon_textinputlayout_minor) as TextInputLayout
        mSerialLayout = view.findViewById<View>(R.id.cardb810beacon_textinputlayout_serial) as TextInputLayout
        mPowerLayout = view.findViewById<View>(R.id.cardb810beacon_textinputlayout_power) as TextInputLayout
        mManufacturerIdLayout = view.findViewById<View>(R.id.cardb810beacon_textinputlayout_manufacturerid) as TextInputLayout
        mUuidValue = view.findViewById<View>(R.id.cardb810beacon_textinput_uuid) as TextInputEditText
        mUuidValue.addTextChangedListener(object : SimplifiedTextWatcher() {
            override fun afterTextChanged(s: Editable) {
                checkUuidValue()
            }
        })
        mMajorValue = view.findViewById<View>(R.id.cardb810beacon_textinput_major) as TextInputEditText
        mMajorValue.addTextChangedListener(object : SimplifiedTextWatcher() {
            override fun afterTextChanged(s: Editable) {
                calculateSerial()
            }
        })
        mMinorValue = view.findViewById<View>(R.id.cardb810beacon_textinput_minor) as TextInputEditText
        mMinorValue.addTextChangedListener(object : SimplifiedTextWatcher() {
            override fun afterTextChanged(s: Editable) {
                calculateSerial()
            }
        })
        mSerialValue = view.findViewById<View>(R.id.cardb810beacon_textinput_serial) as TextInputEditText
        mSerialValue.addTextChangedListener(object : SimplifiedTextWatcher() {
            override fun afterTextChanged(s: Editable) {
                checkSerialValue()
            }
        })
        mPowerValue = view.findViewById<View>(R.id.cardb810beacon_textinput_power) as TextInputEditText
        mPowerValue.addTextChangedListener(object : SimplifiedTextWatcher() {
            override fun afterTextChanged(s: Editable) {
                checkPowerValue()
            }
        })
        mManufacturerIdValue = view.findViewById<View>(R.id.cardb810beacon_textinput_manufacturerid) as TextInputEditText
        mManufacturerIdValue.addTextChangedListener(object : SimplifiedTextWatcher() {
            override fun afterTextChanged(s: Editable) {
                checkManufacturerIdValue()
            }
        })
        mUuidButton = view.findViewById<View>(R.id.cardb810beacon_button_resetuuid) as Button
        mUuidButton.setOnClickListener { mUuidValue.setText(UUID.fromString("B810736B-11FC-85C3-1762-80DF658F0B31").toString()) }
        mTime = view.findViewById(R.id.timeAccelerationEt)
        view.findViewById<View>(R.id.start_acceleration).setOnClickListener {
            var time = 0
            if (mTime.text.toString() != null && mTime.text.toString() != "") {
                time = mTime.text.toString().toInt()
            }
            sendAcceleration(time)
        }
        view.findViewById<View>(R.id.stop).setOnClickListener { sendParking() }
        view.findViewById<View>(R.id.crash).setOnClickListener {
//            view.findViewById<View>(R.id.crash).isEnabled = false
            sendCrash()
        }

        B810Beacon.crashProgressCallback = {
            view.findViewById<ProgressBar>(R.id.progressCrash).progress = it
            if (it > 600) {
//                view.findViewById<View>(R.id.crash).isEnabled = true
                Toast.makeText(context, "sending crash completed", Toast.LENGTH_LONG).show()
            }

        }
        val indicator = view.findViewById<View>(R.id.connected_indicator)
        if (B810Beacon.connecttionStatus) {
            indicator.setBackgroundResource(R.drawable.connected_shape)
        } else {
            indicator.setBackgroundResource(R.drawable.diconnected_shape)
        }
        B810Beacon.connectedCallback = { connected ->
            view.post {
                if (connected) {
                    indicator.setBackgroundResource(R.drawable.connected_shape)
                } else {
                    indicator.setBackgroundResource(R.drawable.diconnected_shape)
                }
            }

        }
        view.findViewById<Button>(R.id.cornering).setOnClickListener {
            B810Beacon.sendCornering()
        }
        view.findViewById<Button>(R.id.braking).setOnClickListener {
            B810Beacon.sendBraking()
        }

    }
}