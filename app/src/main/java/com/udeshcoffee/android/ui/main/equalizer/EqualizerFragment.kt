package com.udeshcoffee.android.ui.main.equalizer

import android.media.audiofx.Equalizer
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.ActionBar
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.util.TypedValue
import android.view.*
import android.widget.*
import com.h6ah4i.android.widget.verticalseekbar.VerticalSeekBar
import com.h6ah4i.android.widget.verticalseekbar.VerticalSeekBarWrapper
import com.udeshcoffee.android.R
import com.udeshcoffee.android.extensions.openDrawer
import com.udeshcoffee.android.extensions.setRoundColor
import com.udeshcoffee.android.ui.dialogs.DeleteEQDialog
import com.udeshcoffee.android.ui.dialogs.SaveEQDialog
import org.koin.android.ext.android.inject


/**
 * Created by Udathari on 9/28/2017.
 */
class EqualizerFragment : Fragment(), EqualizerContract.View {

    val TAG = this.javaClass.simpleName

    override val presenter: EqualizerContract.Presenter by inject()

    var actionBar: ActionBar? = null
    private lateinit var presetSpinner: Spinner
    private lateinit var actionSave: ImageButton
    private lateinit var bandBars: Array<VerticalSeekBar?>
    private lateinit var dbTextViews: Array<TextView?>
    private lateinit var bandHolder: LinearLayout

    // Virtualizer
    private lateinit var virtualizerSwitch: Switch
    private lateinit var virtualizerSeekbar: SeekBar

    // BassBoost
    private lateinit var bassBoostSwitch: Switch
    private lateinit var bassBoostSeekbar: SeekBar

    // Reverb
    private lateinit var reverbSpinner: Spinner

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.frag_equalizer, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setHasOptionsMenu(true)

