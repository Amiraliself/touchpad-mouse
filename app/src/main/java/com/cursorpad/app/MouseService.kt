package com.cursorpad.app

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.Path
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.WindowManager
import android.view.WindowManager.LayoutParams as LP
import android.view.accessibility.AccessibilityEvent
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

class MouseService : AccessibilityService(), PadListener {

    companion object {
        @Volatile
        var instance: MouseService? = null

        private const val ACTION_TOGGLE = "com.cursorpad.app.TOGGLE"
        private const val CHANNEL = "cursorpad_channel"
        private const val NOTIF_ID = 7
    }

    private class Job(
        val g: GestureDescription,
        val minX: Float,
        val minY: Float,
        val maxX: Float,
        val maxY: Float,
        val durMs: Long,
        val next: Job? = null
    )

    private lateinit var st: Prefs
    private lateinit var wm: WindowManager
    private val handler = Handler(Looper.getMainLooper())
    private val prefListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> reload() }

    private val toggleReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            st.enabled = !st.enabled
        }
    }

    private var cursor: CursorView? = null
    private var pad: TouchpadView? = null
    private var padLp: LP? = null
    private var shown = false

    private var screenW = 1080
    private var screenH = 1920
    private var curX = 0f
    private var curY = 0f

    private val queue = ArrayDeque<Job>()
    private var running = false

    private var dragging = false
    private var dragMoved = false
    private var dragT0 = 0L
    private var dragStartX = 0f
    private var dragStartY = 0f
    private val dragPts = ArrayList<Float>()

    private var padStartX = 0
    private var padStartY = 0
    private var notifKey = -1

    // ================= چرخه‌ی عمر =================

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        st = Prefs(this)
        wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        st.prefs.registerOnSharedPreferenceChangeListener(prefListener)
        registerToggle()
        readScreen()
        curX = screenW / 2f
        curY = screenH / 2f
        reload()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {}

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        handler.postDelayed({
            if (shown) {
                readScreen()
                updateVisuals()
            }
        }, 200)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        shutdown()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        shutdown()
        super.onDestroy()
    }

    @Suppress("DEPRECATION")
    private fun shutdown() {
        if (::st.isInitialized) {
            st.prefs.unregisterOnSharedPreferenceChangeListener(prefListener)
        }
        runCatching { unregisterReceiver(toggleReceiver) }
        removeOverlays()
        runCatching { stopForeground(true) }
        notifKey = -1
        instance = null
    }

    private fun registerToggle() {
        val f = IntentFilter(ACTION_TOGGLE)
        runCatching {
            if (Build.VERSION.SDK_INT >= 33) {
                registerReceiver(toggleReceiver, f, Context.RECEIVER_NOT_EXPORTED)
            } else {
                registerReceiver(toggleReceiver, f)
            }
        }
    }

    fun reload() {
        handler.post {
            if (!::st.isInitialized) return@post
            if (!st.enabled) {
                removeOverlays()
            } else {
                readScreen()
                if (!shown) addOverlays()
                updateVisuals()
            }
            updateNotification()
        }
    }

    // ================= اعلان ثابت =================

    @Suppress("DEPRECATION")
    private fun updateNotification() {
        val key = (if (st.notif) 2 else 0) + (if (st.enabled) 1 else 0)
        if (key == notifKey) return
        notifKey = key
        if (!st.notif) {
            runCatching { stopForeground(true) }
            return
        }
        runCatching {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL, "نشانگر موس", NotificationManager.IMPORTANCE_LOW)
            )
            val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            val openIntent = Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            val open = PendingIntent.getActivity(this, 0, openIntent, flags)
            val tog = PendingIntent.getBroadcast(
                this, 1, Intent(ACTION_TOGGLE).setPackage(packageName), flags
            )
            val on = st.enabled
            val n = Notification.Builder(this, CHANNEL)
                .setSmallIcon(R.drawable.ic_tile)
                .setContentTitle("نشانگر موس")
                .setContentText(if (on) "روشن است" else "خاموش است")
                .setContentIntent(open)
                .setOngoing(true)
                .addAction(R.drawable.ic_tile, if (on) "خاموش کردن" else "روشن کردن", tog)
                .build()
            val ok = runCatching { startForeground(NOTIF_ID, n) }.isSuccess
            if (!ok) nm.notify(NOTIF_ID, n)
        }
    }

    // ================= پنجره‌ها =================

    @Suppress("DEPRECATION")
    private fun readScreen() {
        if (Build.VERSION.SDK_INT >= 30) {
            val b = wm.currentWindowMetrics.bounds
            screenW = b.width()
            screenH = b.height()
        } else {
            val dm = DisplayMetrics()
            wm.defaultDisplay.getRealMetrics(dm)
            screenW = dm.widthPixels
            screenH = dm.heightPixels
        }
    }

    private fun addOverlays() {
        val dens = resources.displayMetrics.density

        // تاچ‌پد (با FLAG_SECURE تا توی فیلم صفحه ضبط نشود)
        val tv = TouchpadView(this, this)
        val pw = (st.padWidthDp * dens).toInt()
        val plp = LP(
            pw, (pw * 0.62f).toInt(),
            LP.TYPE_ACCESSIBILITY_OVERLAY,
            LP.FLAG_NOT_FOCUSABLE or LP.FLAG_LAYOUT_IN_SCREEN or
                LP.FLAG_LAYOUT_NO_LIMITS or LP.FLAG_SECURE,
            PixelFormat.TRANSLUCENT
        )
        plp.gravity = Gravity.TOP or Gravity.START
        padLp = plp
        pad = tv
        placePad()
        wm.addView(tv, plp)

        // نشانگر (تمام‌صفحه، غیرقابل‌لمس، روی تاچ‌پد)
        val cv = CursorView(this)
        val clp = LP(
            LP.MATCH_PARENT, LP.MATCH_PARENT,
            LP.TYPE_ACCESSIBILITY_OVERLAY,
            LP.FLAG_NOT_FOCUSABLE or LP.FLAG_NOT_TOUCHABLE or
                LP.FLAG_LAYOUT_IN_SCREEN or LP.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )
        if (Build.VERSION.SDK_INT >= 28) {
            clp.layoutInDisplayCutoutMode = LP.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        cursor = cv
        wm.addView(cv, clp)
        shown = true
    }

    private fun removeOverlays() {
        handler.removeCallbacksAndMessages(null)
        queue.clear()
        running = false
        dragging = false
        cursor?.let { v -> runCatching { wm.removeView(v) } }
        pad?.let { v -> runCatching { wm.removeView(v) } }
        cursor = null
        pad = null
        padLp = null
        shown = false
    }

    private fun landscape() = screenW > screenH

    private fun placePad() {
        val lp = padLp ?: return
        val dens = resources.displayMetrics.density
        var x = st.padX(landscape())
        var y = st.padY(landscape())
        if (x < 0 || y < 0) {
            if (landscape()) {
                x = screenW - lp.width - (40 * dens).toInt()
                y = screenH - lp.height - (16 * dens).toInt()
            } else {
                x = (screenW - lp.width) / 2
                y = screenH - lp.height - (110 * dens).toInt()
            }
        }
        lp.x = x.coerceIn(0, max(0, screenW - lp.width))
        lp.y = y.coerceIn(0, max(0, screenH - lp.height))
    }

    private fun updateVisuals() {
        val dens = resources.displayMetrics.density
        cursor?.let {
            it.shape = st.shape
            it.black = st.black
            it.shadow = st.shadow
            it.sizePx = st.sizeDp * dens
            it.animStyle = st.clickAnim
            it.animColor = Themes.accents[st.clickColor.coerceIn(0, Themes.accents.size - 1)]
            it.alpha = st.cursorAlpha / 100f
        }
        val lp = padLp
        val pv = pad
        if (lp != null && pv != null) {
            pv.alpha = st.padAlpha / 100f
            pv.edgeEnabled = st.edge
            pv.locked = st.padLocked
            pv.rightBtn = st.rightBtn
            pv.themeIndex = st.padTheme
            lp.width = (st.padWidthDp * dens).toInt()
            lp.height = (lp.width * 0.62f).toInt()
            placePad()
            runCatching { wm.updateViewLayout(pv, lp) }
            pv.invalidate()
        }
        curX = curX.coerceIn(0f, screenW - 1f)
        curY = curY.coerceIn(0f, screenH - 1f)
        cursor?.moveTo(curX, curY)
    }

    private fun setPadTouchable(touchable: Boolean) {
        val lp = padLp ?: return
        val v = pad ?: return
        val f = if (touchable) lp.flags and LP.FLAG_NOT_TOUCHABLE.inv()
        else lp.flags or LP.FLAG_NOT_TOUCHABLE
        if (f == lp.flags) return
        lp.flags = f
        runCatching { wm.updateViewLayout(v, lp) }
    }

    private fun hitsPad(x0: Float, y0: Float, x1: Float, y1: Float): Boolean {
        val lp = padLp ?: return false
        val m = 8f * resources.displayMetrics.density
        return x1 >= lp.x - m && x0 <= lp.x + lp.width + m &&
            y1 >= lp.y - m && y0 <= lp.y + lp.height + m
    }

    private fun haptic() {
        if (!st.haptic) return
        runCatching {
            @Suppress("DEPRECATION")
            val v = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            v.vibrate(VibrationEffect.createOneShot(14, 110))
        }
    }

    // ================= صف ژست‌ها (بعد از برداشتن انگشت اجرا می‌شوند) =================

    private inner class Runner(val j: Job) {
        var done = false
        val failsafe = Runnable { finish(false) }

        fun finish(ok: Boolean) {
            if (done) return
            done = true
            handler.removeCallbacks(failsafe)
            val n = j.next
            if (ok && n != null) queue.addFirst(n)
            runNext()
        }

        fun start() {
            val ok = dispatchGesture(j.g, object : AccessibilityService.GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    finish(true)
                }

                override fun onCancelled(gestureDescription: GestureDescription?) {
                    finish(false)
                }
            }, handler)
            if (ok) {
                handler.postDelayed(failsafe, j.durMs + 1500L)
            } else {
                finish(false)
            }
        }
    }

    private fun enqueue(j: Job) {
        queue.addLast(j)
        if (!running) runNext()
    }

    private fun runNext() {
        val j = queue.removeFirstOrNull()
        if (j == null) {
            running = false
            setPadTouchable(true)
            return
        }
        running = true
        if (hitsPad(j.minX, j.minY, j.maxX, j.maxY)) {
            setPadTouchable(false)
            handler.postDelayed({ Runner(j).start() }, 50L)
        } else {
            Runner(j).start()
        }
    }

    private fun clickAt(x: Float, y: Float, dur: Long) {
        haptic()
        cursor?.click()
        val p = Path().apply { moveTo(x, y) }
        val g = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(p, 0L, dur))
            .build()
        enqueue(Job(g, x, y, x, y, dur))
    }

    // ================= PadListener =================

    override fun onMove(dx: Float, dy: Float, dtMs: Long) {
        var f = st.speed
        if (st.accel) {
            val v = hypot(dx, dy) / max(1L, dtMs)
            f *= 0.45f + min(v, 3.2f) * 0.8f
        }
        curX = (curX + dx * f).coerceIn(0f, screenW - 1f)
        curY = (curY + dy * f).coerceIn(0f, screenH - 1f)
        cursor?.moveTo(curX, curY)
        if (dragging) recordDrag()
    }

    override fun onTap() {
        clickAt(curX, curY, 45L)
    }

    override fun onTwoFingerTap() {
        clickAt(curX, curY, 650L)
    }

    override fun onRightClick() {
        clickAt(curX, curY, 650L)
    }

    private fun recordDrag() {
        dragMoved = true
        if (dragT0 == 0L) dragT0 = SystemClock.uptimeMillis()
        val n = dragPts.size
        if (n >= 1200) return
        if (n == 0 || hypot(curX - dragPts[n - 2], curY - dragPts[n - 1]) >= 4f) {
            dragPts.add(curX)
            dragPts.add(curY)
        }
    }

    override fun onDragStart() {
        dragging = true
        dragMoved = false
        dragPts.clear()
        dragT0 = 0L
        dragStartX = curX
        dragStartY = curY
    }

    override fun onDragEnd() {
        if (!dragging) return
        dragging = false
        val straight = hypot(curX - dragStartX, curY - dragStartY)
        if (!dragMoved || (straight < 8f && dragPts.size < 6)) {
            clickAt(curX, curY, 45L)
            return
        }
        val path = Path()
        path.moveTo(dragStartX, dragStartY)
        var minX = min(dragStartX, curX)
        var maxX = max(dragStartX, curX)
        var minY = min(dragStartY, curY)
        var maxY = max(dragStartY, curY)
        var i = 0
        while (i + 1 < dragPts.size) {
            val px = dragPts[i]
            val py = dragPts[i + 1]
            path.lineTo(px, py)
            minX = min(minX, px)
            maxX = max(maxX, px)
            minY = min(minY, py)
            maxY = max(maxY, py)
            i += 2
        }
        path.lineTo(curX, curY)
        val dur = (SystemClock.uptimeMillis() - dragT0).coerceIn(200L, 6000L)
        haptic()
        val hold = st.dragHold.toLong()
        if (hold > 0L) {
            val a = GestureDescription.StrokeDescription(
                Path().apply { moveTo(dragStartX, dragStartY) }, 0L, hold, true
            )
            val b = a.continueStroke(path, 0L, dur, false)
            val jb = Job(
                GestureDescription.Builder().addStroke(b).build(),
                minX, minY, maxX, maxY, dur
            )
            val ja = Job(
                GestureDescription.Builder().addStroke(a).build(),
                dragStartX, dragStartY, dragStartX, dragStartY, hold, jb
            )
            enqueue(ja)
        } else {
            val s = GestureDescription.StrokeDescription(path, 0L, dur)
            enqueue(
                Job(GestureDescription.Builder().addStroke(s).build(), minX, minY, maxX, maxY, dur)
            )
        }
    }

    override fun onScrollGesture(dx: Float, dy: Float, durMs: Long) {
        val k = 2.4f * st.scrollSens * (if (st.scrollReverse) -1f else 1f)
        var vx = dx * k
        var vy = dy * k
        val len = hypot(vx, vy)
        if (len < 30f) return
        val maxLen = min(screenW, screenH) * 0.8f
        if (len > maxLen) {
            vx *= maxLen / len
            vy *= maxLen / len
        }
        var x0 = curX - vx / 2
        var x1 = curX + vx / 2
        var y0 = curY - vy / 2
        var y1 = curY + vy / 2
        val m = 6f
        val shx = when {
            min(x0, x1) < m -> m - min(x0, x1)
            max(x0, x1) > screenW - m -> (screenW - m) - max(x0, x1)
            else -> 0f
        }
        val shy = when {
            min(y0, y1) < m -> m - min(y0, y1)
            max(y0, y1) > screenH - m -> (screenH - m) - max(y0, y1)
            else -> 0f
        }
        x0 += shx
        x1 += shx
        y0 += shy
        y1 += shy
        val dur = durMs.coerceIn(120L, 700L)
        val p = Path()
        p.moveTo(x0, y0)
        p.lineTo(x1, y1)
        val g = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(p, 0L, dur))
            .build()
        enqueue(Job(g, min(x0, x1), min(y0, y1), max(x0, x1), max(y0, y1), dur))
    }

    override fun onPinchGesture(ratio: Float) {
        val r = ratio.coerceIn(0.25f, 4f)
        if (abs(r - 1f) < 0.12f) return
        val base = min(screenW, screenH) * 0.14f
        val cx = curX.coerceIn(screenW * 0.2f, screenW * 0.8f)
        val cy = curY.coerceIn(8f, screenH - 8f)
        val room = min(cx, screenW - cx) - 8f
        val hs = base
        var he = base * r
        if (he > room) he = room
        if (abs(he - hs) < 6f) return
        val p1 = Path().apply {
            moveTo(cx - hs, cy)
            lineTo(cx - he, cy)
        }
        val p2 = Path().apply {
            moveTo(cx + hs, cy)
            lineTo(cx + he, cy)
        }
        val g = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(p1, 0L, 350L))
            .addStroke(GestureDescription.StrokeDescription(p2, 0L, 350L))
            .build()
        val w = max(hs, he)
        enqueue(Job(g, cx - w, cy, cx + w, cy, 350L))
    }

    override fun onEdge(action: Int) {
        haptic()
        when (action) {
            0 -> performGlobalAction(GLOBAL_ACTION_BACK)
            1 -> performGlobalAction(GLOBAL_ACTION_HOME)
            else -> performGlobalAction(GLOBAL_ACTION_RECENTS)
        }
    }

    override fun onPadDragStart() {
        val lp = padLp ?: return
        padStartX = lp.x
        padStartY = lp.y
    }

    override fun onPadDrag(totalDx: Float, totalDy: Float) {
        val lp = padLp ?: return
        val v = pad ?: return
        lp.x = (padStartX + totalDx.toInt()).coerceIn(0, max(0, screenW - lp.width))
        lp.y = (padStartY + totalDy.toInt()).coerceIn(0, max(0, screenH - lp.height))
        runCatching { wm.updateViewLayout(v, lp) }
    }

    override fun onPadDragEnd() {
        val lp = padLp ?: return
        st.setPad(landscape(), lp.x, lp.y)
    }
}
