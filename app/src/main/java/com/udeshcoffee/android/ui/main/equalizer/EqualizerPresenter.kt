package com.udeshcoffee.android.ui.main.equalizer

import android.util.Log
import com.udeshcoffee.android.extensions.getService
import com.udeshcoffee.android.service.AudioFXHelper
import org.koin.standalone.KoinComponent
import java.util.*

/**
* Created by Udathari on 9/28/2017.
*/
class EqualizerPresenter: EqualizerContract.Presenter, KoinComponent {

    val TAG = "EqualizerPresenter"

    override lateinit var view: EqualizerContract.View

    override val helper: AudioFXHelper? = getService()?.audioFXHelper

    override val enabled: Boolean?
        get() = helper?.equalizerEnabled

    private var presetCount = 0
    private var userCount = 0

    object SaveType {
        const val CAN_SAVE = 0
        const val CANT_SAVE = 1
        const val DELETE = 2
    }

    override fun start() {
        helper?.apply {
            view.setEnableAction()
            equalizer?.let{
                view.createBands(it)
                view.setEnabled(equalizerEnabled)

                presetCount = it.numberOfPresets.toInt()
                userCount = getUserPresetCount()

                loadPresets(this)

                view.setVirtualizerEnabled(virtualizerEnabled, false)
                view.setVirtualizer(virtualizerStrength.toInt())

                view.setBassBoostEnabled(bassboostEnabled, false)
                view.setBassBoost(bassboostStrength.toInt())

                val reverbPresets = ArrayList<String>()
                reverbPresets.add("None")
                reverbPresets.add("Large Hall")
                reverbPresets.add("Large Room")
                reverbPresets.add("Medium Hall")
                reverbPresets.add("Medium Room")
                reverbPresets.add("Small Room")
                reverbPresets.add("Plate")
                view.setReverbs(reverbPresets, presetReverbPreset.toInt())
            }
        }

    }

    override fun stop() {

    }

    override fun actionEnable(enable: Boolean) {
        helper?.equalizerEnabled = enable
        view.setEnabled(enable)
    }

    override fun actionSaveOrDelete() {
        helper?.apply {
            val tempPreset = preset
            if (tempPreset == -1)
                view.showSaveEQDialog()
            else {
                view.showDeleteEQDialog(tempPreset, getUserPresetName(tempPreset - presetCount))
            }
        }
    }

    override fun deletePreset(preset: Int) {
        helper?.apply {
            deleteUserPreset(preset - presetCount)
            userCount = getUserPresetCount()
            this.preset = -1
            loadPresets(helper)
        }
    }

    override fun actionVirtualizerEnable(enable: Boolean) {
        helper?.virtualizerEnabled = enable
        view.setVirtualizerEnabled(enable, true)
    }

    override fun changeVirtualizer(amount: Int) {
        helper?.virtualizerStrength = amount.toShort()
    }

    override fun actionBassBoostEnable(enable: Boolean) {
        helper?.bassboostEnabled = enable
        view.setBassBoostEnabled(enable, true)
    }

    override fun changeBassBoost(amount: Int) {
        helper?.bassboostStrength = amount.toShort()
    }

    override fun changeFrequency(band: Int, amount: Int) {
        helper?.equalizer?.apply {
            val db = amount + bandLevelRange[0]
            setBandLevel(band.toShort(), db.toShort())
            Log.d(TAG, "Band: $band Amount: $amount DB: $db}")
        }
    }

    override fun setFrequency(band: Int, amount: Int) {
        helper?.apply {
            equalizer?.let { setEqualizerBandStrength(band.toShort(), (amount + it.bandLevelRange[0]).toShort()) }
            if (preset != -1) {
                preset = -1
                view.setPreset(presetCount + userCount)
            }
        }
    }

    override fun saveUserPreset(name: String) {
        helper?.saveUserPreset(name)
        helper?.let {
            it.preset = presetCount + userCount
            userCount = it.getUserPresetCount()
            loadPresets(it)
        }
    }

    override fun pickPreset(preset: Int) {
        when {
            preset == presetCount + userCount -> {
                helper?.preset = -1
                view.setSaveAction(SaveType.CAN_SAVE)
            }
            preset < presetCount -> {
                helper?.preset = preset
                view.setSaveAction(SaveType.CANT_SAVE)
            }
            else -> {
                helper?.preset = preset
                view.setSaveAction(SaveType.DELETE)
            }
        }
        helper?.equalizer?.let {
            view.setBands(it)
        }
    }

    override fun pickReverb(reverb: Int) {
        helper?.presetReverbPreset = reverb.toShort()
    }

    private fun loadPresets(helper: AudioFXHelper) {
        helper.apply {
            val eqPresets = (0 until presetCount).mapTo(ArrayList<String>()) { equalizer!!.getPresetName(it.toShort()) }
            (0 until userCount).mapTo(eqPresets) { getUserPresetName(it) }
            eqPresets.add("Custom")
            val tempPreset = if (preset == -1) {
                view.setSaveAction(SaveType.CAN_SAVE)
                presetCount + userCount
            } else if(preset < presetCount) {
                view.setSaveAction(SaveType.CANT_SAVE)
                preset
            } else {
                view.setSaveAction(SaveType.DELETE)
                preset
            }
            view.setPresets(eqPresets, tempPreset)
        }
    }

}