        view.apply {
            val toolbar = findViewById<Toolbar>(R.id.toolbar)
            (activity as AppCompatActivity).apply {
                setSupportActionBar(toolbar)
                this@EqualizerFragment.actionBar = supportActionBar
            }

            actionBar?.apply {
                setDisplayHomeAsUpEnabled(true)
                title = ""
            }
            val titleView = findViewById<TextView>(R.id.title_view)
            titleView.setRoundColor(R.color.eqAccent)

            presetSpinner = findViewById(R.id.eq_preset_spinner)

            actionSave = findViewById(R.id.action_save)
            actionSave.setOnClickListener{
                presenter.actionSaveOrDelete()
            }

            bandHolder = findViewById(R.id.equalizerLinearLayout)

            virtualizerSwitch = findViewById<Switch>(R.id.virtualizer_switch)
            virtualizerSwitch.setOnCheckedChangeListener { _, b ->
                presenter.actionVirtualizerEnable(b)
            }
            virtualizerSeekbar = findViewById<SeekBar>(R.id.virtualizer_seekbar).also{
                it.max = 1000
                it.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
                    override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                        if (p2) {
                            presenter.changeVirtualizer(p1)
                        }
                    }
                    override fun onStartTrackingTouch(p0: SeekBar?) {}
                    override fun onStopTrackingTouch(p0: SeekBar?) {}
                })
            }

            bassBoostSwitch = findViewById(R.id.bass_boost_switch)
            bassBoostSwitch.setOnCheckedChangeListener { _, b ->
                presenter.actionBassBoostEnable(b)
            }
            bassBoostSeekbar = findViewById<SeekBar>(R.id.bass_boost_seekbar).also{
                it.max = 1000
                it.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
                    override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                        if (p2) {
                            presenter.changeBassBoost(p1)
                        }
                    }
                    override fun onStartTrackingTouch(p0: SeekBar?) {}
                    override fun onStopTrackingTouch(p0: SeekBar?) {}
                })
            }

            reverbSpinner = findViewById(R.id.reverb_spinner)
        }

        presenter.view = this
        presenter.start()
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        inflater?.inflate(R.menu.equalizer_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onPrepareOptionsMenu(menu: Menu?) {
        val item = menu?.findItem(R.id.switch_equalizer)
        item?.let {
            val actionEnable = item.actionView.findViewById<Switch>(R.id.action_enable_equalizer)
            presenter.enabled?.let { actionEnable.isChecked = it }
            actionEnable.setOnCheckedChangeListener { _, isChecked ->
                presenter.actionEnable(isChecked)
            }
        }
        super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            android.R.id.home -> activity?.openDrawer()
            R.id.switch_equalizer -> {
                Toast.makeText(context, "Switch: ${item.isChecked}", Toast.LENGTH_SHORT).show()
                presenter.actionEnable(item.isChecked)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    fun onSaveEQDialogResult(name: String){
        presenter.saveUserPreset(name)
    }

    fun onDeleteEQDialogResult(id: Int){
        presenter.deletePreset(id)
    }

    override fun setEnableAction() {
        activity?.invalidateOptionsMenu()
    }

    override fun setEnabled(enabled: Boolean) {
        for (bar in bandBars) {
            bar?.isEnabled = enabled
        }
        presetSpinner.isEnabled = enabled
    }

    override fun setSaveAction(type: Int) {
        when (type) {
            EqualizerPresenter.SaveType.CAN_SAVE -> {
                actionSave.setImageResource(R.drawable.ic_save_white_24dp)
                actionSave.alpha = 1f
                actionSave.isEnabled = true
            }
            EqualizerPresenter.SaveType.CANT_SAVE -> {
                actionSave.setImageResource(R.drawable.ic_save_white_24dp)
                actionSave.alpha = 0.3f
                actionSave.isEnabled = false
            }
            EqualizerPresenter.SaveType.DELETE -> {
                actionSave.setImageResource(R.drawable.ic_delete_forever_white_24dp)
                actionSave.alpha = 1f
                actionSave.isEnabled = true
            }
        }
    }

    override fun setVirtualizerEnabled(enabled: Boolean, byAction: Boolean) {
        if (!byAction) virtualizerSwitch.isChecked = enabled
        virtualizerSeekbar.isEnabled = enabled
    }

    override fun setVirtualizer(amount: Int) {
        virtualizerSeekbar.progress = amount
    }

    override fun setBassBoostEnabled(enabled: Boolean, byAction: Boolean) {
        if (!byAction) bassBoostSwitch.isChecked = enabled
        bassBoostSeekbar.isEnabled = enabled
    }

    override fun setBassBoost(amount: Int) {
        bassBoostSeekbar.progress = amount
    }

    override fun setReverbs(reverbs: ArrayList<String>, initReverb: Int) {
        val dataAdapterReverb = ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, reverbs)
        dataAdapterReverb.setDropDownViewResource(R.layout.spinner_dropdown_item)
        reverbSpinner.adapter = dataAdapterReverb
        reverbSpinner.setSelection(initReverb)
        reverbSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onNothingSelected(p0: AdapterView<*>?) {}

            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, index: Int, p3: Long) {
                presenter.pickReverb(index)
            }

        }
    }

    override fun setPresets(presets: ArrayList<String>, initPreset: Int) {
        val dataAdapterEq = ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, presets)
        dataAdapterEq.setDropDownViewResource(R.layout.spinner_dropdown_item)
        presetSpinner.adapter = dataAdapterEq
        presetSpinner.setSelection(initPreset)
        presetSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onNothingSelected(p0: AdapterView<*>?) {}

            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, index: Int, p3: Long) {
                presenter.pickPreset(index)
            }

        }
    }

    override fun setPreset(preset: Int) {
        presetSpinner.setSelection(preset)
    }

    override fun createBands(equalizer: Equalizer) {
        // Create the Equalizer object (an AudioEffect subclass) and attach it to our media player,
        // with a default priority (0).
        val bands = equalizer.numberOfBands
        bandBars = arrayOfNulls(bands.toInt())
        dbTextViews = arrayOfNulls(bands.toInt())
        bandHolder.weightSum = bands.toFloat()
        for (i in 0 until bands) {
            val row = LinearLayout(context)
            val rowParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT)
            rowParams.weight = 1f
            row.layoutParams = rowParams
            row.orientation = LinearLayout.VERTICAL
            row.gravity = Gravity.CENTER_HORIZONTAL
            val freqTextView = TextView(context)
            freqTextView.layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT)
            freqTextView.textSize = 13.0f
            freqTextView.text = "${equalizer.getCenterFreq(i.toShort()) / 1000} Hz"
            dbTextViews[i] = TextView(context)
            dbTextViews[i]?.apply {
                layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT)
                textSize = 11.0f
            }
            val layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 150f, resources.displayMetrics).toInt())
            layoutParams.weight = 1f
            val wrapper = VerticalSeekBarWrapper(context)
            wrapper.layoutParams = layoutParams
            val view = LayoutInflater.from(context).inflate(R.layout.equalizer_seekbar, null)
            bandBars[i] = view.findViewById(R.id.equalizerBandBar)
            bandBars[i]?.apply {
                max = equalizer.bandLevelRange[1] - equalizer.bandLevelRange[0]
                rotationAngle = 270
                rotation = 270f
                setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(seekBar: SeekBar, progress: Int,
                                                   fromUser: Boolean) {
                        if (fromUser)
                            presenter.changeFrequency(i, progress)
                        dbTextViews[i]?.text = "${(progress + equalizer.bandLevelRange[0])/100}dB"
                    }

                    override fun onStartTrackingTouch(seekBar: SeekBar) {
                    }

                    override fun onStopTrackingTouch(seekBar: SeekBar) {
                        presenter.setFrequency(i, seekBar.progress)
                    }
                })
            }
            wrapper.addView(view)
            row.addView(freqTextView)
            row.addView(wrapper)
            row.addView(dbTextViews[i])
            bandHolder.addView(row)
        }
    }

    override fun setBands(equalizer: Equalizer) {
        for (i in 0 until bandBars.size) {
            bandBars[i]?.progress = equalizer.getBandLevel(i.toShort()) - equalizer.bandLevelRange[0]
        }
    }

    override fun showSaveEQDialog() {
        val mDialog = SaveEQDialog()
        mDialog.setTargetFragment(this, 0)
        mDialog.show(fragmentManager, "SaveEQDialog")
    }

    override fun showDeleteEQDialog(preset: Int, name: String) {
        val mDialog = DeleteEQDialog()
        mDialog.setTargetFragment(this, 0)
        val bundle = Bundle()
        bundle.putInt(DeleteEQDialog.ARGUMENT_PRESET, preset)
        bundle.putString(DeleteEQDialog.ARGUMENT_NAME, name)
        mDialog.arguments = bundle
        mDialog.show(fragmentManager, "SaveEQDialog")
    }

    companion object {
        fun create(): EqualizerFragment = EqualizerFragment()
    }
}