package com.cursorpad.app

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings as AndroidSettings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

private val BgTop = Color(0xFF0B0E17)
private val BgBottom = Color(0xFF181D31)
private val Blue = Color(0xFF7AA2FF)
private val Green = Color(0xFF4ADE80)
private val Orange = Color(0xFFFFB86B)
private val Violet = Color(0xFFC084FC)
private val Cyan = Color(0xFF22D3EE)
private val Pink = Color(0xFFF472B6)
private val Red = Color(0xFFF87171)
private val Muted = Color(0xFFB4BCD0)

class MainActivity : ComponentActivity() {
    private val tick = mutableIntStateOf(0)
    private val notifPerm =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val prefs = Prefs(this)
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notifPerm.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                MaterialTheme(
                    colorScheme = darkColorScheme(
                        primary = Blue,
                        onPrimary = Color(0xFF0B1020),
                        surface = Color(0xFF1B2133),
                        onSurface = Color.White
                    )
                ) {
                    App(prefs, tick.intValue)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        tick.intValue++
    }
}

private fun safeStart(ctx: Context, i: Intent) {
    try {
        ctx.startActivity(i)
    } catch (e: Exception) {
        Toast.makeText(ctx, "باز نشد", Toast.LENGTH_SHORT).show()
    }
}

private fun copyText(ctx: Context, text: String) {
    val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("id", text))
    Toast.makeText(ctx, "کپی شد", Toast.LENGTH_SHORT).show()
}

@Composable
fun App(p: Prefs, tick: Int) {
    var tab by remember { mutableIntStateOf(0) }
    Column(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(BgTop, BgBottom)))
            .systemBarsPadding()
    ) {
        Box(Modifier.weight(1f)) {
            when (tab) {
                0 -> HomeTab(p, tick)
                1 -> TutorialTab()
                else -> ContactTab()
            }
        }
        BottomBar(tab) { tab = it }
    }
}

