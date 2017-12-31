package com.udeshcoffee.android.service

import android.content.Context
import android.content.SharedPreferences
import android.media.audiofx.*
import android.os.Build
import android.preference.PreferenceManager
import android.util.Log
import com.udeshcoffee.android.data.DataRepository
import com.udeshcoffee.android.data.model.EQPreset

/**
 * Created by Udathari on 7/27/2017.
 */
class AudioFXHelper private constructor(private val dataRepository: DataRepository) {

    companion object {
        val TAG = "AudioFxHelper"

        val EQUALIZER_PRESET_PREF = "eqpreset"
        val EQUALIZER_USER_PRESET_PREF = "equser"
        val EQUALIZER_FQ_PREFIX = "eq"

        val EQUALIZER_ENABLED_PREF = "eq_enabled"
        val BASSBOOST_ENABLED_PREF = "bassboost_enabled"
        val VIRTUALIZER_ENABLED_PREF = "virtualizer_enabled"
        val PRESETREVERB_ENABLED_PREF = "preset_reverb_enabled"
        val LOUDNESSENHANCER_ENABLED_PREF = "loudness_enhancer_enabled"

        val BASSBOOST_STRENGTH_PREF = "bassboost_strength"
        val VIRTUALIZER_STRENGTH_PREF = "virtualizer_strength"
        val PRESETREVERB_PRESET_PREF = "preset_reverb_preset"
        val LOUDNESSENHANCER_STRENGTH_PREF = "loudness_enhancer_strength"
    }

    lateinit var sharedPreferences : SharedPreferences

    // AudioFx Objects
    var equalizer : Equalizer? = null
    private var bassboost : BassBoost? = null
    private var virtualizer : Virtualizer? = null
    private var presetreverb : PresetReverb? = null
    private var loudnessenhancer : LoudnessEnhancer? = null

    // Support properties
    var equalizerSupported : Boolean = false
        private set
    var bassboostSupported : Boolean = false
        private set
    var virtualizerSupported : Boolean = false
        private set
    var presetReverbSupported : Boolean = false
        private set
    var loudnessEnhancerSupported : Boolean = false
        private set

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
            if (bassboost != null)
                return bassboost!!.enabled
            else return false
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
            if (virtualizer != null)
                return virtualizer!!.enabled
            else return false
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
            if (presetreverb != null)
                return presetreverb!!.enabled
            else return false
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
            if (loudnessenhancer != null)
                return loudnessenhancer!!.enabled
            else return false
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
            if (bassboost != null)
                return bassboost!!.roundedStrength
            else return 0
        }
        set(value) {
            bassboost?.setStrength(value)
            Log.d(TAG, "bassboostStrength: $value")
            sharedPreferences.edit().putInt(BASSBOOST_STRENGTH_PREF, value.toInt()).apply()
        }

    var virtualizerStrength : Short
        get() {
            if (virtualizer != null)
                return virtualizer!!.roundedStrength
            else return 0
        }
        set(value) {
            virtualizer?.setStrength(value)
            Log.d(TAG, "virtualizerStrength: $value roundec: ${virtualizer?.roundedStrength}")
            sharedPreferences.edit().putInt(VIRTUALIZER_STRENGTH_PREF, value.toInt()).apply()
        }

    var presetReverbPreset : Short
        get() {
            if (presetreverb != null)
                return presetreverb!!.preset
            else return 0
        }
        set(value) {
            presetreverb?.preset = value
            Log.d(TAG, "presetReverbPreset: $value")
            sharedPreferences.edit().putInt(PRESETREVERB_PRESET_PREF, value.toInt()).apply()
        }

    var loudnessEnhancerStrength : Float
        get() {
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && loudnessenhancer != null)
                return loudnessenhancer!!.targetGain
            else return 0.0f
        }
        set(value) {
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
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
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
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
            if (preset == -1) {
                for (i in 0 until numberOfBands) {
                    setBandLevel(i.toShort(), this@AudioFXHelper.getEqualizerBandStrength(i.toShort()))
                }
            } else if (preset < noPresets) {
                usePreset(preset.toShort())
            } else {
                for (i in 0 until numberOfBands) {
                    setBandLevel(i.toShort(), this@AudioFXHelper.getUserPresetBandStrength(preset - noPresets ,i.toShort()))
                }
            }
        }
    }

    fun getUserPresetBandStrength(id: Int, band: Short): Short {
        val eqPreset = userPresets[id]
        val value = sharedPreferences.getInt(EQUALIZER_USER_PRESET_PREF+ eqPreset.id + EQUALIZER_FQ_PREFIX + band,0).toShort()
        Log.d(TAG, "getUserPresetBandStrength: band: $band, value: $value")
        return value
    }

    fun getEqualizerBandStrength(band: Short): Short {
        val value = sharedPreferences.getInt(AudioFXHelper.EQUALIZER_FQ_PREFIX + band,0).toShort()
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

        loudnessenhancer?.release()
        loudnessenhancer = null
    }

}