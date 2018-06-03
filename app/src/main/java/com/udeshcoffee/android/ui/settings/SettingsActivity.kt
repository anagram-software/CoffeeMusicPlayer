package com.udeshcoffee.android.ui.settings

import android.annotation.TargetApi
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.preference.CheckBoxPreference
import android.support.v7.preference.ListPreference
import android.support.v7.preference.Preference
import android.support.v7.preference.PreferenceFragmentCompat
import android.support.v7.widget.Toolbar
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.TextView
import com.udeshcoffee.android.R
import com.udeshcoffee.android.service.CollectionService
import com.udeshcoffee.android.extensions.setRoundColor
import com.udeshcoffee.android.ui.common.dialogs.WhatsNewDialog
import com.udeshcoffee.android.utils.PreferenceUtil

/**
 * Created by Udathari on 10/24/2017.
 */
class SettingsActivity: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.SettingsTheme)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        toolbar.title = ""

        val titleView = findViewById<TextView>(R.id.title_view)
        titleView.setRoundColor(R.color.settingsAccent)

        setSupportActionBar(toolbar)

        val ab = supportActionBar
        ab?.setDisplayHomeAsUpEnabled(true)

        val preferenceFragment = PreferenceFragment()

        val fm = supportFragmentManager
        fm.beginTransaction()
                .replace(R.id.settings_content, preferenceFragment)
                .commit()
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    class PreferenceFragment : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener {

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            addPreferencesFromResource(R.xml.pref_main)
        }

        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

            val prefLibStart = findPreference(PreferenceUtil.PREF_LIB_START) as ListPreference
            prefLibStart.summary = prefLibStart.entry

            val prefCollectContent = findPreference(PreferenceUtil.PREF_COLLECT_CONTENT) as CheckBoxPreference
            findPreference(PreferenceUtil.PREF_WIFI_ONLY).isEnabled = prefCollectContent.isChecked

            val prefCollectLyrics = findPreference(PreferenceUtil.PREF_COLLECT_LYRICS)
            prefCollectLyrics.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                val intent = Intent(context, CollectionService::class.java)
                intent.action = CollectionService.ACTION_COLLECT_LYRICS
                context?.let { it1 -> ContextCompat.startForegroundService(it1,intent) }
                true
            }

            val prefOpen = findPreference(PreferenceUtil.PREF_OPEN)
            prefOpen.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                val view = LayoutInflater.from(context).inflate(R.layout.dialog_licence, null) as WebView
                view.loadUrl("file:///android_asset/licences.html")
                AlertDialog.Builder(context!!, R.style.Theme_AppCompat_Light_Dialog_Alert)
                        .setTitle("Open Source Licences")
                        .setView(view)
                        .setPositiveButton(android.R.string.ok, null)
                        .show()
                true
            }

            val prefWhatsNew = findPreference(PreferenceUtil.PREF_WHATS_NEW)
            prefWhatsNew.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                WhatsNewDialog.create().show(fragmentManager, "WhatsNewDialog")
                true
            }

            val prefVersion = findPreference(PreferenceUtil.PREF_VERSION)
            prefVersion.summary = activity!!.packageManager.getPackageInfo( activity!!.packageName, 0).versionName

            return super.onCreateView(inflater, container, savedInstanceState)
        }

        override fun onResume() {
            super.onResume()
            preferenceManager.sharedPreferences.registerOnSharedPreferenceChangeListener(this)

        }

        override fun onPause() {
            preferenceManager.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
            super.onPause()
        }

        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
            when (key) {
                PreferenceUtil.PREF_NEW_PLAYER -> {
                    val pref = findPreference(key) as CheckBoxPreference
                    pref.summary = resources.getString(R.string.pref_new_player) + " Restart to affect the changes."
                }
                PreferenceUtil.PREF_LIB_START -> {
                    val pref = findPreference(key) as ListPreference
                    pref.summary = pref.entry
                }
                PreferenceUtil.PREF_COLLECT_CONTENT -> {
                    val enabled = sharedPreferences.getBoolean(key, true)
                    findPreference(PreferenceUtil.PREF_WIFI_ONLY).isEnabled = enabled
                }
            }

        }
    }
}