@Composable
fun BottomBar(selected: Int, onSelect: (Int) -> Unit) {
    val items = listOf("⚙️" to "تنظیمات", "📖" to "آموزش", "💬" to "ارتباط با ما")
    Row(
        Modifier
            .fillMaxWidth()
            .background(Color(0x22FFFFFF))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items.forEachIndexed { i, item ->
            val sel = i == selected
            Column(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (sel) Color(0x337AA2FF) else Color.Transparent)
                    .clickable { onSelect(i) }
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(item.first, fontSize = 20.sp)
                Text(
                    item.second,
                    color = if (sel) Blue else Muted,
                    fontSize = 12.sp,
                    fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

// ======================= تنظیمات =======================

@Composable
fun HomeTab(p: Prefs, tick: Int) {
    val ctx = LocalContext.current
    val active = remember(tick) { MouseService.instance != null }

    var enabled by remember(tick) { mutableStateOf(p.enabled) }
    var shape by remember { mutableIntStateOf(p.shape) }
    var black by remember { mutableStateOf(p.black) }
    var curSize by remember { mutableFloatStateOf(p.sizeDp.toFloat()) }
    var curAlpha by remember { mutableFloatStateOf(p.cursorAlpha.toFloat()) }
    var shadow by remember { mutableStateOf(p.shadow) }
    var anim by remember { mutableIntStateOf(p.clickAnim) }
    var animColor by remember { mutableIntStateOf(p.clickColor) }
    var speed by remember { mutableFloatStateOf(p.speed) }
    var accel by remember { mutableStateOf(p.accel) }
    var haptic by remember { mutableStateOf(p.haptic) }
    var edge by remember { mutableStateOf(p.edge) }
    var rightBtn by remember { mutableStateOf(p.rightBtn) }
    var dragHold by remember { mutableFloatStateOf(p.dragHold.toFloat()) }
    var scrollRev by remember { mutableStateOf(p.scrollReverse) }
    var scrollSens by remember { mutableFloatStateOf(p.scrollSens) }
    var padAlpha by remember { mutableFloatStateOf(p.padAlpha.toFloat()) }
    var padWidth by remember { mutableFloatStateOf(p.padWidthDp.toFloat()) }
    var theme by remember { mutableIntStateOf(p.padTheme) }
    var locked by remember { mutableStateOf(p.padLocked) }
    var notif by remember { mutableStateOf(p.notif) }
    var showDialog by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ---------- سربرگ ----------
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF3B5BDB), Color(0xFF9C36B5))))
                .padding(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("نشانگر موس", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                        Text("موس و تاچ‌پد لپ‌تاپی برای گوشی", color = Color(0xCCFFFFFF), fontSize = 13.sp)
                    }
                    Box(
                        Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color(0x33FFFFFF)),
                        contentAlignment = Alignment.Center
                    ) { Text("🖱️", fontSize = 28.sp) }
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0x33000000))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("روشن / خاموش", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        Text(
                            when {
                                !active -> "سرویس دسترسی‌پذیری هنوز فعال نیست"
                                enabled -> "نشانگر و تاچ‌پد روشن است"
                                else -> "خاموش است"
                            },
                            color = if (active) Color(0xFFB9F6CA) else Color(0xFFFFD180),
                            fontSize = 13.sp
                        )
                    }
                    Switch(
                        checked = enabled,
                        onCheckedChange = { enabled = it; p.enabled = it },
                        colors = switchColors(Green)
                    )
                }
            }
        }

        if (!active) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color(0x22FFB86B))
                    .border(1.dp, Orange.copy(alpha = 0.5f), RoundedCornerShape(22.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("⚠️ سرویس دسترسی‌پذیری فعال نیست", color = Orange, fontWeight = FontWeight.Bold)
                Text(
                    "یک‌بار باید سرویس «نشانگر موس» را در تنظیمات دسترسی‌پذیری روشن کنی. " +
                        "اگر گزینه خاکستری بود: تنظیمات ← برنامه‌ها ← نشانگر موس ← سه‌نقطه‌ی بالا ← " +
                        "«اجازه‌ی تنظیمات محدودشده». جزئیات در بخش آموزش.",
                    color = Muted, fontSize = 13.sp, lineHeight = 20.sp
                )
                Button(
                    onClick = { safeStart(ctx, Intent(AndroidSettings.ACTION_ACCESSIBILITY_SETTINGS)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Orange, contentColor = Color(0xFF1B1200))
                ) { Text("باز کردن تنظیمات دسترسی‌پذیری", fontWeight = FontWeight.Bold) }
            }
        }

        // ---------- نشانگر ----------
        Section("نشانگر", "🖱️", Blue) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF8D96AB), Color(0xFF5D667C))))
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    drawIntoCanvas { c ->
                        val sz = curSize.dp.toPx()
                        CursorPainter.draw(
                            c.nativeCanvas, shape, black, shadow, sz,
                            size.width / 2 + CursorPainter.offX(shape, sz),
                            size.height / 2 + CursorPainter.offY(shape, sz)
                        )
                    }
                }
            }
            for (row in 0..1) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    for (col in 0..3) {
                        val i = row * 4 + col
                        val sel = shape == i
                        Box(
                            Modifier
                                .weight(1f)
                                .height(66.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Brush.linearGradient(listOf(Color(0xFF8D96AB), Color(0xFF5D667C))))
                                .border(
                                    if (sel) 3.dp else 1.dp,
                                    if (sel) Blue else Color(0x33FFFFFF),
                                    RoundedCornerShape(14.dp)
                                )
                                .clickable { shape = i; p.shape = i }
                        ) {
                            Canvas(Modifier.fillMaxSize()) {
                                drawIntoCanvas { c ->
                                    val sz = 26.dp.toPx()
                                    CursorPainter.draw(
                                        c.nativeCanvas, i, black, shadow, sz,
                                        size.width / 2 + CursorPainter.offX(i, sz),
                                        size.height / 2 + CursorPainter.offY(i, sz)
                                    )
                                }
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
            SliderRow("اندازه‌ی نشانگر", "${curSize.roundToInt()}", curSize, 14f..64f, Blue) {
                curSize = it; p.sizeDp = it.roundToInt()
            }
            SliderRow("شفافیت نشانگر", "${curAlpha.roundToInt()}٪", curAlpha, 20f..100f, Blue) {
                curAlpha = it; p.cursorAlpha = it.roundToInt()
            }
            SwitchRow("سایه‌ی نشانگر", "برای دیده شدن روی زمینه‌های روشن و تیره", shadow, Blue) {
                shadow = it; p.shadow = it
            }
            Text("انیمیشن کلیک", color = Color.White, fontSize = 15.sp)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Chip("خاموش", anim == 0, Blue, Modifier.weight(1f)) { anim = 0; p.clickAnim = 0 }
                Chip("حلقه", anim == 1, Blue, Modifier.weight(1f)) { anim = 1; p.clickAnim = 1 }
                Chip("ضربه", anim == 2, Blue, Modifier.weight(1f)) { anim = 2; p.clickAnim = 2 }
            }
            Text("رنگ انیمیشن", color = Color.White, fontSize = 15.sp)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                for (i in Themes.accents.indices) {
                    val sel = animColor == i
                    Box(
                        Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(Themes.accents[i]))
                            .border(
                                if (sel) 3.dp else 1.dp,
                                if (sel) Color.White else Color(0x44FFFFFF),
                                CircleShape
                            )
                            .clickable { animColor = i; p.clickColor = i }
                    )
                }
            }
        }

        // ---------- حرکت و کلیک ----------
        Section("حرکت و کلیک", "👆", Green) {
            SliderRow("سرعت نشانگر", String.format("%.1f", speed), speed, 0.5f..6f, Green) {
                speed = it; p.speed = it
            }
            SwitchRow("شتاب نشانگر", "حرکت آهسته دقیق، حرکت تند دور", accel, Green) {
                accel = it; p.accel = it
            }
            SwitchRow("لرزش موقع کلیک", null, haptic, Green) { haptic = it; p.haptic = it }
            SwitchRow("دکمه‌ی کلیک راست", "پایین سمت راست تاچ‌پد (لمس طولانی)", rightBtn, Green) {
                rightBtn = it; p.rightBtn = it
            }
            SwitchRow(
                "سوایپ از لبه‌ها",
                "چپ/راست: بازگشت — پایین: هوم — پایین و نگه‌داشتن: برنامه‌های اخیر",
                edge, Green
            ) { edge = it; p.edge = it }
            SliderRow("تأخیر قبل از درگ", "${dragHold.roundToInt()} ms", dragHold, 0f..1000f, Green) {
                dragHold = it; p.dragHold = it.roundToInt()
            }
        }

        // ---------- اسکرول و زوم ----------
        Section("اسکرول و زوم", "📜", Orange) {
            SwitchRow("جهت اسکرول برعکس", "خاموش: مثل گوشی — روشن: مثل ویندوز", scrollRev, Orange) {
                scrollRev = it; p.scrollReverse = it
            }
            SliderRow("حساسیت اسکرول", String.format("%.1f", scrollSens), scrollSens, 0.4f..3f, Orange) {
                scrollSens = it; p.scrollSens = it
            }
            Text(
                "اسکرول و زوم بعد از برداشتن انگشت‌ها از تاچ‌پد اجرا می‌شوند.",
                color = Muted, fontSize = 12.sp, lineHeight = 18.sp
            )
        }

        // ---------- تاچ‌پد ----------
        Section("تاچ‌پد", "🎛️", Violet) {
            SliderRow("شفافیت تاچ‌پد", "${padAlpha.roundToInt()}٪", padAlpha, 0f..100f, Violet) {
                padAlpha = it; p.padAlpha = it.roundToInt()
            }
            if (padAlpha < 4f) {
                Text(
                    "تاچ‌پد نامرئی است ولی همچنان کار می‌کند.",
                    color = Violet, fontSize = 12.sp
                )
            }
            SliderRow("اندازه‌ی تاچ‌پد", "${padWidth.roundToInt()}", padWidth, 160f..380f, Violet) {
                padWidth = it; p.padWidthDp = it.roundToInt()
            }
            Text("رنگ تاچ‌پد", color = Color.White, fontSize = 15.sp)
            val themes = Themes.pad
            for (rowItems in themes.indices.chunked(5)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (i in rowItems) {
                        val t = themes[i]
                        val sel = theme == i
                        Column(
                            Modifier
                                .weight(1f)
                                .clickable { theme = i; p.padTheme = i },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(46.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(
                                                Color(t.top).copy(alpha = 1f),
                                                Color(t.bottom).copy(alpha = 1f)
                                            )
                                        )
                                    )
                                    .border(
                                        if (sel) 3.dp else 1.dp,
                                        if (sel) Color(t.accent) else Color(0x44FFFFFF),
                                        RoundedCornerShape(14.dp)
                                    )
                            )
                            Text(t.name, color = if (sel) Color.White else Muted, fontSize = 11.sp)
                        }
                    }
                    for (k in rowItems.size until 5) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
            SwitchRow("قفل جای تاچ‌پد", "با کشیدن اتفاقی جابه‌جا نشود", locked, Violet) {
                locked = it; p.padLocked = it
            }
        }

        // ---------- ماندگاری ----------
        Section("ماندگاری و میانبر", "🔋", Cyan) {
            SwitchRow("اعلان ثابت", "برنامه را زنده نگه می‌دارد و دکمه‌ی روشن/خاموش دارد", notif, Cyan) {
                notif = it; p.notif = it
            }
            Text(
                "برای اینکه با بستن برنامه نشانگر خاموش نشود، این دو را انجام بده و بخش «آموزش» را هم بخوان:",
                color = Muted, fontSize = 13.sp, lineHeight = 20.sp
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { safeStart(ctx, Intent(AndroidSettings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Cyan, contentColor = Color(0xFF00232B))
                ) { Text("تنظیمات باتری", fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                Button(
                    onClick = {
                        safeStart(
                            ctx,
                            Intent(
                                AndroidSettings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.parse("package:" + ctx.packageName)
                            )
                        )
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Cyan, contentColor = Color(0xFF00232B))
                ) { Text("اطلاعات برنامه", fontSize = 13.sp, fontWeight = FontWeight.Bold) }
            }
        }

        Button(
            onClick = { showDialog = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5A2B35), contentColor = Color.White)
        ) { Text("بازیابی تنظیمات", fontWeight = FontWeight.Bold) }
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
                    curAlpha = p.cursorAlpha.toFloat()
                    shadow = p.shadow
                    anim = p.clickAnim
                    animColor = p.clickColor
                    speed = p.speed
                    accel = p.accel
                    haptic = p.haptic
                    edge = p.edge
                    rightBtn = p.rightBtn
                    dragHold = p.dragHold.toFloat()
                    scrollRev = p.scrollReverse
                    scrollSens = p.scrollSens
                    padAlpha = p.padAlpha.toFloat()
                    padWidth = p.padWidthDp.toFloat()
                    theme = p.padTheme
                    locked = p.padLocked
                    notif = p.notif
                    MouseService.instance?.reload()
                    showDialog = false
                }) { Text("بازیابی") }
            },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("لغو") } }
        )
    }
}

