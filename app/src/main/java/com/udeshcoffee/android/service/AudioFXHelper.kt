package com.udeshcoffee.android.service

import android.content.Context
import androidx.preference.PreferenceManager
import android.content.SharedPreferences
import android.media.audiofx.*
import android.os.Build
import android.util.Log
import com.udeshcoffee.android.data.DataRepository
import com.udeshcoffee.android.data.model.EQPreset

/**
 * Created by Udathari on 7/27/2017.
 */
class AudioFXHelper private constructor(private val dataRepository: DataRepository) {

    companion object {
        const val TAG = "AudioFxHelper"

        const val EQUALIZER_PRESET_PREF = "eqpreset"
        const val EQUALIZER_USER_PRESET_PREF = "equser"
        const val EQUALIZER_FQ_PREFIX = "eq"

        const val EQUALIZER_ENABLED_PREF = "eq_enabled"
        const val BASSBOOST_ENABLED_PREF = "bassboost_enabled"
        const val VIRTUALIZER_ENABLED_PREF = "virtualizer_enabled"
        const val PRESETREVERB_ENABLED_PREF = "preset_reverb_enabled"
        const val LOUDNESSENHANCER_ENABLED_PREF = "loudness_enhancer_enabled"

        const val BASSBOOST_STRENGTH_PREF = "bassboost_strength"
        const val VIRTUALIZER_STRENGTH_PREF = "virtualizer_strength"
        const val PRESETREVERB_PRESET_PREF = "preset_reverb_preset"
        const val LOUDNESSENHANCER_STRENGTH_PREF = "loudness_enhancer_strength"
    }

    private lateinit var sharedPreferences : SharedPreferences

    // AudioFx Objects
    var equalizer : Equalizer? = null
    private var bassboost : BassBoost? = null
    private var virtualizer : Virtualizer? = null
    private var presetreverb : PresetReverb? = null
    private var loudnessenhancer : LoudnessEnhancer? = null

    // Support properties
    private var equalizerSupported : Boolean = false
    private var bassboostSupported : Boolean = false
    private var virtualizerSupported : Boolean = false
    private var presetReverbSupported : Boolean = false
    private var loudnessEnhancerSupported : Boolean = false

    // Enable properties
    var equalizerEnabled : Boolean
        get() {
            return if (equalizer != null)
                equalizer!!.enabled
            else false
        }
        set(value) {
            if (value != equalizer?.enabled)
                if (equalizer?.setEnabled(value) == AudioEffect.SUCCESS) {
                    Log.d(TAG, "equalizerEnabled: $value")
                    sharedPreferences.edit().putBoolean(EQUALIZER_ENABLED_PREF, value).apply()
                }
        }

    var bassboostEnabled : Boolean
        get() {
            return if (bassboost != null)
                bassboost!!.enabled
            else false
        }
        set(value) {
            Log.d(TAG, "bassboostEnabled: $value")
            if (value != bassboost?.enabled) {
                if (!value) {
                    bassboost?.setStrength(1.toShort())
                    bassboost?.setStrength(0.toShort())
                } else {
                    bassboost?.setStrength(sharedPreferences.getInt(BASSBOOST_STRENGTH_PREF, 0).toShort())
                }
                bassboost?.enabled = value
                sharedPreferences.edit().putBoolean(BASSBOOST_ENABLED_PREF, value).apply()
            }
        }

    var virtualizerEnabled : Boolean
        get() {
            return if (virtualizer != null)
                virtualizer!!.enabled
            else false
        }
        set(value) {
            Log.d(TAG, "virtualizerEnabled: $value")
            if (value != virtualizer?.enabled) {
                if (!value) {
                    virtualizer?.setStrength(1.toShort())
                    virtualizer?.setStrength(0.toShort())
                } else {
                    virtualizer?.setStrength(sharedPreferences.getInt(VIRTUALIZER_STRENGTH_PREF, 0).toShort())
                }
                // virtualizer?.enabled = value
                sharedPreferences.edit().putBoolean(VIRTUALIZER_ENABLED_PREF, value).apply()
            }
        }

