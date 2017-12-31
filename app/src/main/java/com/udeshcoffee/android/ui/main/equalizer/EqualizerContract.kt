package com.udeshcoffee.android.ui.main.equalizer

import android.media.audiofx.Equalizer
import com.udeshcoffee.android.service.AudioFXHelper
import com.udeshcoffee.android.ui.BasePresenter
import com.udeshcoffee.android.ui.BaseView

/**
 * Created by Udathari on 9/28/2017.
 */
interface EqualizerContract {

    interface View: BaseView<Presenter> {

        fun setEnableAction()

        fun setEnabled(enabled: Boolean)

        fun setSaveAction(type: Int)

        fun setVirtualizerEnabled(enabled: Boolean, byAction: Boolean)

        fun setVirtualizer(amount: Int)

        fun setBassBoostEnabled(enabled: Boolean, byAction: Boolean)

        fun setBassBoost(amount: Int)

        fun setReverbs(reverbs: ArrayList<String>, initReverb: Int)

        fun setPresets(presets: ArrayList<String>, initPreset: Int)

        fun setPreset(preset: Int)

        fun createBands(equalizer: Equalizer)

        fun setBands(equalizer: Equalizer)

        fun showSaveEQDialog()

        fun showDeleteEQDialog(preset: Int, name: String)

    }

    interface Presenter: BasePresenter {

        fun actionEnable(enable: Boolean)

        fun actionSaveOrDelete()

        fun deletePreset(preset: Int)

        fun actionVirtualizerEnable(enable: Boolean)

        fun changeVirtualizer(amount: Int)

        fun actionBassBoostEnable(enable: Boolean)

        fun changeBassBoost(amount: Int)

        fun changeFrequency(band: Int, amount: Int)

        fun saveUserPreset(name: String)

        fun setFrequency(band: Int, amount: Int)

        fun pickPreset(preset: Int)

        fun pickReverb(reverb: Int)

        val helper: AudioFXHelper?

        val enabled: Boolean?

    }
}