// ======================= آموزش =======================

@Composable
fun TutorialTab() {
    val entries = listOf(
        Triple(
            "🚀", "شروع سریع",
            "۱) برنامه را باز کن.\n" +
                "۲) در بخش تنظیمات روی «باز کردن تنظیمات دسترسی‌پذیری» بزن، سرویس «نشانگر موس» را پیدا کن و روشنش کن.\n" +
                "۳) اگر گزینه خاکستری بود (اندروید ۱۳ به بالا): تنظیمات گوشی ← برنامه‌ها ← نشانگر موس ← سه‌نقطه‌ی بالا ← «اجازه‌ی تنظیمات محدودشده». بعد دوباره سرویس را روشن کن.\n" +
                "۴) به برنامه برگرد؛ نشانگر و تاچ‌پد روی صفحه ظاهر می‌شوند.\n\n" +
                "این کار فقط یک‌بار لازم است. اگر نسخه‌ی جدید نصب کردی، اول نسخه‌ی قبلی را پاک کن."
        ),
        Triple(
            "👆", "حرکات تاچ‌پد",
            "• یک انگشت: حرکت نشانگر\n" +
                "• تپ: کلیک\n" +
                "• دو تپ و بعد کشیدن (تپ دوم را نگه دار و بکش): درگ. برای انتخاب متن، جابه‌جایی آیکون و کشیدن اسلایدر. نشانگر هنگام کشیدن حرکت می‌کند و بعد از برداشتن انگشت، عمل اجرا می‌شود.\n" +
                "• دو تپ سریع بدون کشیدن: دابل‌کلیک\n" +
                "• تپ دو انگشتی: لمس طولانی\n" +
                "• دکمه‌ی «کلیک راست» پایین سمت راست تاچ‌پد: لمس طولانی. اندروید کلیک راست واقعی ندارد و نزدیک‌ترین معادلش لمس طولانی است.\n" +
                "• کشیدن دو انگشت: اسکرول\n" +
                "• جمع یا باز کردن دو انگشت: زوم"
        ),
        Triple(
            "📜", "اسکرول، درگ و زوم",
            "اندروید در بیشتر گوشی‌ها اجازه نمی‌دهد وقتی انگشتت روی تاچ‌پد است، لمس شبیه‌سازی‌شده هم‌زمان اجرا شود. برای همین اسکرول، درگ و زوم بعد از برداشتن انگشت اجرا می‌شوند.\n\n" +
                "اسکرول: دو انگشت را بکش و رها کن. هرچه بیشتر بکشی، صفحه بیشتر جابه‌جا می‌شود و هرچه تندتر بکشی، سرعتش بیشتر است.\n\n" +
                "اگر مقدار اسکرول کم یا زیاد بود، «حساسیت اسکرول» را تنظیم کن. با «جهت اسکرول برعکس» جهت را مثل ویندوز کن.\n\n" +
                "درگ: «تأخیر قبل از درگ» مدت نگه‌داشتن اولیه‌ی لمس است. برای انتخاب متن و جابه‌جایی آیکون حدود ۴۵۰ میلی‌ثانیه خوب است. برای کشیدن اسلایدر می‌توانی کمترش کنی یا صفر بگذاری.\n\n" +
                "زوم: دو انگشت را از هم دور کن (زوم‌این) یا به هم نزدیک کن (زوم‌اوت) و رها کن."
        ),
        Triple(
            "↔️", "سوایپ لبه‌ها",
            "اگر روشن باشد:\n" +
                "• شروع از لبه‌ی چپ یا راست تاچ‌پد و کشیدن به داخل: بازگشت\n" +
                "• شروع از لبه‌ی پایین و کشیدن به بالا: هوم\n" +
                "• همان کشیدن به بالا و چند لحظه نگه‌داشتن قبل از برداشتن انگشت: برنامه‌های اخیر\n\n" +
                "نوارهای کم‌رنگ روی تاچ‌پد محل لبه‌ها را نشان می‌دهند. اگر اتفاقی فعال می‌شوند، این گزینه را خاموش کن."
        ),
        Triple(
            "🧲", "جابه‌جایی و قفل تاچ‌پد",
            "نوار باریک بالای تاچ‌پد را بگیر و بکش تا جابه‌جا شود. جای حالت عمودی و افقی جدا ذخیره می‌شود.\n\n" +
                "با «قفل جای تاچ‌پد» دیگر با کشیدن اتفاقی جابه‌جا نمی‌شود. برای جابه‌جایی دوباره، قفل را خاموش کن.\n\n" +
                "اندازه‌ی تاچ‌پد را با اسلایدر «اندازه‌ی تاچ‌پد» تنظیم کن. نشانگر نمی‌تواند از صفحه‌ی گوشی بیرون برود."
        ),
        Triple(
            "🖱️", "تنظیمات نشانگر",
            "• شکل: ۸ مدل (ویندوز، مک، فلش توخالی، دست، دایره، نقطه، ضربدر، هدف)\n" +
                "• رنگ: سفید یا مشکی\n" +
                "• اندازه و شفافیت: با اسلایدر\n" +
                "• سایه: دیده شدن روی زمینه‌های مختلف\n" +
                "• انیمیشن کلیک: خاموش، حلقه یا ضربه، با ۶ رنگ دلخواه\n" +
                "• سرعت: سرعت پایه‌ی حرکت\n" +
                "• شتاب: حرکت آهسته دقیق و حرکت تند دور، مثل لپ‌تاپ"
        ),
        Triple(
            "🎨", "شفافیت و رنگ تاچ‌پد",
            "شفافیت را تا ۰٪ می‌توانی پایین بیاوری. در این حالت تاچ‌پد نامرئی است ولی همچنان کار می‌کند. اگر جایش را فراموش کردی، شفافیت را از برنامه بالا ببر.\n\n" +
                "۹ رنگ‌بندی آماده داری: نیمه‌شب، گرافیت، زمرد، یاقوت، بنفش، کهربا، اقیانوس، صورتی و شیشه‌ای."
        ),
        Triple(
            "🎥", "ضبط صفحه",
            "تاچ‌پد با ویژگی امنیتی اندروید در فیلم‌برداری و اسکرین‌شات دیده نمی‌شود، ولی نشانگر دیده می‌شود.\n\n" +
                "این روی ضبط‌کننده‌ی خود سیستم معمولاً جواب می‌دهد. بعضی برنامه‌های ضبط ممکن است این ویژگی را نادیده بگیرند. اگر تاچ‌پد در فیلم دیده شد، با ضبط‌کننده‌ی خود گوشی امتحان کن."
        ),
        Triple(
            "⚡", "روشن و خاموش سریع",
            "• کاشی سریع: پرده‌ی تنظیمات سریع را باز کن، دکمه‌ی ویرایش (مداد) را بزن و کاشی «نشانگر موس» را به بالا بکش. بعد با یک لمس روشن و خاموش می‌شود.\n" +
                "• اعلان ثابت: وقتی روشن باشد، توی اعلان‌ها دکمه‌ی روشن/خاموش داری.\n" +
                "• داخل برنامه: کلید بزرگ بالای صفحه‌ی تنظیمات."
        ),
        Triple(
            "🔋", "خاموش نشدن با بستن برنامه",
            "بعضی گوشی‌ها (شیائومی، سامسونگ، اوپو، هواوی و…) برنامه‌ها را بعد از بستن یا برای صرفه‌جویی باتری می‌بندند. برای ماندگاری:\n\n" +
                "۱) اعلان ثابت را روشن نگه دار.\n" +
                "۲) برنامه را در لیست برنامه‌های اخیر قفل کن (انگشت را روی کارت برنامه نگه دار ← Lock).\n" +
                "۳) در تنظیمات باتری برای این برنامه «بدون محدودیت» را انتخاب کن (دکمه‌ی «تنظیمات باتری» در بخش ماندگاری).\n" +
                "۴) در شیائومی: «شروع خودکار» (Autostart) را برای برنامه روشن کن.\n\n" +
                "بعد از این‌ها بستن برنامه نشانگر را خاموش نمی‌کند. فقط کلید داخل برنامه، کاشی سریع یا اعلان آن را خاموش می‌کند."
        ),
        Triple(
            "🛠️", "رفع مشکل",
            "• نشانگر نمی‌آید: سرویس دسترسی‌پذیری و کلید روشن/خاموش را بررسی کن.\n" +
                "• کلیک کار نمی‌کند: سرویس را یک‌بار از تنظیمات دسترسی‌پذیری خاموش و روشن کن.\n" +
                "• درگ کار نکرد: «تأخیر قبل از درگ» را کم یا زیاد کن.\n" +
                "• اسکرول کم یا زیاد است: «حساسیت اسکرول» را تنظیم کن.\n" +
                "• اسکرول و درگ زیر خود تاچ‌پد: تاچ‌پد را کمی جابه‌جا کن.\n" +
                "• بعضی برنامه‌ها (مثل بانک‌ها) لمس شبیه‌سازی‌شده را مسدود می‌کنند.\n" +
                "• اگر چیزی درست نشد، از بخش «ارتباط با ما» پیام بده."
        )
    )
    val accents = listOf(Blue, Green, Orange, Violet, Cyan, Pink, Red)

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF0F766E), Color(0xFF2563EB))))
                .padding(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("📖 آموزش کامل", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text("روی هر بخش بزن تا باز شود", color = Color(0xCCFFFFFF), fontSize = 13.sp)
            }
        }
        entries.forEachIndexed { i, e ->
            Accordion(e.first, e.second, e.third, accents[i % accents.size], i == 0)
        }
        Box(Modifier.height(8.dp))
    }
}

