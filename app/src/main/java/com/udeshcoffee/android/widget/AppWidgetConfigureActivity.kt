package com.udeshcoffee.android.widget

import android.app.Activity
import android.app.WallpaperManager
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v7.widget.Toolbar
import android.widget.*
import com.udeshcoffee.android.R
import com.udeshcoffee.android.getColorWithAlpha
import com.udeshcoffee.android.getService
import com.udeshcoffee.android.service.MusicService
import com.udeshcoffee.android.ui.BaseActivity

/**
 * Created by Udathari on 10/26/2017.
 */
class AppWidgetConfigureActivity : BaseActivity() {

    private var appWidgetId: Int = -1
    var color: Int = 0
    var alpha: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_widget_config)


        val intent = intent
        val extras = intent.extras
        if (extras != null) {
            appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        }
        setResult(Activity.RESULT_CANCELED)
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
        }

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        val actionBar = supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(true)

        val background = findViewById<ImageView>(R.id.background)
        val wallpaperManager = WallpaperManager.getInstance(this)
        val wallpaperDrawable = wallpaperManager.drawable
        background.post { background.setImageDrawable(wallpaperDrawable) }

        val themeCheckBox = findViewById<CheckBox>(R.id.theme_check)
        val buttonCheckBox = findViewById<CheckBox>(R.id.button_check)
        val alphaSeekBar = findViewById<SeekBar>(R.id.aplha_seekbar)
        alphaSeekBar.max = 100
        alphaSeekBar.progress = 100
        color = Color.BLACK

        val relativeLayout = findViewById<RelativeLayout>(R.id.relativeLayout)
        val titleView = findViewById<TextView>(R.id.text1)
        val artistView = findViewById<TextView>(R.id.text2)
        val prevBtn = findViewById<ImageButton>(R.id.prev_button)
        val playBtn = findViewById<ImageButton>(R.id.play_button)
        val nextBtn = findViewById<ImageButton>(R.id.next_button)

        themeCheckBox.setOnCheckedChangeListener { _, checked ->
            color = if (checked) {
                Color.BLACK
            } else {
                Color.WHITE
            }
            relativeLayout.setBackgroundColor(color.getColorWithAlpha(alphaSeekBar.progress.toFloat() / 100))
        }

        buttonCheckBox.setOnCheckedChangeListener { _, checked ->
            if (checked) {
                titleView.setTextColor(Color.BLACK)
                artistView.setTextColor(Color.BLACK)
                prevBtn.setColorFilter(Color.BLACK)
                playBtn.setColorFilter(Color.BLACK)
                nextBtn.setColorFilter(Color.BLACK)
            } else {
                titleView.setTextColor(Color.WHITE)
                artistView.setTextColor(Color.WHITE)
                prevBtn.setColorFilter(Color.WHITE)
                playBtn.setColorFilter(Color.WHITE)
                nextBtn.setColorFilter(Color.WHITE)
            }
        }

        alphaSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
                relativeLayout.setBackgroundColor(color.getColorWithAlpha(i.toFloat() / 100))
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {

            }
        })

        val done = findViewById<Button>(R.id.done_btn)
        done.setOnClickListener { _ ->
            val editor = PreferenceManager.getDefaultSharedPreferences(this).edit()
            editor.putBoolean(MediumWidgetProvider.MEDIUM_WIDGET_THEME + appWidgetId, themeCheckBox.isChecked)
            editor.putBoolean(MediumWidgetProvider.MEDIUM_WIDGET_BUTTONS + appWidgetId, buttonCheckBox.isChecked)
            editor.putInt(MediumWidgetProvider.MEDIUM_WIDGET_ALPHA + appWidgetId, alphaSeekBar.progress)
            editor.apply()
            getService()?.let { MediumWidgetProvider.instance?.notifyChange(it, MusicService.InternalIntents.METADATA_CHANGED) }
            val resultValue = Intent()
            resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            setResult(Activity.RESULT_OK, resultValue)
            finish()
        }
    }
}