    var presetReverbEnabled : Boolean
        get() {
            return if (presetreverb != null)
                presetreverb!!.enabled
            else false
        }
        set(value) {
            if (value != presetreverb?.enabled)
                if (presetreverb?.setEnabled(value) == AudioEffect.SUCCESS) {
                    Log.d(TAG, "presetReverbEnabled: $value")
                    sharedPreferences.edit().putBoolean(PRESETREVERB_ENABLED_PREF, value).apply()
                }
        }

    var loudnessEnhancerEnabled : Boolean
        get() {
            return if (loudnessenhancer != null)
                loudnessenhancer!!.enabled
            else false
        }
        set(value) {
            if (loudnessenhancer?.setEnabled(value) == 0) {
                Log.d(TAG, "loudnessEnhancerEnabled: $value")
                sharedPreferences.edit().putBoolean(LOUDNESSENHANCER_ENABLED_PREF, value).apply()
            }
        }

    // User Preset
    var preset : Int
        get() = sharedPreferences.getInt(AudioFXHelper.EQUALIZER_PRESET_PREF, 0)
        set(value) {
            Log.d(TAG, "preset: $value")
            setBandsByPreset(value)
            sharedPreferences.edit().putInt(EQUALIZER_PRESET_PREF, value).apply()
        }

    lateinit var userPresets: ArrayList<EQPreset>

    // Strength properties
    var bassboostStrength : Short
        get() {
            return if (bassboost != null)
                bassboost!!.roundedStrength
            else 0
        }
        set(value) {
            bassboost?.setStrength(value)
            Log.d(TAG, "bassboostStrength: $value")
            sharedPreferences.edit().putInt(BASSBOOST_STRENGTH_PREF, value.toInt()).apply()
        }

    var virtualizerStrength : Short
        get() {
            return if (virtualizer != null)
                virtualizer!!.roundedStrength
            else 0
        }
        set(value) {
            virtualizer?.setStrength(value)
            Log.d(TAG, "virtualizerStrength: $value roundec: ${virtualizer?.roundedStrength}")
            sharedPreferences.edit().putInt(VIRTUALIZER_STRENGTH_PREF, value.toInt()).apply()
        }

    var presetReverbPreset : Short
        get() {
            return if (presetreverb != null)
                presetreverb!!.preset
            else 0
        }
        set(value) {
            presetreverb?.preset = value
            Log.d(TAG, "presetReverbPreset: $value")
            sharedPreferences.edit().putInt(PRESETREVERB_PRESET_PREF, value.toInt()).apply()
        }