@Composable
fun Accordion(emoji: String, title: String, body: String, accent: Color, startOpen: Boolean) {
    var open by remember { mutableStateOf(startOpen) }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0x1AFFFFFF))
            .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable { open = !open }
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center
            ) { Text(emoji, fontSize = 18.sp) }
            Text(
                title,
                modifier = Modifier.weight(1f),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Text(if (open) "▲" else "▼", color = accent, fontSize = 13.sp)
        }
        if (open) {
            Text(
                body,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                color = Muted,
                fontSize = 14.sp,
                lineHeight = 25.sp
            )
        }
    }
}

// ======================= ارتباط با ما =======================

@Composable
fun ContactTab() {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp))
                .background(Brush.linearGradient(listOf(Color(0xFFDB2777), Color(0xFF7C3AED))))
                .padding(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("💬 ارتباط با ما", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text(
                    "نظر، پیشنهاد یا گزارش مشکل داری؟ از طریق روبیکا با ما در ارتباط باش.",
                    color = Color(0xDDFFFFFF), fontSize = 14.sp, lineHeight = 22.sp
                )
            }
        }
        IdCard("@Amirali_asli_1", "Amirali_asli_1", Pink)
        IdCard("@BaLe_KaBiR", "BaLe_KaBiR", Violet)
        Section("نکته", "💡", Cyan) {
            Text(
                "اگر دکمه‌ی «باز کردن» کار نکرد، آیدی را کپی کن و داخل روبیکا جستجو کن. " +
                    "هنگام گزارش مشکل، مدل گوشی، نسخه‌ی اندروید و توضیح کوتاهی از اتفاقی که افتاد را بنویس.",
                color = Muted, fontSize = 14.sp, lineHeight = 23.sp
            )
        }
        Text(
            "نشانگر موس — نسخه‌ی ۱٫۱",
            color = Muted,
            fontSize = 12.sp,
            modifier = Modifier.fillMaxWidth()
        )
        Box(Modifier.height(8.dp))
    }
}

