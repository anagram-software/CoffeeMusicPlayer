package com.udeshcoffee.android.widget

import android.app.Activity
import android.app.WallpaperManager
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.constraint.ConstraintLayout
import android.support.constraint.ConstraintSet
import android.support.v7.widget.Toolbar
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.ViewStub
import android.widget.*
import com.udeshcoffee.android.R
import com.udeshcoffee.android.extensions.getColorWithAlpha
import com.udeshcoffee.android.extensions.getService
import com.udeshcoffee.android.extensions.loadArtwork
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
        var appWidgetProvider: AppWidgetProviderInfo? = null
        if (extras != null) {
            appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            appWidgetProvider = AppWidgetManager.getInstance(this).getAppWidgetInfo(appWidgetId)
        }
        setResult(Activity.RESULT_CANCELED)
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID || appWidgetProvider == null) {
            finish()
        }

        Log.d(TAG, "Package Name: ${appWidgetProvider?.provider?.className}")

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

        val constraintLayout = findViewById<ConstraintLayout>(R.id.constraint_layout)
        val stub = findViewById<ViewStub>(R.id.layout_stub)
        stub.layoutResource = if (appWidgetProvider?.provider?.className == MediumAppWidget::class.java.name) {
            stub.layoutParams = ConstraintLayout.LayoutParams(
                    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 0f, resources.displayMetrics).toInt(),
                    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 96f, resources.displayMetrics).toInt()
            )
            R.layout.widget_medium
        } else {
            stub.layoutParams = ConstraintLayout.LayoutParams(
                    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 0f, resources.displayMetrics).toInt(),
                    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 172f, resources.displayMetrics).toInt()
            )
            R.layout.widget_big
        }
        stub.inflate()

        val constraintSet = ConstraintSet()
        constraintSet.clone(constraintLayout)
        constraintSet.connect(R.id.widget_layout, ConstraintSet.TOP, R.id.toolbar, ConstraintSet.BOTTOM, 0)
        constraintSet.connect(R.id.widget_layout, ConstraintSet.BOTTOM, R.id.background, ConstraintSet.BOTTOM, 0)
        constraintSet.applyTo(constraintLayout)

        val relativeLayout = findViewById<View>(R.id.widget_layout_holder)
        val artView = findViewById<ImageView>(R.id.album_art)
        val line1 = findViewById<TextView>(R.id.text1)
        val line2 = findViewById<TextView>(R.id.text2)
        val line3 = findViewById<TextView>(R.id.text3)
        val prevBtn = findViewById<ImageButton>(R.id.prev_button)
        val playBtn = findViewById<ImageButton>(R.id.play_button)
        val nextBtn = findViewById<ImageButton>(R.id.next_button)

        getService()?.let {
            it.currentSong()?.apply {
                line1.text = title
                if (line3 == null)
                    line2.text =  String.format("%s • %s", artistName, albumName)
                else {
                    line2.text = albumName
                    line3.text = artistName
                }
                this.loadArtwork(this@AppWidgetConfigureActivity, artView)
            }
        }

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
                line1.setTextColor(Color.BLACK)
                line2.setTextColor(Color.BLACK)
                line3?.setTextColor(Color.BLACK)
                prevBtn.setColorFilter(Color.BLACK)
                playBtn.setColorFilter(Color.BLACK)
                nextBtn.setColorFilter(Color.BLACK)
            } else {
                line1.setTextColor(Color.WHITE)
                line2.setTextColor(Color.WHITE)
                line3?.setTextColor(Color.WHITE)
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
            var themeKey = MediumAppWidget.WIDGET_THEME
            var buttonKey = MediumAppWidget.WIDGET_BUTTONS
            var alphaKey = MediumAppWidget.WIDGET_ALPHA
            if (appWidgetProvider?.provider?.className == BigAppWidget::class.java.name) {
                themeKey = BigAppWidget.WIDGET_THEME
                buttonKey = BigAppWidget.WIDGET_BUTTONS
                alphaKey = BigAppWidget.WIDGET_ALPHA
            }
            val editor = PreferenceManager.getDefaultSharedPreferences(this).edit()
            editor.putBoolean(themeKey + appWidgetId, themeCheckBox.isChecked)
            editor.putBoolean(buttonKey + appWidgetId, buttonCheckBox.isChecked)
            editor.putInt(alphaKey + appWidgetId, alphaSeekBar.progress)
            editor.apply()
            getService()?.let {
                if (appWidgetProvider?.provider?.className == MediumAppWidget::class.java.name) {
                    MediumAppWidget.getInstance().notifyChange(it.applicationContext,
                            MusicService.InternalIntents.PLAYBACK_STATE_CHANGED, it.currentSong(), it.isPlaying())
                    MediumAppWidget.getInstance().notifyChange(it.applicationContext,
                            MusicService.InternalIntents.METADATA_CHANGED, it.currentSong(), it.isPlaying())
                } else {
                    BigAppWidget.getInstance().notifyChange(it.applicationContext,
                            MusicService.InternalIntents.PLAYBACK_STATE_CHANGED, it.currentSong(), it.isPlaying())
                    BigAppWidget.getInstance().notifyChange(it.applicationContext,
                            MusicService.InternalIntents.METADATA_CHANGED, it.currentSong(), it.isPlaying())
                }

            }
            val resultValue = Intent()
            resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            setResult(Activity.RESULT_OK, resultValue)
            finish()
        }
    }
}