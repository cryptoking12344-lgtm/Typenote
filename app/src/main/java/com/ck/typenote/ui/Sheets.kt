package com.ck.typenote.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ck.typenote.Graph
import com.ck.typenote.data.InkStyle
import com.ck.typenote.data.Margins
import com.ck.typenote.data.Settings

private fun change(f: (Settings) -> Settings) = Graph.settings.update(f)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaperInkSheet(settings: Settings, onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Sheet,
        contentColor = Cream,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Text("Paper & ink", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)

            Section("Paper") {
                Segmented(listOf("Flat", "Curved"), if (settings.curved) 1 else 0) { i -> change { it.copy(curved = i == 1) } }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PaperColors.forEachIndexed { i, c ->
                        Box(
                            Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(c)
                                .border(2.dp, if (settings.paper == i) Brass else Color.Transparent, CircleShape)
                                .clickable(role = Role.RadioButton, onClickLabel = "${PaperNames[i]} paper") {
                                    change { it.copy(paper = i) }
                                }
                        )
                    }
                }
            }

            Section("Ink") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    InkStyle.entries.forEach { ink ->
                        val selected = settings.ink == ink
                        Column(
                            Modifier
                                .weight(1f)
                                .height(66.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(PaperColors[0])
                                .border(2.dp, if (selected) Brass else Color.Transparent, RoundedCornerShape(12.dp))
                                .clickable(role = Role.RadioButton) { change { it.copy(ink = ink) } },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                "Aa",
                                style = TextStyle(
                                    fontFamily = if (ink == InkStyle.Crisp) FontFamily.Monospace else TypeFont,
                                    fontSize = 20.sp,
                                    fontWeight = if (ink == InkStyle.Crisp) FontWeight.Bold else FontWeight.Normal,
                                    color = Ink,
                                    shadow = when (ink) {
                                        InkStyle.Fresh -> Shadow(Ink.copy(alpha = 0.45f), Offset(0.4f, 0.3f), 1.2f)
                                        InkStyle.Smudged -> Shadow(Ink.copy(alpha = 0.55f), Offset(0.8f, 0.6f), 3.2f)
                                        else -> null
                                    },
                                ),
                                modifier = Modifier.alpha(if (ink == InkStyle.Faded) 0.6f else 1f),
                            )
                            Text(ink.label, fontSize = 11.sp, color = Color(0xFF4A4238), fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Type size", fontSize = 15.sp, modifier = Modifier.weight(1f))
                Stepper(
                    value = "${settings.typeSize}",
                    onMinus = { change { it.copy(typeSize = (it.typeSize - 1).coerceAtLeast(13)) } },
                    onPlus = { change { it.copy(typeSize = (it.typeSize + 1).coerceAtMost(26)) } },
                    minusLabel = "Smaller text", plusLabel = "Larger text",
                )
            }

            Section("Margins") {
                Segmented(Margins.entries.map { it.label }, settings.margins.ordinal) { i ->
                    change { it.copy(margins = Margins.entries[i]) }
                }
            }

            Section("Backspace") {
                Segmented(listOf("Strike through", "Erase clean"), if (settings.strikeThrough) 0 else 1) { i ->
                    change { it.copy(strikeThrough = i == 0) }
                }
            }

            Column {
                Toggle("Key sounds", settings.keySounds) { v -> change { it.copy(keySounds = v) } }
                Toggle("Bell at end of line", settings.bell) { v -> change { it.copy(bell = v) } }
                Toggle("Haptics on every key", settings.haptics) { v -> change { it.copy(haptics = v) } }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LampSheet(settings: Settings, onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Sheet,
        contentColor = Cream,
        scrimColor = Color.Black.copy(alpha = 0.2f),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(LineIcons.Lamp, contentDescription = null, tint = Brass)
                Spacer(Modifier.width(10.dp))
                Text("Desk lamp", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Switch(
                    checked = settings.lamp,
                    onCheckedChange = { v -> change { it.copy(lamp = v) } },
                    colors = switchColors(),
                )
            }
            val kelvin = 4500 - settings.warmth * 25
            LampSlider("Warmth", "$kelvin K", settings.warmth) { v -> change { it.copy(warmth = v, lamp = true) } }
            LampSlider("Brightness", "${settings.brightness}%", settings.brightness) { v -> change { it.copy(brightness = v, lamp = true) } }
            LampSlider("Beam width", "${settings.beam}%", settings.beam) { v -> change { it.copy(beam = v, lamp = true) } }
            Text(
                "Tip: long-press the roller to switch the lamp on or off",
                fontSize = 12.sp, color = Muted, modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
    }
}

@Composable
private fun LampSlider(label: String, display: String, value: Int, onChange: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row {
            Text(label, fontSize = 13.sp, color = Label, modifier = Modifier.weight(1f))
            Text(display, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Brass)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SquareButton(LineIcons.Minus, "Decrease ${label.lowercase()}") { onChange((value - 5).coerceAtLeast(0)) }
            Slider(
                value = value.toFloat(),
                onValueChange = { onChange(it.toInt()) },
                valueRange = 0f..100f,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = Brass,
                    activeTrackColor = Brass,
                    inactiveTrackColor = Color(0xFF3A332B),
                ),
            )
            SquareButton(LineIcons.Plus, "Increase ${label.lowercase()}") { onChange((value + 5).coerceAtMost(100)) }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, fontSize = 13.sp, color = Label)
        content()
    }
}

@Composable
private fun Segmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Field)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        options.forEachIndexed { i, label ->
            val on = i == selected
            Box(
                Modifier
                    .weight(1f)
                    .heightIn(min = 40.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (on) Color(0xFF3A322A) else Color.Transparent)
                    .clickable(role = Role.RadioButton) { onSelect(i) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    fontSize = 14.sp,
                    fontWeight = if (on) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (on) PaperColors[0] else Label,
                    textDecoration = if (label == "Strike through") TextDecoration.LineThrough else null,
                )
            }
        }
    }
}

@Composable
private fun Toggle(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(role = Role.Switch) { onChange(!checked) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontSize = 15.sp, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange, colors = switchColors())
    }
}

@Composable
private fun switchColors() = SwitchDefaults.colors(
    checkedThumbColor = Color.White,
    checkedTrackColor = Ribbon,
    uncheckedThumbColor = Label,
    uncheckedTrackColor = Field,
    uncheckedBorderColor = FieldBorder,
)

@Composable
fun Stepper(value: String, onMinus: () -> Unit, onPlus: () -> Unit, minusLabel: String, plusLabel: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        SquareButton(LineIcons.Minus, minusLabel, onMinus)
        Text(value, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Brass, modifier = Modifier.width(28.dp))
        SquareButton(LineIcons.Plus, plusLabel, onPlus)
    }
}

@Composable
fun SquareButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Field)
            .border(1.dp, FieldBorder, RoundedCornerShape(12.dp))
            .clickable(role = Role.Button, onClickLabel = label, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = label, tint = Cream, modifier = Modifier.size(18.dp))
    }
}
