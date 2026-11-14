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
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

// ======================= زبان =======================

val LocalLang = compositionLocalOf { "fa" }

@Composable
fun tr(fa: String, en: String): String = if (LocalLang.current == "en") en else fa

// ======================= رنگ‌ها =======================

private val Ink = Color(0xFF070A14)
private val Indigo = Color(0xFF818CF8)
private val Green = Color(0xFF4ADE80)
private val Orange = Color(0xFFFFB86B)
private val Violet = Color(0xFFC084FC)
private val Cyan = Color(0xFF22D3EE)
private val Pink = Color(0xFFF472B6)
private val Red = Color(0xFFF87171)
private val Muted = Color(0xFFB4BCD0)
private val GlassBorder = Brush.linearGradient(
    listOf(Color(0x66FFFFFF), Color(0x0AFFFFFF), Color(0x33FFFFFF))
)
private val THEME_EN = listOf(
    "Midnight", "Graphite", "Emerald", "Ruby", "Violet", "Amber", "Ocean", "Pink", "Glass"
)

fun Modifier.glass(radius: Dp = 24.dp, tint: Color = Color(0x16FFFFFF)): Modifier {
    val shape = RoundedCornerShape(radius)
    return this.clip(shape).background(tint).border(1.dp, GlassBorder, shape)
}

// ======================= اکتیویتی =======================

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
            var lang by remember { mutableStateOf(prefs.lang) }
            CompositionLocalProvider(
                LocalLang provides lang,
                LocalLayoutDirection provides
                    (if (lang == "en") LayoutDirection.Ltr else LayoutDirection.Rtl)
            ) {
                MaterialTheme(
                    colorScheme = darkColorScheme(
                        primary = Indigo,
                        onPrimary = Color(0xFF0B1020),
                        surface = Color(0xFF1B2133),
                        onSurface = Color.White
                    )
                ) {
                    App(prefs, tick.intValue) { l ->
                        lang = l
                        prefs.lang = l
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

private fun safeStart(ctx: Context, i: Intent, failMsg: String) {
    try {
        ctx.startActivity(i)
    } catch (e: Exception) {
        Toast.makeText(ctx, failMsg, Toast.LENGTH_SHORT).show()
    }
}

private fun copyText(ctx: Context, text: String, msg: String) {
    val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("text", text))
    Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
}

// ======================= قالب کلی =======================

@Composable
fun App(p: Prefs, tick: Int, onLang: (String) -> Unit) {
    var tab by remember { mutableIntStateOf(0) }
    var welcome by remember { mutableStateOf(!p.welcomeDone) }
    AuroraBackground {
        if (welcome) {
            Welcome(onLang) {
                p.welcomeDone = true
                welcome = false
            }
        } else {
            Column(Modifier.fillMaxSize().systemBarsPadding()) {
                Box(Modifier.weight(1f)) {
                    when (tab) {
                        0 -> HomeTab(p, tick, onLang)
                        1 -> TutorialTab()
                        else -> ContactTab()
                    }
                }
                BottomBar(tab) { tab = it }
            }
        }
    }
}

private fun DrawScope.blob(color: Color, center: Offset, radius: Float, alpha: Float) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color.copy(alpha = alpha), Color.Transparent),
            center = center,
            radius = radius
        ),
        radius = radius,
        center = center
    )
}

@Composable
fun AuroraBackground(content: @Composable () -> Unit) {
    val t = rememberInfiniteTransition(label = "bg")
    val a by t.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(15000, easing = LinearEasing), RepeatMode.Reverse),
        label = "a"
    )
    val b by t.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(21000, easing = LinearEasing), RepeatMode.Reverse),
        label = "b"
    )
    Box(Modifier.fillMaxSize().background(Ink)) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            blob(Color(0xFF6366F1), Offset(w * (0.1f + 0.6f * a), h * (0.08f + 0.18f * b)), w * 0.95f, 0.55f)
            blob(Color(0xFFEC4899), Offset(w * (0.9f - 0.6f * b), h * (0.45f + 0.15f * a)), w * 0.85f, 0.38f)
            blob(Color(0xFF06B6D4), Offset(w * (0.2f + 0.5f * a), h * (0.9f - 0.2f * b)), w * 0.9f, 0.34f)
            blob(Color(0xFF9333EA), Offset(w * (0.8f - 0.4f * a), h * (0.75f + 0.1f * b)), w * 0.7f, 0.3f)
        }
        content()
    }
}