@Composable
fun IdCard(display: String, username: String, accent: Color) {
    val ctx = LocalContext.current
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0x1AFFFFFF))
            .border(1.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(22.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) { Text("👤", fontSize = 20.sp) }
            Text(display, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = { copyText(ctx, display) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0x33FFFFFF), contentColor = Color.White)
            ) { Text("کپی آیدی") }
            Button(
                onClick = { safeStart(ctx, Intent(Intent.ACTION_VIEW, Uri.parse("https://rubika.ir/" + username))) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color(0xFF10131C))
            ) { Text("باز کردن", fontWeight = FontWeight.Bold) }
        }
    }
}

// ======================= اجزای مشترک =======================

@Composable
fun switchColors(accent: Color) = SwitchDefaults.colors(
    checkedThumbColor = Color.White,
    checkedTrackColor = accent,
    uncheckedThumbColor = Color(0xFFB4BCD0),
    uncheckedTrackColor = Color(0x33FFFFFF),
    uncheckedBorderColor = Color(0x44FFFFFF)
)

@Composable
fun Section(title: String, emoji: String, accent: Color, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0x1AFFFFFF))
            .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(22.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center
            ) { Text(emoji, fontSize = 17.sp) }
            Text(title, color = accent, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
        content()
    }
}

@Composable
fun SliderRow(
    label: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    accent: Color,
    onChange: (Float) -> Unit
) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = Color.White, fontSize = 15.sp)
            Text(valueText, color = accent, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = accent,
                activeTrackColor = accent,
                inactiveTrackColor = Color(0x33FFFFFF)
            )
        )
    }
}

@Composable
fun SwitchRow(label: String, sub: String?, checked: Boolean, accent: Color, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, color = Color.White, fontSize = 15.sp)
            if (sub != null) Text(sub, color = Muted, fontSize = 12.sp, lineHeight = 18.sp)
        }
        Switch(checked = checked, onCheckedChange = onChange, colors = switchColors(accent))
    }
}

@Composable
fun Chip(text: String, selected: Boolean, accent: Color, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .height(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) accent.copy(alpha = 0.25f) else Color(0x14FFFFFF))
            .border(
                if (selected) 2.dp else 1.dp,
                if (selected) accent else Color(0x33FFFFFF),
                RoundedCornerShape(14.dp)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            color = if (selected) Color.White else Muted,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
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
                if (selected) Blue else Color(0x55FFFFFF),
                RoundedCornerShape(14.dp)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = textColor, fontWeight = FontWeight.Bold)
    }
}
