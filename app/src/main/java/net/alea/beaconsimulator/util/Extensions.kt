package net.alea.beaconsimulator.util

import android.widget.SeekBar

fun SeekBar.onProgressChange(listener:(value:Int)->Unit){
    setOnSeekBarChangeListener(object :SeekBar.OnSeekBarChangeListener{
        override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
            listener(p1)
        }
        override fun onStartTrackingTouch(p0: SeekBar?) {}
        override fun onStopTrackingTouch(p0: SeekBar?) {}
    })
}