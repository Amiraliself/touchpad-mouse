package com.cursorpad.app

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.Path
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.WindowManager
import android.view.WindowManager.LayoutParams as LP
import android.view.accessibility.AccessibilityEvent
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

class MouseService : AccessibilityService(), PadListener {

    companion object {
        @Volatile
        var instance: MouseService? = null

        private const val NATURAL_SCROLL = true
        private const val SCROLL_GAIN = 2.2f
        private const val G_NONE = 0
        private const val G_DRAG = 1
        private const val G_SCROLL = 2
    }

    private lateinit var st: Prefs
    private lateinit var wm: WindowManager
    private val handler = Handler(Looper.getMainLooper())
    private val prefListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> reload() }

    private var cursor: CursorView? = null
    private var pad: TouchpadView? = null
    private var padLp: LP? = null
    private var shown = false

    private var screenW = 1080
    private var screenH = 1920
    private var curX = 0f
    private var curY = 0f

    private var gMode = G_NONE
    private var gActive = false
    private var gBusy = false
    private var gEnding = false
    private var gStroke: GestureDescription.StrokeDescription? = null
    private var gLastX = 0f
    private var gLastY = 0f
    private var gTx = 0f
    private var gTy = 0f
    private var tapInFlight = false
    private var dragSkipped = false
    private var dragMoved = false
    private var originX = 0f
    private var originY = 0f
    private var accX = 0f
    private var accY = 0f
    private var padStartX = 0
    private var padStartY = 0

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        st = Prefs(this)
        wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        st.prefs.registerOnSharedPreferenceChangeListener(prefListener)
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

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        shutdown()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        shutdown()
        super.onDestroy()
    }

    private fun shutdown() {
        if (::st.isInitialized) {
            st.prefs.unregisterOnSharedPreferenceChangeListener(prefListener)
        }
        removeOverlays()
        instance = null
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
        }
    }

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

        val cv = CursorView(this)
        val clp = LP(
            LP.MATCH_PARENT, LP.MATCH_PARENT,
            LP.TYPE_ACCESSIBILITY_OVERLAY,
            LP.FLAG_NOT_FOCUSABLE or LP.FLAG_NOT_TOUCHABLE or
                LP.FLAG_LAYOUT_IN_SCREEN or LP.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )
        if (Build.VERSION.SDK_INT >= 28) {
            clp.layoutInDisplayCutoutMode =
                LP.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        cursor = cv
        wm.addView(cv, clp)
        shown = true
    }

    private fun removeOverlays() {
        gReset()
        handler.removeCallbacksAndMessages(null)
        cursor?.let { v -> runCatching { wm.removeView(v) } }
        pad?.let { v -> runCatching { wm.removeView(v) } }
        cursor = null
        pad = null
        padLp = null
        shown = false
        tapInFlight = false
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
            it.sizePx = st.sizeDp * dens
        }
        val lp = padLp
        val pv = pad
        if (lp != null && pv != null) {
            pv.alpha = st.padAlpha / 100f
            pv.edgeEnabled = st.edge
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

    private fun underPad(x: Float, y: Float): Boolean {
        val lp = padLp ?: return false
        val m = 6f * resources.displayMetrics.density
        return x >= lp.x - m && x <= lp.x + lp.width + m &&
            y >= lp.y - m && y <= lp.y + lp.height + m
    }

    private fun haptic() {
        if (!st.haptic) return
        runCatching {
            @Suppress("DEPRECATION")
            val v = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            v.vibrate(VibrationEffect.createOneShot(14, 110))
        }
    }

    private fun gIdle() = !gActive && !gBusy && !tapInFlight

    private fun gReset() {
        gActive = false
        gEnding = false
        gStroke = null
        gMode = G_NONE
    }

    private fun gStart(x: Float, y: Float): Boolean {
        if (!gIdle()) return false
        gActive = true
        gEnding = false
        gLastX = x
        gLastY = y
        gTx = x
        gTy = y
        val s = GestureDescription.StrokeDescription(Path().apply { moveTo(x, y) }, 0, 50, true)
        gStroke = s
        gDispatch(s)
        return true
    }

    private fun gMove(x: Float, y: Float) {
        if (!gActive) return
        gTx = x
        gTy = y
        if (!gBusy) gPump()
    }

    private fun gEnd() {
        if (!gActive) return
        gEnding = true
        if (!gBusy) gPump()
    }

    private fun gPump() {
        if (!gActive || gBusy) return
        val prev = gStroke
        if (prev == null) {
            gReset()
            return
        }
        val moved = gTx != gLastX || gTy != gLastY
        if (!moved && !gEnding) return
        val p = Path()
        p.moveTo(gLastX, gLastY)
        var dur = 10L
        if (moved) {
            p.lineTo(gTx, gTy)
            dur = (hypot(gTx - gLastX, gTy - gLastY) / 2f).toLong().coerceIn(10L, 120L)
        }
        val cont = !gEnding
        val s = try {
            prev.continueStroke(p, 0, dur, cont)
        } catch (ex: Exception) {
            gReset()
            return
        }
        gLastX = gTx
        gLastY = gTy
        if (cont) {
            gStroke = s
        } else {
            gStroke = null
            gActive = false
            gMode = G_NONE
        }
        gDispatch(s)
    }

    private fun gDispatch(s: GestureDescription.StrokeDescription) {
        gBusy = true
        val g = GestureDescription.Builder().addStroke(s).build()
        val ok = dispatchGesture(g, object : AccessibilityService.GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                gBusy = false
                gPump()
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                gBusy = false
                if (gActive) gReset()
            }
        }, handler)
        if (!ok) {
            gBusy = false
            gReset()
        }
    }

    private fun clickAt(x: Float, y: Float, dur: Long) {
        if (!gIdle()) return
        tapInFlight = true
        haptic()
        cursor?.click()
        val under = underPad(x, y)
        if (under) setPadTouchable(false)

        val finish = Runnable {
            tapInFlight = false
            setPadTouchable(true)
        }
        handler.postDelayed({
            val p = Path().apply { moveTo(x, y) }
            val g = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(p, 0, dur))
                .build()
            val ok = dispatchGesture(g, object : AccessibilityService.GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    handler.removeCallbacks(finish)
                    finish.run()
                }

                override fun onCancelled(gestureDescription: GestureDescription?) {
                    handler.removeCallbacks(finish)
                    finish.run()
                }
            }, handler)
            if (!ok) finish.run()
        }, if (under) 50L else 0L)
        handler.postDelayed(finish, dur + 900L)
    }

    override fun onMove(dx: Float, dy: Float, dtMs: Long) {
        var f = st.speed
        if (st.accel) {
            val v = hypot(dx, dy) / max(1L, dtMs)
            f *= 0.45f + min(v, 3.2f) * 0.8f
        }
        curX = (curX + dx * f).coerceIn(0f, screenW - 1f)
        curY = (curY + dy * f).coerceIn(0f, screenH - 1f)
        cursor?.moveTo(curX, curY)
        if (gMode == G_DRAG) {
            dragMoved = true
            gMove(curX, curY)
        }
    }

    override fun onTap() {
        clickAt(curX, curY, 45L)
    }

    override fun onTwoFingerTap() {
        clickAt(curX, curY, 650L)
    }

    override fun onDragStart() {
        dragMoved = false
        dragSkipped = true
        if (underPad(curX, curY) || !gIdle()) return
        if (gStart(curX, curY)) {
            gMode = G_DRAG
            dragSkipped = false
            haptic()
        }
    }

    override fun onDragEnd() {
        if (gMode == G_DRAG) {
            gEnd()
        } else if (dragSkipped && !dragMoved) {
            onTap()
        }
        dragSkipped = false
    }

    override fun onScrollStart() {
        if (underPad(curX, curY) || !gIdle()) return
        originX = curX
        originY = curY
        accX = 0f
        accY = 0f
        if (gStart(curX, curY)) gMode = G_SCROLL
    }

    override fun onScroll(dx: Float, dy: Float) {
        if (gMode != G_SCROLL) return
        val k = SCROLL_GAIN * (if (NATURAL_SCROLL) 1f else -1f)
        val tx = (originX + accX + dx * k).coerceIn(2f, screenW - 3f)
        val ty = (originY + accY + dy * k).coerceIn(2f, screenH - 3f)
        accX = tx - originX
        accY = ty - originY
        gMove(tx, ty)
    }

    override fun onScrollEnd() {
        if (gMode == G_SCROLL) gEnd()
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
