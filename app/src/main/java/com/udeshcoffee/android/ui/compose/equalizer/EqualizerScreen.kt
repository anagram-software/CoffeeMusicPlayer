package com.udeshcoffee.android.ui.compose.equalizer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.udeshcoffee.android.playback.AudioEffects
import com.udeshcoffee.android.service.AudioFXHelper
import com.udeshcoffee.android.ui.compose.components.ConfirmDialog
import com.udeshcoffee.android.ui.compose.components.EmptyState
import com.udeshcoffee.android.ui.compose.components.TextInputDialog
import com.udeshcoffee.android.ui.compose.components.TopLevelScaffold
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

data class EqState(
        val enabled: Boolean,
        val presets: List<String>,
        /** Index into [presets]; the last entry is "Custom". */
        val selected: Int,
        val builtInCount: Int,
        val bandFrequencies: List<Int>,
        val bandLevels: List<Int>,
        val minLevel: Int,
        val maxLevel: Int,
        val virtualizerEnabled: Boolean,
        val virtualizer: Int,
        val bassBoostEnabled: Boolean,
        val bassBoost: Int,
        val reverb: Int
) {
    val isCustom get() = selected == presets.lastIndex
    val isUserPreset get() = selected >= builtInCount && !isCustom
}

val ReverbPresets = listOf("None", "Large hall", "Large room", "Medium hall", "Medium room", "Small room", "Plate")

/** Equalizer and effects of the running player (ported from EqualizerPresenter). */
class EqualizerViewModel : ViewModel() {
    private val helper: AudioFXHelper? = AudioEffects.helper
    private val _state = MutableStateFlow(read())
    val state: StateFlow<EqState?> = _state.asStateFlow()

    private fun read(): EqState? {
        val h = helper ?: return null
        val eq = h.equalizer ?: return null
        val builtIn = eq.numberOfPresets.toInt()
        val presets = (0 until builtIn).map { eq.getPresetName(it.toShort()) } + h.userPresets.map { it.name } + "Custom"
        val range = eq.bandLevelRange
        return EqState(
                enabled = h.equalizerEnabled,
                presets = presets,
                selected = if (h.preset == -1) presets.lastIndex else h.preset.coerceIn(0, presets.lastIndex),
                builtInCount = builtIn,
                bandFrequencies = (0 until eq.numberOfBands).map { eq.getCenterFreq(it.toShort()) / 1000 },
                bandLevels = (0 until eq.numberOfBands).map { eq.getBandLevel(it.toShort()).toInt() },
                minLevel = range[0].toInt(),
                maxLevel = range[1].toInt(),
                virtualizerEnabled = h.virtualizerEnabled,
                virtualizer = h.virtualizerStrength.toInt(),
                bassBoostEnabled = h.bassboostEnabled,
                bassBoost = h.bassboostStrength.toInt(),
                reverb = h.presetReverbPreset.toInt())
    }

    private fun refresh() { _state.value = read() }

    fun setEnabled(enabled: Boolean) { helper?.equalizerEnabled = enabled; refresh() }

    fun pickPreset(index: Int) {
        val s = _state.value ?: return
        helper?.preset = if (index == s.presets.lastIndex) -1 else index
        refresh()
    }

    /** Moving a band switches to the "Custom" preset. */
    fun setBand(band: Int, level: Int) {
        val h = helper ?: return
        h.setEqualizerBandStrength(band.toShort(), level.toShort())
        if (h.preset != -1) h.preset = -1
        refresh()
    }

    fun setVirtualizerEnabled(enabled: Boolean) { helper?.virtualizerEnabled = enabled; refresh() }
    fun setVirtualizer(value: Int) { helper?.virtualizerStrength = value.toShort(); refresh() }
    fun setBassBoostEnabled(enabled: Boolean) { helper?.bassboostEnabled = enabled; refresh() }
    fun setBassBoost(value: Int) { helper?.bassboostStrength = value.toShort(); refresh() }
    fun setReverb(index: Int) { helper?.presetReverbPreset = index.toShort(); refresh() }

    fun saveCustom(name: String) {
        val h = helper ?: return
        viewModelScope.launch {
            val newIndex = (_state.value?.presets?.size ?: 1) - 1 // position the new preset takes (before "Custom")
            h.saveUserPreset(name)
            h.preset = newIndex
            refresh()
        }
    }