@Composable
fun BottomBar(selected: Int, onSelect: (Int) -> Unit) {
    val items = listOf(
        Triple("⚙️", tr("تنظیمات", "Settings"), Indigo),
        Triple("📖", tr("آموزش", "Guide"), Green),
        Triple("💬", tr("ارتباط با ما", "Contact"), Pink)
    )
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .glass(30.dp, Color(0x26FFFFFF))
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items.forEachIndexed { i, item ->
            val sel = i == selected
            val bg by animateColorAsState(
                if (sel) item.third.copy(alpha = 0.30f) else Color.Transparent,
                label = "navbg"
            )
            val sc by animateFloatAsState(if (sel) 1.1f else 1f, label = "navsc")
            Column(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(bg)
                    .clickable { onSelect(i) }
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(item.first, fontSize = (20f * sc).sp)
                Text(
                    item.second,
                    color = if (sel) Color.White else Muted,
                    fontSize = 12.sp,
                    fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
fun LangSwitch(lang: String, onLang: (String) -> Unit) {
    Row(Modifier.glass(20.dp, Color(0x22FFFFFF)).padding(3.dp)) {
        listOf("fa" to "فارسی", "en" to "EN").forEach { (code, label) ->
            val sel = lang == code
            Box(
                Modifier
                    .clip(RoundedCornerShape(17.dp))
                    .background(if (sel) Indigo.copy(alpha = 0.40f) else Color.Transparent)
                    .clickable { onLang(code) }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    label,
                    color = if (sel) Color.White else Muted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ======================= تاچ‌پد متحرک =======================

@Composable
fun HeroPad(modifier: Modifier, accent: Color) {
    val t = rememberInfiniteTransition(label = "hero")
    val p by t.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(7000, easing = LinearEasing), RepeatMode.Restart),
        label = "p"
    )
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val a = p * 2f * PI.toFloat()

        val sl = w * 0.08f
        val stp = h * 0.03f
        val sw = w * 0.84f
        val sh = h * 0.52f
        drawRoundRect(
            brush = Brush.verticalGradient(listOf(Color(0xFF26305E), Color(0xFF111631))),
            topLeft = Offset(sl, stp),
            size = Size(sw, sh),
            cornerRadius = CornerRadius(h * 0.07f)
        )
        drawRoundRect(
            color = Color(0x55AFC3FF),
            topLeft = Offset(sl, stp),
            size = Size(sw, sh),
            cornerRadius = CornerRadius(h * 0.07f),
            style = Stroke(width = 1.5f.dp.toPx())
        )

        val pl = w * 0.22f
        val pt = h * 0.64f
        val pw = w * 0.56f
        val ph = h * 0.34f
        drawRoundRect(
            brush = Brush.verticalGradient(listOf(Color(0xFF3A4270), Color(0xFF151A30))),
            topLeft = Offset(pl, pt),
            size = Size(pw, ph),
            cornerRadius = CornerRadius(h * 0.08f)
        )
        drawRoundRect(
            color = Color(0x88AFC3FF),
            topLeft = Offset(pl, pt),
            size = Size(pw, ph),
            cornerRadius = CornerRadius(h * 0.08f),
            style = Stroke(width = 2.dp.toPx())
        )

        val fx = pl + pw * (0.5f + 0.34f * cos(a))
        val fy = pt + ph * (0.5f + 0.28f * sin(2f * a))
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(accent.copy(alpha = 0.7f), Color.Transparent),
                center = Offset(fx, fy),
                radius = h * 0.2f
            ),
            radius = h * 0.2f,
            center = Offset(fx, fy)
        )

        val cx = sl + sw * (0.5f + 0.4f * cos(a))
        val cy = stp + sh * (0.5f + 0.34f * sin(2f * a))
        val pulse = (p * 3f) % 1f
        drawCircle(
            color = accent.copy(alpha = 0.6f * (1f - pulse)),
            radius = 6.dp.toPx() + pulse * 22.dp.toPx(),
            center = Offset(cx, cy),
            style = Stroke(width = 2.dp.toPx())
        )
        drawIntoCanvas { c ->
            CursorPainter.draw(c.nativeCanvas, 0, false, true, 22.dp.toPx(), cx, cy)
        }
    }
}

// ======================= خوش‌آمد =======================

@Composable
fun Welcome(onLang: (String) -> Unit, onDone: () -> Unit) {
    val ctx = LocalContext.current
    val lang = LocalLang.current
    var page by remember { mutableIntStateOf(0) }
    val failMsg = tr("باز نشد", "Could not open")

    Column(
        Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LangSwitch(lang, onLang)
            Text(
                tr("رد کردن", "Skip"),
                color = Muted,
                modifier = Modifier.clickable { onDone() }.padding(8.dp)
            )
        }
        Box(
            Modifier.weight(1f).fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Crossfade(targetState = page, label = "welcome") { pg ->
                Column(
                    Modifier.verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    when (pg) {
                        0 -> {
                            HeroPad(Modifier.fillMaxWidth().height(200.dp), Indigo)
                            WTitle(tr("به نشانگر موس خوش آمدی", "Welcome to Touchpad Mouse"))
                            WBody(
                                tr(
                                    "گوشی‌ات را مثل لپ‌تاپ با یک نشانگر واقعی و تاچ‌پد کنترل کن.",
                                    "Control your phone like a laptop, with a real cursor and a touchpad."
                                ),
                                TextAlign.Center
                            )
                        }
                        1 -> {
                            WTitle(tr("حرکات تاچ‌پد", "Touchpad gestures"))
                            WBody(
                                tr(
                                    "☝️ یک انگشت: حرکت نشانگر\n" +
                                        "👆 تپ: کلیک\n" +
                                        "🫳 دو تپ و کشیدن: درگ\n" +
                                        "✌️ دو انگشت: اسکرول و زوم\n" +
                                        "🖱️ دکمه‌ی کلیک راست: پایین تاچ‌پد",
                                    "☝️ One finger: move the cursor\n" +
                                        "👆 Tap: click\n" +
                                        "🫳 Double-tap & drag: drag\n" +
                                        "✌️ Two fingers: scroll and zoom\n" +
                                        "🖱️ Right-click button: bottom of the pad"
                                ),
                                TextAlign.Start
                            )
                        }
                        2 -> {
                            WTitle(tr("چرا دسترسی‌پذیری؟", "Why Accessibility?"))
                            WBody(
                                tr(
                                    "اندازه‌ی نشانگر روی همه‌ی برنامه‌ها و ارسال کلیک، اسکرول و زوم به‌جای انگشت تو، فقط از راه «دسترسی‌پذیری» ممکن است.\n\n" +
                                        "🔒 برنامه اینترنت ندارد و هیچ اطلاعاتی ارسال یا ذخیره نمی‌کند.\n" +
                                        "🔒 محتوای صفحه و چیزهایی که تایپ می‌کنی را نمی‌خواند.\n" +
                                        "🔒 فقط نشانگر را نشان می‌دهد و حرکاتی را اجرا می‌کند که خودت روی تاچ‌پد می‌زنی.",
                                    "Showing a cursor over every app and sending clicks, scrolls and zoom for you is only possible through Android's Accessibility service.\n\n" +
                                        "🔒 The app has no internet access and never sends or stores your data.\n" +
                                        "🔒 It does not read the screen or what you type.\n" +
                                        "🔒 It only shows the cursor and performs the gestures you make on the touchpad."
                                ),
                                TextAlign.Start
                            )
                        }
                        else -> {
                            WTitle(tr("یک قدم مانده", "One last step"))
                            WBody(
                                tr(
                                    "سرویس «نشانگر موس» را در تنظیمات دسترسی‌پذیری روشن کن.\n\n" +
                                        "اگر گزینه خاکستری بود (اندروید ۱۳ به بالا): تنظیمات گوشی ← برنامه‌ها ← نشانگر موس ← سه‌نقطه‌ی بالا ← «اجازه‌ی تنظیمات محدودشده».",
                                    "Turn on the «Touchpad Mouse» service in Accessibility settings.\n\n" +
                                        "If it is greyed out (Android 13+): Phone settings ← Apps ← this app ← ⋮ menu ← «Allow restricted settings»."
                                ),
                                TextAlign.Start
                            )
                            Button(
                                onClick = {
                                    safeStart(
                                        ctx,
                                        Intent(AndroidSettings.ACTION_ACCESSIBILITY_SETTINGS),
                                        failMsg
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Orange,
                                    contentColor = Color(0xFF1B1200)
                                )
                            ) {
                                Text(
                                    tr("باز کردن تنظیمات دسترسی‌پذیری", "Open Accessibility settings"),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 0..3) {
                Box(
                    Modifier
                        .height(8.dp)
                        .width(if (i == page) 26.dp else 8.dp)
                        .clip(CircleShape)
                        .background(if (i == page) Indigo else Color(0x44FFFFFF))
                )
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(top = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (page > 0) {
                Button(
                    onClick = { page -= 1 },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0x22FFFFFF),
                        contentColor = Color.White
                    )
                ) { Text(tr("قبلی", "Back")) }
            }
            Button(
                onClick = { if (page < 3) page += 1 else onDone() },
                modifier = Modifier.weight(if (page > 0) 2f else 1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Indigo,
                    contentColor = Color(0xFF0B1020)
                )
            ) {
                Text(
                    if (page < 3) tr("بعدی", "Next") else tr("شروع", "Start"),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun WTitle(text: String) {
    Text(
        text,
        color = Color.White,
        fontSize = 26.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center
    )
}

@Composable
fun WBody(text: String, align: TextAlign) {
    Text(
        text,
        color = Muted,
        fontSize = 15.sp,
        lineHeight = 26.sp,
        textAlign = align,
        modifier = Modifier.fillMaxWidth()
    )
}

// ======================= تنظیمات =======================

@Composable
fun HomeTab(p: Prefs, tick: Int, onLang: (String) -> Unit) {
    val ctx = LocalContext.current
    val lang = LocalLang.current
    val active = remember(tick) { MouseService.instance != null }
    val failMsg = tr("باز نشد", "Could not open")

    var enabled by remember(tick) { mutableStateOf(p.enabled) }
    var rec by remember(tick) { mutableStateOf(p.recordMode) }
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
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    tr("نشانگر موس", "Touchpad Mouse"),
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    tr("موس و تاچ‌پد لپ‌تاپی برای گوشی", "Laptop-style mouse & touchpad for your phone"),
                    color = Muted,
                    fontSize = 13.sp
                )
            }
            LangSwitch(lang, onLang)
        }

        // ---------- کارت اصلی ----------
        Column(
            Modifier.fillMaxWidth().glass(32.dp, Color(0x1FFFFFFF)).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            HeroPad(Modifier.fillMaxWidth().height(170.dp), Indigo)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ToggleTile(
                    "🟢", tr("نشانگر", "Pointer"),
                    if (enabled) tr("روشن", "On") else tr("خاموش", "Off"),
                    enabled, Green, Modifier.weight(1f)
                ) {
                    enabled = !enabled
                    p.enabled = enabled
                }
                ToggleTile(
                    "🎥", tr("حالت ضبط", "Record mode"),
                    if (rec) tr("تاچ‌پد نامرئی", "Pad hidden") else tr("خاموش", "Off"),
                    rec, Pink, Modifier.weight(1f)
                ) {
                    rec = !rec
                    p.recordMode = rec
                }
            }
            Text(
                when {
                    !active -> tr("سرویس دسترسی‌پذیری هنوز فعال نیست", "Accessibility service is not enabled yet")
                    enabled -> tr("نشانگر و تاچ‌پد روشن است", "Pointer and touchpad are on")
                    else -> tr("خاموش است", "Turned off")
                },
                color = if (active) Color(0xFFB9F6CA) else Color(0xFFFFD180),
                fontSize = 13.sp
            )
        }

        if (!active) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .glass(26.dp, Color(0x22FFB86B))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    tr("⚠️ سرویس دسترسی‌پذیری فعال نیست", "⚠️ Accessibility service is off"),
                    color = Orange,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    tr(
                        "یک‌بار باید سرویس «نشانگر موس» را در تنظیمات دسترسی‌پذیری روشن کنی. اگر گزینه خاکستری بود: تنظیمات ← برنامه‌ها ← نشانگر موس ← سه‌نقطه‌ی بالا ← «اجازه‌ی تنظیمات محدودشده».",
                        "Turn on the «Touchpad Mouse» service once in Accessibility settings. If it is greyed out: Settings ← Apps ← this app ← ⋮ ← «Allow restricted settings»."
                    ),
                    color = Muted, fontSize = 13.sp, lineHeight = 20.sp
                )
                Button(
                    onClick = {
                        safeStart(ctx, Intent(AndroidSettings.ACTION_ACCESSIBILITY_SETTINGS), failMsg)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Orange,
                        contentColor = Color(0xFF1B1200)
                    )
                ) {
                    Text(
                        tr("باز کردن تنظیمات دسترسی‌پذیری", "Open Accessibility settings"),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // ---------- نشانگر ----------
        Section(tr("نشانگر", "Cursor"), "🖱️", Indigo) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .clip(RoundedCornerShape(18.dp))
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
                                .clip(RoundedCornerShape(16.dp))
                                .background(Brush.linearGradient(listOf(Color(0xFF8D96AB), Color(0xFF5D667C))))
                                .border(
                                    if (sel) 3.dp else 1.dp,
                                    if (sel) Indigo else Color(0x33FFFFFF),
                                    RoundedCornerShape(16.dp)
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
                ColorChip(tr("سفید", "White"), !black, Color.White, Color.Black, Modifier.weight(1f)) {
                    black = false; p.black = false
                }
                ColorChip(tr("مشکی", "Black"), black, Color.Black, Color.White, Modifier.weight(1f)) {
                    black = true; p.black = true
                }
            }
            SliderRow(tr("اندازه‌ی نشانگر", "Cursor size"), "${curSize.roundToInt()}", curSize, 14f..64f, Indigo) {
                curSize = it; p.sizeDp = it.roundToInt()
            }
            SliderRow(tr("شفافیت نشانگر", "Cursor opacity"), "${curAlpha.roundToInt()}%", curAlpha, 20f..100f, Indigo) {
                curAlpha = it; p.cursorAlpha = it.roundToInt()
            }
            SwitchRow(
                tr("سایه‌ی نشانگر", "Cursor shadow"),
                tr("برای دیده شدن روی زمینه‌های روشن و تیره", "Easier to see on light and dark backgrounds"),
                shadow, Indigo
            ) { shadow = it; p.shadow = it }
            Text(tr("انیمیشن کلیک", "Click animation"), color = Color.White, fontSize = 15.sp)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Chip(tr("خاموش", "Off"), anim == 0, Indigo, Modifier.weight(1f)) { anim = 0; p.clickAnim = 0 }
                Chip(tr("حلقه", "Ring"), anim == 1, Indigo, Modifier.weight(1f)) { anim = 1; p.clickAnim = 1 }
                Chip(tr("ضربه", "Pulse"), anim == 2, Indigo, Modifier.weight(1f)) { anim = 2; p.clickAnim = 2 }
            }
            Text(tr("رنگ انیمیشن", "Animation color"), color = Color.White, fontSize = 15.sp)
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
        Section(tr("حرکت و کلیک", "Movement & clicks"), "👆", Green) {
            SliderRow(tr("سرعت نشانگر", "Cursor speed"), String.format("%.1f", speed), speed, 0.5f..6f, Green) {
                speed = it; p.speed = it
            }
            SwitchRow(
                tr("شتاب نشانگر", "Pointer acceleration"),
                tr("حرکت آهسته دقیق، حرکت تند دور", "Slow = precise, fast = far"),
                accel, Green
            ) { accel = it; p.accel = it }
            SwitchRow(tr("لرزش موقع کلیک", "Vibrate on click"), null, haptic, Green) {
                haptic = it; p.haptic = it
            }
            SwitchRow(
                tr("دکمه‌ی کلیک راست", "Right-click button"),
                tr("پایین سمت راست تاچ‌پد (لمس طولانی)", "Bottom-right of the pad (long press)"),
                rightBtn, Green
            ) { rightBtn = it; p.rightBtn = it }
            SwitchRow(
                tr("سوایپ از لبه‌ها", "Edge swipes"),
                tr(
                    "چپ/راست: بازگشت — پایین: هوم — پایین و نگه‌داشتن: برنامه‌های اخیر",
                    "Left/right: back — bottom: home — bottom & hold: recents"
                ),
                edge, Green
            ) { edge = it; p.edge = it }
            SliderRow(tr("تأخیر قبل از درگ", "Drag hold delay"), "${dragHold.roundToInt()} ms", dragHold, 0f..1000f, Green) {
                dragHold = it; p.dragHold = it.roundToInt()
            }
        }

        // ---------- اسکرول و زوم ----------
        Section(tr("اسکرول و زوم", "Scroll & zoom"), "📜", Orange) {
            SwitchRow(
                tr("جهت اسکرول برعکس", "Reverse scroll direction"),
                tr("خاموش: مثل گوشی — روشن: مثل ویندوز", "Off: phone style — On: Windows style"),
                scrollRev, Orange
            ) { scrollRev = it; p.scrollReverse = it }
            SliderRow(tr("حساسیت اسکرول", "Scroll sensitivity"), String.format("%.1f", scrollSens), scrollSens, 0.4f..3f, Orange) {
                scrollSens = it; p.scrollSens = it
            }
            Text(
                tr(
                    "اسکرول و زوم بعد از برداشتن انگشت‌ها از تاچ‌پد اجرا می‌شوند.",
                    "Scroll and zoom run after you lift your fingers from the pad."
                ),
                color = Muted, fontSize = 12.sp, lineHeight = 18.sp
            )
        }

        // ---------- تاچ‌پد ----------
        Section(tr("تاچ‌پد", "Touchpad"), "🎛️", Violet) {
            SliderRow(tr("شفافیت تاچ‌پد", "Pad opacity"), "${padAlpha.roundToInt()}%", padAlpha, 0f..100f, Violet) {
                padAlpha = it; p.padAlpha = it.roundToInt()
            }
            if (padAlpha < 4f) {
                Text(
                    tr("تاچ‌پد نامرئی است ولی همچنان کار می‌کند.", "The pad is invisible but still works."),
                    color = Violet, fontSize = 12.sp
                )
            }
            SliderRow(tr("اندازه‌ی تاچ‌پد", "Pad size"), "${padWidth.roundToInt()}", padWidth, 160f..380f, Violet) {
                padWidth = it; p.padWidthDp = it.roundToInt()
            }
            Text(tr("رنگ تاچ‌پد", "Pad color"), color = Color.White, fontSize = 15.sp)
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
                            Text(
                                if (lang == "en") THEME_EN[i] else t.name,
                                color = if (sel) Color.White else Muted,
                                fontSize = 11.sp
                            )
                        }
                    }
                    for (k in rowItems.size until 5) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
            SwitchRow(
                tr("قفل جای تاچ‌پد", "Lock pad position"),
                tr("با کشیدن اتفاقی جابه‌جا نشود", "Prevents accidental moving"),
                locked, Violet
            ) { locked = it; p.padLocked = it }
        }

        // ---------- ماندگاری ----------
        Section(tr("ماندگاری و میانبر", "Keep-alive & shortcuts"), "🔋", Cyan) {
            SwitchRow(
                tr("اعلان ثابت", "Persistent notification"),
                tr("برنامه را زنده نگه می‌دارد و دکمه‌ی روشن/خاموش دارد", "Helps keep the app alive and has an on/off button"),
                notif, Cyan
            ) { notif = it; p.notif = it }
            Text(
                tr(
                    "برای اینکه با بستن برنامه نشانگر خاموش نشود، برنامه را در لیست برنامه‌های اخیر قفل کن و این دو را بزن. جزئیات در بخش آموزش.",
                    "To keep the pointer alive after closing the app, lock the app in recents and use these two. Details in the Guide."
                ),
                color = Muted, fontSize = 13.sp, lineHeight = 20.sp
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = {
                        safeStart(ctx, Intent(AndroidSettings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS), failMsg)
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Cyan, contentColor = Color(0xFF00232B))
                ) { Text(tr("تنظیمات باتری", "Battery"), fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                Button(
                    onClick = {
                        safeStart(
                            ctx,
                            Intent(
                                AndroidSettings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.parse("package:" + ctx.packageName)
                            ),
                            failMsg
                        )
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Cyan, contentColor = Color(0xFF00232B))
                ) { Text(tr("اطلاعات برنامه", "App info"), fontSize = 13.sp, fontWeight = FontWeight.Bold) }
            }
        }

        Button(
            onClick = { showDialog = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7F1D3A), contentColor = Color.White)
        ) { Text(tr("بازیابی تنظیمات", "Reset settings"), fontWeight = FontWeight.Bold) }
        Box(Modifier.height(8.dp))
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(tr("بازیابی تنظیمات", "Reset settings")) },
            text = {
                Text(
                    tr(
                        "همه‌ی تنظیمات و جای تاچ‌پد به حالت اولیه برمی‌گردد.",
                        "All settings and the pad position will return to defaults."
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    p.reset()
                    enabled = p.enabled
                    rec = p.recordMode
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
                }) { Text(tr("بازیابی", "Reset")) }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text(tr("لغو", "Cancel")) }
            }
        )
    }
}

// ======================= آموزش =======================

private class Tut(val e: String, val tf: String, val te: String, val bf: String, val be: String)

private val TUTORIALS = listOf(
    Tut(
        "🚀", "شروع سریع", "Quick start",
        "۱) برنامه را باز کن و صفحه‌ی خوش‌آمد را ببین.\n" +
            "۲) در تنظیمات دسترسی‌پذیری گوشی، سرویس «نشانگر موس» را روشن کن (دکمه‌اش در برنامه هست).\n" +
            "۳) اگر گزینه خاکستری بود (اندروید ۱۳ به بالا): تنظیمات گوشی ← برنامه‌ها ← نشانگر موس ← سه‌نقطه‌ی بالا ← «اجازه‌ی تنظیمات محدودشده»، بعد دوباره روشنش کن.\n" +
            "۴) به برنامه برگرد؛ نشانگر و تاچ‌پد روی صفحه ظاهر می‌شوند.\n\n" +
            "این کار فقط یک‌بار لازم است. اگر نسخه‌ی جدید با امضای دیگری نصب می‌کنی، اول نسخه‌ی قبلی را پاک کن.",
        "1) Open the app and go through the welcome screens.\n" +
            "2) In your phone's Accessibility settings, turn on the «Touchpad Mouse» service (the app has a button for it).\n" +
            "3) If it is greyed out (Android 13+): Phone settings ← Apps ← this app ← ⋮ menu ← «Allow restricted settings», then turn it on again.\n" +
            "4) Go back to the app; the cursor and touchpad appear on screen.\n\n" +
            "You only do this once. If you install a build signed with a different key, uninstall the old one first."
    ),
    Tut(
        "👆", "حرکات تاچ‌پد", "Touchpad gestures",
        "• یک انگشت: حرکت نشانگر\n" +
            "• تپ: کلیک\n" +
            "• دو تپ و بعد کشیدن (تپ دوم را نگه دار و بکش): درگ برای انتخاب متن، جابه‌جایی آیکون و کشیدن اسلایدر\n" +
            "• دو تپ سریع بدون کشیدن: دابل‌کلیک\n" +
            "• تپ دو انگشتی: لمس طولانی\n" +
            "• دکمه‌ی «کلیک راست» پایین سمت راست تاچ‌پد: لمس طولانی. اندروید کلیک راست واقعی ندارد و نزدیک‌ترین معادلش لمس طولانی است.\n" +
            "• کشیدن دو انگشت: اسکرول\n" +
            "• جمع یا باز کردن دو انگشت: زوم",
        "• One finger: move the cursor\n" +
            "• Tap: click\n" +
            "• Double-tap and drag (hold the second tap): drag, to select text, move icons or drag sliders\n" +
            "• Fast double-tap without dragging: double click\n" +
            "• Two-finger tap: long press\n" +
            "• «Right click» button at the pad's bottom-right: long press. Android has no real right-click; long press is the closest equivalent.\n" +
            "• Two-finger drag: scroll\n" +
            "• Pinch or spread two fingers: zoom"
    ),
    Tut(
        "📜", "اسکرول، درگ و زوم", "Scroll, drag & zoom",
        "اندروید در بیشتر گوشی‌ها اجازه نمی‌دهد وقتی انگشتت روی تاچ‌پد است، لمس شبیه‌سازی‌شده هم‌زمان اجرا شود. برای همین اسکرول، درگ و زوم بعد از برداشتن انگشت اجرا می‌شوند.\n\n" +
            "اسکرول: دو انگشت را بکش و رها کن. هرچه بیشتر بکشی، صفحه بیشتر جابه‌جا می‌شود و هرچه تندتر بکشی، سرعتش بیشتر است. با «حساسیت اسکرول» مقدارش را تنظیم کن و با «جهت اسکرول برعکس» جهت را مثل ویندوز کن.\n\n" +
            "درگ: «تأخیر قبل از درگ» مدت نگه‌داشتن اولیه است. برای انتخاب متن و جابه‌جایی آیکون حدود ۴۵۰ میلی‌ثانیه خوب است. برای اسلایدر می‌توانی کمترش کنی.\n\n" +
            "زوم: دو انگشت را از هم دور (زوم‌این) یا نزدیک (زوم‌اوت) کن و رها کن.",
        "On most phones Android does not allow a simulated touch while your real finger is on the pad. So scroll, drag and zoom run after you lift your fingers.\n\n" +
            "Scroll: drag two fingers and release. The farther and faster you drag, the more the page moves. Adjust it with «Scroll sensitivity», and flip the direction (Windows style) with «Reverse scroll direction».\n\n" +
            "Drag: «Drag hold delay» is the initial hold time. About 450 ms works for selecting text and moving icons. Lower it for sliders.\n\n" +
            "Zoom: spread two fingers (zoom in) or pinch them (zoom out) and release."
    ),
    Tut(
        "↔️", "سوایپ لبه‌ها", "Edge swipes",
        "اگر روشن باشد:\n" +
            "• شروع از لبه‌ی چپ یا راست تاچ‌پد و کشیدن به داخل: بازگشت\n" +
            "• شروع از لبه‌ی پایین و کشیدن به بالا: هوم\n" +
            "• همان کشیدن به بالا و چند لحظه نگه‌داشتن قبل از برداشتن انگشت: برنامه‌های اخیر\n\n" +
            "نوارهای کم‌رنگ روی تاچ‌پد محل لبه‌ها را نشان می‌دهند. اگر اتفاقی فعال می‌شوند، خاموشش کن.",
        "When enabled:\n" +
            "• Start at the pad's left or right edge and swipe inward: Back\n" +
            "• Start at the bottom edge and swipe up: Home\n" +
            "• Same swipe up, then hold briefly before lifting: Recents\n\n" +
            "Faint bars on the pad show the edges. Turn it off if it triggers by accident."
    ),
    Tut(
        "🧲", "جابه‌جایی و قفل تاچ‌پد", "Moving & locking the pad",
        "نوار باریک بالای تاچ‌پد را بگیر و بکش تا جابه‌جا شود. جای حالت عمودی و افقی جدا ذخیره می‌شود.\n\n" +
            "با «قفل جای تاچ‌پد» دیگر با کشیدن اتفاقی جابه‌جا نمی‌شود. برای جابه‌جایی دوباره، قفل را خاموش کن.\n\n" +
            "اندازه‌ی تاچ‌پد را با اسلایدر «اندازه‌ی تاچ‌پد» تنظیم کن. نشانگر نمی‌تواند از صفحه‌ی گوشی بیرون برود.",
        "Grab the thin bar at the top of the pad and drag to move it. Portrait and landscape positions are saved separately.\n\n" +
            "«Lock pad position» stops accidental moves. Turn it off to move the pad again.\n\n" +
            "Change the size with «Pad size». The cursor can never leave the phone screen."
    ),
    Tut(
        "🖱️", "تنظیمات نشانگر", "Cursor settings",
        "• شکل: ۸ مدل (ویندوز، مک، فلش توخالی، دست، دایره، نقطه، ضربدر، هدف)\n" +
            "• رنگ: سفید یا مشکی\n" +
            "• اندازه و شفافیت: با اسلایدر\n" +
            "• سایه: دیده شدن روی زمینه‌های مختلف\n" +
            "• انیمیشن کلیک: خاموش، حلقه یا ضربه، با ۶ رنگ دلخواه\n" +
            "• سرعت: سرعت پایه‌ی حرکت\n" +
            "• شتاب: حرکت آهسته دقیق و حرکت تند دور، مثل لپ‌تاپ",
        "• Shape: 8 styles (Windows, Mac, hollow arrow, hand, circle, dot, cross, target)\n" +
            "• Color: white or black\n" +
            "• Size and opacity: sliders\n" +
            "• Shadow: easier to see on any background\n" +
            "• Click animation: off, ring or pulse, in 6 colors\n" +
            "• Speed: base movement speed\n" +
            "• Acceleration: slow is precise, fast goes far, like a laptop"
    ),
    Tut(
        "🎨", "شفافیت و رنگ تاچ‌پد", "Pad opacity & colors",
        "شفافیت را تا ۰٪ می‌توانی پایین بیاوری. در این حالت تاچ‌پد نامرئی است ولی همچنان کار می‌کند. اگر جایش را فراموش کردی، شفافیت را از برنامه بالا ببر.\n\n" +
            "۹ رنگ‌بندی آماده داری: نیمه‌شب، گرافیت، زمرد، یاقوت، بنفش، کهربا، اقیانوس، صورتی و شیشه‌ای.",
        "You can lower the opacity to 0%. The pad is then invisible but still works. If you forget where it is, raise the opacity in the app.\n\n" +
            "There are 9 color themes: Midnight, Graphite, Emerald, Ruby, Violet, Amber, Ocean, Pink and Glass."
    ),
    Tut(
        "🎥", "ضبط صفحه و حالت ضبط", "Screen recording & Record mode",
        "برای فیلم‌برداری «حالت ضبط» را روشن کن (از برنامه یا کاشی سریع «حالت ضبط»). تاچ‌پد نامرئی می‌شود ولی همچنان کار می‌کند و فقط نشانگر در فیلم دیده می‌شود. بعد از ضبط خاموشش کن تا تاچ‌پد برگردد.\n\n" +
            "اگر جای تاچ‌پد را فراموش کردی، حالت ضبط را از برنامه خاموش کن.",
        "Before recording, turn on «Record mode» (in the app or with the «Record mode» quick tile). The pad becomes invisible but keeps working, and only the cursor appears in the video. Turn it off afterwards to bring the pad back.\n\n" +
            "If you forget where the pad is, turn off Record mode from the app."
    ),
    Tut(
        "⚡", "روشن و خاموش سریع", "Quick toggles",
        "• کاشی‌های سریع: پرده‌ی تنظیمات سریع را باز کن، دکمه‌ی ویرایش (مداد) را بزن و کاشی‌های «نشانگر موس» و «حالت ضبط» را به بالا بکش.\n" +
            "• اعلان ثابت: وقتی روشن باشد، توی اعلان‌ها دکمه‌ی روشن/خاموش داری.\n" +
            "• داخل برنامه: کاشی‌های بزرگ بالای صفحه‌ی تنظیمات.",
        "• Quick tiles: open Quick Settings, tap Edit (pencil) and drag the «Touchpad Mouse» and «Record mode» tiles up.\n" +
            "• Persistent notification: when enabled, it has an on/off button.\n" +
            "• In the app: the big tiles at the top of Settings."
    ),
    Tut(
        "🔋", "خاموش نشدن با بستن برنامه", "Keeping it running",
        "بستن ساده‌ی برنامه مشکلی ایجاد نمی‌کند. ولی ضربدر (پاک‌سازی همه) در لیست برنامه‌های اخیر بعضی گوشی‌ها (مثل شیائومی) برنامه را کامل می‌بندد و اندروید سرویس دسترسی‌پذیری را هم خاموش می‌کند. هیچ برنامه‌ای نمی‌تواند خودش را دوباره روشن کند. برای جلوگیری:\n\n" +
            "۱) برنامه را در لیست برنامه‌های اخیر قفل کن (انگشت را روی کارت برنامه نگه دار ← Lock) تا ضربدر آن را نبندد.\n" +
            "۲) اعلان ثابت را روشن نگه دار.\n" +
            "۳) در تنظیمات باتری برای این برنامه «بدون محدودیت» را انتخاب کن (دکمه‌ی «تنظیمات باتری» در بخش ماندگاری).\n" +
            "۴) در شیائومی: «شروع خودکار» (Autostart) را روشن کن.",
        "Simply swiping the app away is fine. But on some phones (like Xiaomi) the X (clear all) button in recents kills the app completely and Android turns the accessibility service off too. No app can turn itself back on. To prevent this:\n\n" +
            "1) Lock the app in recents (long-press its card ← Lock) so the X does not close it.\n" +
            "2) Keep the persistent notification on.\n" +
            "3) In battery settings set this app to «No restrictions» (the «Battery» button in Keep-alive).\n" +
            "4) On Xiaomi: enable «Autostart» for the app."
    ),
    Tut(
        "🔒", "حریم خصوصی و دسترسی", "Privacy & permission",
        "برنامه به اینترنت دسترسی ندارد و هیچ اطلاعاتی ارسال یا ذخیره نمی‌کند. محتوای صفحه و چیزهایی که تایپ می‌کنی را نمی‌خواند.\n\n" +
            "دسترسی‌پذیری فقط برای دو کار لازم است: کشیدن نشانگر روی همه‌ی برنامه‌ها و اجرای کلیک، اسکرول، درگ و زوم به‌جای انگشت تو.",
        "The app has no internet access and never sends or stores your data. It does not read the screen or what you type.\n\n" +
            "Accessibility is needed for two things only: drawing the cursor over every app, and performing clicks, scrolls, drags and zoom for you."
    ),
    Tut(
        "🛠️", "رفع مشکل", "Troubleshooting",
        "• نشانگر نمی‌آید: سرویس دسترسی‌پذیری و کلید روشن/خاموش را بررسی کن.\n" +
            "• کلیک کار نمی‌کند: سرویس را یک‌بار از تنظیمات دسترسی‌پذیری خاموش و روشن کن.\n" +
            "• درگ کار نکرد: «تأخیر قبل از درگ» را کم یا زیاد کن.\n" +
            "• اسکرول کم یا زیاد است: «حساسیت اسکرول» را تنظیم کن.\n" +
            "• اسکرول و درگ زیر خود تاچ‌پد: تاچ‌پد را کمی جابه‌جا کن.\n" +
            "• بعضی برنامه‌ها (مثل بانک‌ها) لمس شبیه‌سازی‌شده را مسدود می‌کنند.\n" +
            "• اگر چیزی درست نشد، از بخش «ارتباط با ما» پیام بده.",
        "• No cursor: check the accessibility service and the on/off switch.\n" +
            "• Clicks don't work: turn the service off and on in Accessibility settings.\n" +
            "• Drag fails: raise or lower «Drag hold delay».\n" +
            "• Scroll too small or too big: adjust «Scroll sensitivity».\n" +
            "• Scroll and drag under the pad itself: move the pad a bit.\n" +
            "• Some apps (like banking apps) block simulated touches.\n" +
            "• Still stuck? Message us from the Contact tab."
    )
)

@Composable
fun TutorialTab() {
    val en = LocalLang.current == "en"
    val accents = listOf(Indigo, Green, Orange, Violet, Cyan, Pink, Red)
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            tr("📖 آموزش کامل", "📖 Complete guide"),
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            tr("روی هر بخش بزن تا باز شود", "Tap a section to open it"),
            color = Muted,
            fontSize = 13.sp
        )
        TUTORIALS.forEachIndexed { i, e ->
            Accordion(
                e.e,
                if (en) e.te else e.tf,
                if (en) e.be else e.bf,
                accents[i % accents.size],
                i == 0
            )
        }
        Box(Modifier.height(8.dp))
    }
}

@Composable
fun Accordion(emoji: String, title: String, body: String, accent: Color, startOpen: Boolean) {
    var open by remember { mutableStateOf(startOpen) }
    Column(Modifier.fillMaxWidth().glass(22.dp)) {
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
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(accent.copy(alpha = 0.55f), accent.copy(alpha = 0.18f))
                        )
                    ),
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
    val ctx = LocalContext.current
    val copied = tr("کپی شد", "Copied")
    val failed = tr("باز نشد", "Could not open")
    val subject = tr("پیام درباره‌ی نشانگر موس", "About Touchpad Mouse")
    val copyLabel = tr("کپی", "Copy")
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            tr("💬 ارتباط با ما", "💬 Contact us"),
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            tr(
                "نظر، پیشنهاد یا گزارش مشکل داری؟ از روبیکا یا ایمیل پیام بده.",
                "Have feedback, ideas or a bug to report? Message us on Rubika or by email."
            ),
            color = Muted,
            fontSize = 14.sp,
            lineHeight = 22.sp
        )
        ContactCard(
            "💬", "Rubika", "@Amirali_asli_1", Pink, copyLabel, tr("باز کردن", "Open"),
            { copyText(ctx, "@Amirali_asli_1", copied) },
            { safeStart(ctx, Intent(Intent.ACTION_VIEW, Uri.parse("https://rubika.ir/Amirali_asli_1")), failed) }
        )
        ContactCard(
            "💬", "Rubika", "@BaLe_KaBiR", Violet, copyLabel, tr("باز کردن", "Open"),
            { copyText(ctx, "@BaLe_KaBiR", copied) },
            { safeStart(ctx, Intent(Intent.ACTION_VIEW, Uri.parse("https://rubika.ir/BaLe_KaBiR")), failed) }
        )
        ContactCard(
            "✉️", tr("ایمیل", "Email"), "ameraliking2@gmail.com", Cyan, copyLabel, tr("ارسال ایمیل", "Send email"),
            { copyText(ctx, "ameraliking2@gmail.com", copied) },
            {
                safeStart(
                    ctx,
                    Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:ameraliking2@gmail.com"))
                        .putExtra(Intent.EXTRA_SUBJECT, subject),
                    failed
                )
            }
        )
        Section(tr("نکته", "Tip"), "💡", Orange) {
            Text(
                tr(
                    "اگر دکمه‌ی «باز کردن» کار نکرد، آیدی را کپی کن و داخل روبیکا جستجو کن. هنگام گزارش مشکل، مدل گوشی، نسخه‌ی اندروید و توضیح کوتاهی از اتفاقی که افتاد را بنویس.",
                    "If «Open» does not work, copy the ID and search for it in Rubika. When reporting a problem, include your phone model, Android version and a short description of what happened."
                ),
                color = Muted, fontSize = 14.sp, lineHeight = 23.sp
            )
        }
        Text(
            tr("نشانگر موس — نسخه‌ی ۲٫۰", "Touchpad Mouse — version 2.0"),
            color = Muted,
            fontSize = 12.sp
        )
        Box(Modifier.height(8.dp))
    }
}

