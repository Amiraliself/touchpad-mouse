package com.cursorpad.app

import android.content.Intent
import android.os.Bundle
import android.provider.Settings as AndroidSettings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

private val Accent = Color(0xFF7AA2FF)
private val Muted = Color(0xFFB4BCD0)

class MainActivity : ComponentActivity() {
    private val tick = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val prefs = Prefs(this)
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                MaterialTheme(
                    colorScheme = darkColorScheme(
                        primary = Accent,
                        onPrimary = Color(0xFF0B1020),
                        surface = Color(0xFF1B2133),
                        onSurface = Color.White
                    )
                ) {
                    Screen(prefs, tick.intValue) {
                        startActivity(Intent(AndroidSettings.ACTION_ACCESSIBILITY_SETTINGS))
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        tick.intValue++
    }
}

@Composable
fun Screen(p: Prefs, tick: Int, openSettings: () -> Unit) {
    val active = remember(tick) { MouseService.instance != null }

    var enabled by remember { mutableStateOf(p.enabled) }
    var shape by remember { mutableIntStateOf(p.shape) }
    var black by remember { mutableStateOf(p.black) }
    var curSize by remember { mutableFloatStateOf(p.sizeDp.toFloat()) }
    var speed by remember { mutableFloatStateOf(p.speed) }
    var accel by remember { mutableStateOf(p.accel) }
    var haptic by remember { mutableStateOf(p.haptic) }
    var edge by remember { mutableStateOf(p.edge) }
    var padAlpha by remember { mutableFloatStateOf(p.padAlpha.toFloat()) }
    var padWidth by remember { mutableFloatStateOf(p.padWidthDp.toFloat()) }
    var showDialog by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0E111A), Color(0xFF1C2336))))
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("نشانگر موس", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)

        Section("وضعیت") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("روشن / خاموش", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text(
                        when {
                            !active -> "سرویس دسترسی‌پذیری هنوز فعال نیست"
                            enabled -> "نشانگر و تاچ‌پد روشن است"
                            else -> "خاموش است"
                        },
                        color = if (active) Muted else Color(0xFFFFB86B),
                        fontSize = 13.sp
                    )
                }
                Switch(checked = enabled, onCheckedChange = { enabled = it; p.enabled = it })
            }
            if (!active) {
                Text(
                    "یک‌بار باید سرویس «نشانگر موس» را در تنظیمات دسترسی‌پذیری روشن کنی. " +
                        "اگر اندروید ۱۳ به بالا داری و گزینه خاکستری بود: تنظیمات ← برنامه‌ها ← نشانگر موس ← " +
                        "سه‌نقطه‌ی بالا ← «اجازه‌ی تنظیمات محدودشده».",
                    color = Muted, fontSize = 13.sp, lineHeight = 20.sp
                )
                Button(onClick = openSettings, modifier = Modifier.fillMaxWidth()) {
                    Text("باز کردن تنظیمات دسترسی‌پذیری")
                }
            }
        }

        Section("نشانگر") {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF8D96AB), Color(0xFF5D667C))))
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    drawIntoCanvas { c ->
                        val sz = curSize.dp.toPx()
                        val ox = if (shape < 2) size.width / 2 - sz * 0.2f else size.width / 2
                        val oy = if (shape < 2) size.height / 2 - sz * 0.5f else size.height / 2
                        CursorPainter.draw(c.nativeCanvas, shape, black, sz, ox, oy)
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                for (i in 0..3) {
                    val sel = shape == i
                    Box(
                        Modifier
                            .weight(1f)
                            .height(64.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Brush.linearGradient(listOf(Color(0xFF8D96AB), Color(0xFF5D667C))))
                            .border(
                                if (sel) 2.dp else 1.dp,
                                if (sel) Accent else Color(0x33FFFFFF),
                                RoundedCornerShape(14.dp)
                            )
                            .clickable { shape = i; p.shape = i }
                    ) {
                        Canvas(Modifier.fillMaxSize()) {
                            drawIntoCanvas { c ->
                                val sz = 26.dp.toPx()
                                val ox = if (i < 2) size.width / 2 - sz * 0.2f else size.width / 2
                                val oy = if (i < 2) size.height / 2 - sz * 0.5f else size.height / 2
                                CursorPainter.draw(c.nativeCanvas, i, black, sz, ox, oy)
                            }
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ColorChip("سفید", !black, Color.White, Color.Black, Modifier.weight(1f)) {
                    black = false; p.black = false
                }
                ColorChip("مشکی", black, Color.Black, Color.White, Modifier.weight(1f)) {
                    black = true; p.black = true
                }
            }
            SliderRow("اندازه‌ی نشانگر", "${curSize.roundToInt()}", curSize, 14f..64f) {
                curSize = it; p.sizeDp = it.roundToInt()
            }
        }

        Section("حرکت و حس موس") {
            SliderRow("سرعت نشانگر", String.format("%.1f", speed), speed, 0.5f..6f) {
                speed = it; p.speed = it
            }
            SwitchRow("شتاب نشانگر", "حرکت آهسته دقیق، حرکت تند دور", accel) {
                accel = it; p.accel = it
            }
            SwitchRow("لرزش موقع کلیک", null, haptic) { haptic = it; p.haptic = it }
            SwitchRow("سوایپ از لبه‌ها", "چپ/راست: بازگشت — پایین: هوم — پایین و نگه‌داشتن: برنامه‌های اخیر", edge) {
                edge = it; p.edge = it
            }
        }

        Section("تاچ‌پد") {
            SliderRow("شفافیت تاچ‌پد", "${padAlpha.roundToInt()}٪", padAlpha, 15f..100f) {
                padAlpha = it; p.padAlpha = it.roundToInt()
            }
            SliderRow("اندازه‌ی تاچ‌پد", "${padWidth.roundToInt()}", padWidth, 160f..380f) {
                padWidth = it; p.padWidthDp = it.roundToInt()
            }
            Text(
                "برای جابه‌جایی، نوار بالای تاچ‌پد را بگیر و بکش. جای تاچ‌پد برای حالت عمودی و افقی جدا ذخیره می‌شود. " +
                    "تاچ‌پد داخل فیلم صفحه دیده نمی‌شود.",
                color = Muted, fontSize = 13.sp, lineHeight = 20.sp
            )
        }

        Section("راهنمای حرکات") {
            Text(
                "• یک انگشت: حرکت نشانگر\n" +
                    "• تپ: کلیک\n" +
                    "• دو انگشت کشیدن: اسکرول\n" +
                    "• دو تپ و بعد کشیدن: درگ (انتخاب متن، جابه‌جایی)\n" +
                    "• تپ دو‌انگشتی: لمس طولانی (مثل کلیک راست)",
                color = Muted, fontSize = 13.sp, lineHeight = 22.sp
            )
        }

        Button(
            onClick = { showDialog = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5A2B35), contentColor = Color.White)
        ) { Text("بازیابی تنظیمات") }
        Box(Modifier.height(8.dp))
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("بازیابی تنظیمات") },
            text = { Text("همه‌ی تنظیمات و جای تاچ‌پد به حالت اولیه برمی‌گردد.") },
            confirmButton = {
                TextButton(onClick = {
                    p.reset()
                    enabled = p.enabled
                    shape = p.shape
                    black = p.black
                    curSize = p.sizeDp.toFloat()
                    speed = p.speed
                    accel = p.accel
                    haptic = p.haptic
                    edge = p.edge
                    padAlpha = p.padAlpha.toFloat()
                    padWidth = p.padWidthDp.toFloat()
                    MouseService.instance?.reload()
                    showDialog = false
                }) { Text("بازیابی") }
            },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("لغو") } }
        )
    }
}

@Composable
fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0x1CFFFFFF))
            .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(title, color = Accent, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        content()
    }
}

@Composable
fun SliderRow(
    label: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit
) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = Color.White, fontSize = 15.sp)
            Text(valueText, color = Accent, fontSize = 15.sp)
        }
        Slider(value = value, onValueChange = onChange, valueRange = range)
    }
}

@Composable
fun SwitchRow(label: String, sub: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, color = Color.White, fontSize = 15.sp)
            if (sub != null) Text(sub, color = Muted, fontSize = 12.sp, lineHeight = 18.sp)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
fun ColorChip(
    text: String,
    selected: Boolean,
    swatch: Color,
    textColor: Color,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier
            .height(46.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(swatch)
            .border(
                if (selected) 3.dp else 1.dp,
                if (selected) Accent else Color(0x55FFFFFF),
                RoundedCornerShape(14.dp)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = textColor, fontWeight = FontWeight.Bold)
    }
}