    fun deleteSelected() {
        val h = helper ?: return
        val s = _state.value ?: return
        viewModelScope.launch {
            h.deleteUserPreset(s.selected - s.builtInCount)
            h.preset = -1
            refresh()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerScreen(viewModel: EqualizerViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var dialog by remember { mutableStateOf<String?>(null) }
    TopLevelScaffold("Equalizer", actions = {
        state?.let { Switch(checked = it.enabled, onCheckedChange = viewModel::setEnabled, modifier = Modifier.padding(end = 12.dp)) }
    }) { padding ->
        val s = state
        if (s == null) {
            EmptyState("Audio effects aren't available on this device", Modifier.padding(padding), Icons.Default.GraphicEq)
            return@TopLevelScaffold
        }
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp).alpha(if (s.enabled) 1f else 0.5f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded, { expanded = it }, Modifier.weight(1f)) {
                    OutlinedTextField(
                            value = s.presets[s.selected], onValueChange = {}, readOnly = true, label = { Text("Preset") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                            enabled = s.enabled,
                            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable))
                    ExposedDropdownMenu(expanded, { expanded = false }) {
                        s.presets.forEachIndexed { i, name ->
                            DropdownMenuItem(text = { Text(name) }, onClick = { expanded = false; viewModel.pickPreset(i) })
                        }
                    }
                }
                when {
                    s.isCustom -> IconButton(onClick = { dialog = "save" }, enabled = s.enabled) { Icon(Icons.Default.Save, contentDescription = "Save preset") }
                    s.isUserPreset -> IconButton(onClick = { dialog = "delete" }, enabled = s.enabled) { Icon(Icons.Default.Delete, contentDescription = "Delete preset") }
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth().height(240.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                s.bandFrequencies.forEachIndexed { band, freq ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text("${s.bandLevels[band] / 100} dB", style = MaterialTheme.typography.labelSmall)
                        VerticalSlider(
                                value = s.bandLevels[band].toFloat(),
                                range = s.minLevel.toFloat()..s.maxLevel.toFloat(),
                                enabled = s.enabled,
                                onValueChange = { viewModel.setBand(band, it.toInt()) },
                                modifier = Modifier.weight(1f))
                        Text(if (freq >= 1000) "${freq / 1000}k" else "$freq", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
            EffectRow("Virtualizer", s.virtualizerEnabled, s.virtualizer, s.enabled, viewModel::setVirtualizerEnabled, viewModel::setVirtualizer)
            EffectRow("Bass boost", s.bassBoostEnabled, s.bassBoost, s.enabled, viewModel::setBassBoostEnabled, viewModel::setBassBoost)
            Spacer(Modifier.height(8.dp))
            var reverbOpen by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(reverbOpen, { reverbOpen = it }) {
                OutlinedTextField(
                        value = ReverbPresets.getOrElse(s.reverb) { "None" }, onValueChange = {}, readOnly = true, label = { Text("Reverb") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(reverbOpen) }, enabled = s.enabled,
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable))
                ExposedDropdownMenu(reverbOpen, { reverbOpen = false }) {
                    ReverbPresets.forEachIndexed { i, name -> DropdownMenuItem(text = { Text(name) }, onClick = { reverbOpen = false; viewModel.setReverb(i) }) }
                }
            }
        }
        when (dialog) {
            "save" -> TextInputDialog("Save preset", "Name", confirm = "Save", onDismiss = { dialog = null }) { viewModel.saveCustom(it) }
            "delete" -> ConfirmDialog("Delete preset", "Delete ${s.presets[s.selected]}?", "Delete", { dialog = null }) { viewModel.deleteSelected() }
        }
    }
}

@Composable
private fun EffectRow(title: String, enabled: Boolean, value: Int, eqEnabled: Boolean, onEnabled: (Boolean) -> Unit, onValue: (Int) -> Unit) {
    Column(Modifier.padding(vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Switch(checked = enabled, onCheckedChange = onEnabled, enabled = eqEnabled)
        }
        Slider(value = value.toFloat(), onValueChange = { onValue(it.toInt()) }, valueRange = 0f..1000f, enabled = eqEnabled && enabled)
    }
}

/** A Material slider rotated to be vertical (bottom = minimum). */
@Composable
private fun VerticalSlider(value: Float, range: ClosedFloatingPointRange<Float>, enabled: Boolean, onValueChange: (Float) -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.width(48.dp), contentAlignment = Alignment.Center) {
        Slider(
                value = value, onValueChange = onValueChange, valueRange = range, enabled = enabled,
                modifier = Modifier
                        .graphicsLayer { rotationZ = -90f }
                        .layout { measurable, constraints ->
                            val placeable = measurable.measure(Constraints(
                                    minWidth = constraints.minHeight, maxWidth = constraints.maxHeight,
                                    minHeight = constraints.minWidth, maxHeight = constraints.maxWidth))
                            layout(placeable.height, placeable.width) {
                                placeable.place(-(placeable.width - placeable.height) / 2, (placeable.width - placeable.height) / 2)
                            }
                        })
    }
}