    var loudnessEnhancerStrength : Float
        get() {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && loudnessenhancer != null)
                loudnessenhancer!!.targetGain
            else 0.0f
        }
        set(value) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                loudnessenhancer?.setTargetGain(value.toInt())
                Log.d(TAG, "loudnessEnhancerStrength: $value")
                sharedPreferences.edit().putInt(LOUDNESSENHANCER_STRENGTH_PREF, value.toInt()).apply()
            }
        }

    constructor(context: Context, audioSessionId : Int, dataRepository: DataRepository) : this(dataRepository){
        Log.d(TAG, "Initializing")
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

        userPresets = dataRepository.getEQPresets() as ArrayList<EQPreset>

        // Initialize equalizer
        try {
            equalizer = Equalizer(1, audioSessionId)
            Log.d(TAG, "equalizerSupported: true")
            equalizerSupported = true
            equalizer?.apply {
                enabled = sharedPreferences.getBoolean(EQUALIZER_ENABLED_PREF, false)
                setBandsByPreset(preset)
            }
        } catch (e : UnsupportedOperationException) {}

        // Initialize bassboost
        try {
            bassboost = BassBoost(1, audioSessionId)
            Log.d(TAG, "bassboostSupported: true")
            bassboostSupported = true
            bassboost?.enabled = sharedPreferences.getBoolean(BASSBOOST_ENABLED_PREF, false)
            bassboost?.setStrength(sharedPreferences.getInt(BASSBOOST_STRENGTH_PREF, 0).toShort())
        } catch (e : UnsupportedOperationException) {}

        // Initialize virtualizer
        try {
            virtualizer = Virtualizer(1, audioSessionId)
            Log.d(TAG, "virtualizerSupported: true")
            virtualizerSupported = true
            virtualizer?.enabled = sharedPreferences.getBoolean(VIRTUALIZER_ENABLED_PREF, false)
            virtualizer?.setStrength(sharedPreferences.getInt(VIRTUALIZER_STRENGTH_PREF, 0).toShort())
        } catch (e : UnsupportedOperationException) {}

        // Initialize presetreverb
        try {
            presetreverb = PresetReverb(1, audioSessionId)
            Log.d(TAG, "presetReverbSupported: true")
            presetReverbSupported = true
            presetreverb?.enabled = sharedPreferences.getBoolean(PRESETREVERB_ENABLED_PREF, true)
            presetreverb?.preset = sharedPreferences.getInt(PRESETREVERB_PRESET_PREF, PresetReverb.PRESET_NONE.toInt()).toShort()
        } catch (e : UnsupportedOperationException) {}

        // Initialize loudnessenhancer
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            try {
                loudnessenhancer = LoudnessEnhancer(audioSessionId)
                Log.d(TAG, "loudnessEnhancerSupported: true")
                loudnessEnhancerSupported = true
                loudnessenhancer?.enabled = sharedPreferences.getBoolean(LOUDNESSENHANCER_ENABLED_PREF, false)
                loudnessenhancer?.setTargetGain(sharedPreferences.getInt(LOUDNESSENHANCER_STRENGTH_PREF, 0))
            } catch (ignored: UnsupportedOperationException) {
            }
        }
    }

    fun saveUserPreset(name: String) {
        equalizer?.apply {
            val id = dataRepository.addEQPreset(EQPreset(0, name))
            val editor = sharedPreferences.edit()
            for (i in 0 until numberOfBands) {
                editor.putInt(EQUALIZER_USER_PRESET_PREF + id + EQUALIZER_FQ_PREFIX+ i, getBandLevel(i.toShort()).toInt()).apply()
            }
            userPresets.add(EQPreset(id.toInt(), name))
        }
    }

    fun deleteUserPreset(userPreset: Int) {
        equalizer?.apply {
            val eqPreset = userPresets[userPreset]
            dataRepository.deleteEQPreset(eqPreset)
            val editor = sharedPreferences.edit()
            for (i in 0 until numberOfBands) {
                editor.remove(EQUALIZER_USER_PRESET_PREF + eqPreset.id + EQUALIZER_FQ_PREFIX+ i).apply()
            }
            userPresets.remove(eqPreset)
        }
    }

    fun getUserPresetCount(): Int = userPresets.size

    fun getUserPresetName(userPreset: Int): String = userPresets[userPreset].name

    private fun setBandsByPreset(preset: Int) {
        equalizer?.apply {
            val noPresets = numberOfPresets
            when {
                preset == -1 -> for (i in 0 until numberOfBands) {
                    setBandLevel(i.toShort(), this@AudioFXHelper.getEqualizerBandStrength(i.toShort()))
                }
                preset < noPresets -> usePreset(preset.toShort())
                else -> for (i in 0 until numberOfBands) {
                    setBandLevel(i.toShort(), this@AudioFXHelper.getUserPresetBandStrength(preset - noPresets ,i.toShort()))
                }
            }
        }
    }

    private fun getUserPresetBandStrength(id: Int, band: Short): Short {
        val eqPreset = userPresets[id]
        val value = sharedPreferences.getInt(EQUALIZER_USER_PRESET_PREF+ eqPreset.id + EQUALIZER_FQ_PREFIX + band,0).toShort()
        Log.d(TAG, "getUserPresetBandStrength: band: $band, value: $value")
        return value
    }

    private fun getEqualizerBandStrength(band: Short): Short {
        val value = sharedPreferences.getInt(EQUALIZER_FQ_PREFIX + band,0).toShort()
        Log.d(TAG, "getEqualizerBandStrength: band: $band, value: $value")
        return value
    }

    fun setEqualizerBandStrength(band: Short, amount: Short) {
        equalizer?.setBandLevel(band, amount)
        sharedPreferences.edit().putInt(EQUALIZER_FQ_PREFIX + band, amount.toInt()).apply()
        Log.d(TAG, "setEqualizerBandStrength: band: $band, value: $amount")
    }

    fun release(){
        Log.d(TAG, "release")

        equalizer?.release()
        equalizer = null

        bassboost?.release()
        bassboost = null

        presetreverb?.release()
        presetreverb = null

        virtualizer?.release()
        virtualizer = null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            loudnessenhancer?.release()
            loudnessenhancer = null
        }
    }

}