@Composable
fun ContactCard(
    emoji: String,
    label: String,
    value: String,
    accent: Color,
    copyLabel: String,
    openLabel: String,
    onCopy: () -> Unit,
    onOpen: () -> Unit
) {
    Column(
        Modifier.fillMaxWidth().glass(26.dp).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(accent.copy(alpha = 0.6f), accent.copy(alpha = 0.2f))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) { Text(emoji, fontSize = 22.sp) }
            Column {
                Text(label, color = Muted, fontSize = 12.sp)
                Text(value, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = onCopy,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0x33FFFFFF),
                    contentColor = Color.White
                )
            ) { Text(copyLabel) }
            Button(
                onClick = onOpen,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accent,
                    contentColor = Color(0xFF10131C)
                )
            ) { Text(openLabel, fontWeight = FontWeight.Bold) }
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
fun ToggleTile(
    emoji: String,
    title: String,
    sub: String,
    checked: Boolean,
    accent: Color,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val bg by animateColorAsState(
        if (checked) accent.copy(alpha = 0.30f) else Color(0x14FFFFFF),
        label = "tile"
    )
    Column(
        modifier
            .clip(RoundedCornerShape(24.dp))
            .background(bg)
            .border(
                1.dp,
                if (checked) accent.copy(alpha = 0.85f) else Color(0x33FFFFFF),
                RoundedCornerShape(24.dp)
            )
            .clickable { onClick() }
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(emoji, fontSize = 26.sp)
        Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Text(sub, color = if (checked) Color.White else Muted, fontSize = 12.sp)
    }
}

@Composable
fun Section(title: String, emoji: String, accent: Color, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().glass(26.dp).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(accent.copy(alpha = 0.55f), accent.copy(alpha = 0.18f))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) { Text(emoji, fontSize = 18.sp) }
            Text(title, color = accent, fontWeight = FontWeight.Bold, fontSize = 17.sp)
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
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) accent.copy(alpha = 0.28f) else Color(0x14FFFFFF))
            .border(
                if (selected) 2.dp else 1.dp,
                if (selected) accent else Color(0x33FFFFFF),
                RoundedCornerShape(16.dp)
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
            .clip(RoundedCornerShape(16.dp))
            .background(swatch)
            .border(
                if (selected) 3.dp else 1.dp,
                if (selected) Indigo else Color(0x55FFFFFF),
                RoundedCornerShape(16.dp)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = textColor, fontWeight = FontWeight.Bold)
    }
}
