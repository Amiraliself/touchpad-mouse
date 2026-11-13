package com.cursorpad.app

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.CornerPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs
import kotlin.math.hypot

class PadTheme(
    val name: String,
    val top: Int,
    val bottom: Int,
    val border: Int,
    val accent: Int,
    val light: Boolean
)

object Themes {
    val pad = listOf(
        PadTheme("نیمه‌شب", 0xF0343C58.toInt(), 0xF0131724.toInt(), 0x66AFC3FF, 0xFF7AA2FF.toInt(), false),
        PadTheme("گرافیت", 0xF03C3C3C.toInt(), 0xF0151515.toInt(), 0x66D0D0D0, 0xFFBDBDBD.toInt(), false),
        PadTheme("زمرد", 0xF0145A40.toInt(), 0xF0062014.toInt(), 0x6672E3A8, 0xFF4ADE80.toInt(), false),
        PadTheme("یاقوت", 0xF06A1B28.toInt(), 0xF021070B.toInt(), 0x66FF8A9B, 0xFFF87171.toInt(), false),
        PadTheme("بنفش", 0xF0522E86.toInt(), 0xF01A0E33.toInt(), 0x66D2A8FF, 0xFFC084FC.toInt(), false),
        PadTheme("کهربا", 0xF0704A12.toInt(), 0xF0241605.toInt(), 0x66FFD27A, 0xFFFBBF24.toInt(), false),
        PadTheme("اقیانوس", 0xF0125A70.toInt(), 0xF0041D28.toInt(), 0x667FE6FF, 0xFF22D3EE.toInt(), false),
        PadTheme("صورتی", 0xF0722450.toInt(), 0xF0240916.toInt(), 0x66FF9FD0, 0xFFF472B6.toInt(), false),
        PadTheme("شیشه‌ای", 0xF0F0F3FA.toInt(), 0xF0BFC8DE.toInt(), 0x99FFFFFF.toInt(), 0xFF3B5BDB.toInt(), true)
    )

    val accents = intArrayOf(
        0xFF7AA2FF.toInt(),
        0xFF4ADE80.toInt(),
        0xFFF87171.toInt(),
        0xFFFBBF24.toInt(),
        0xFFC084FC.toInt(),
        0xFF22D3EE.toInt()
    )
}

/** رسم شکل‌های نشانگر؛ هم در سرویس و هم در پیش‌نمایش برنامه استفاده می‌شود. */
object CursorPainter {
    private val fillP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val lineP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }
    private val path = Path()
    private val rr = RectF()

    private val WIN = floatArrayOf(
        0f, 0f, 0f, 16.5f, 3.9f, 12.9f, 6.6f, 19.2f, 9.4f, 18f, 6.7f, 11.8f, 11.8f, 11.8f
    )
    private val MAC = floatArrayOf(
        0f, 0f, 0f, 15.8f, 3.5f, 12.6f, 5.9f, 18.2f, 8.5f, 17.1f, 6.2f, 11.7f, 11f, 11.4f
    )

    // برای پیش‌نمایش: شکل را وسط کادر می‌آورد
    fun offX(shape: Int, size: Float): Float = if (shape <= 2) -size * 0.2f else 0f

    fun offY(shape: Int, size: Float): Float = if (shape <= 3) -size * 0.5f else 0f

    /**
     * شکل‌ها: 0 ویندوز، 1 مک، 2 فلش توخالی، 3 دست، 4 دایره، 5 نقطه، 6 ضربدر، 7 هدف
     * (x, y) نقطه‌ی کلیک است.
     */
    fun draw(c: Canvas, shape: Int, black: Boolean, shadow: Boolean, size: Float, x: Float, y: Float) {
        val main = if (black) Color.BLACK else Color.WHITE
        val edge = if (black) Color.WHITE else Color.BLACK
        val w = (size * 0.07f).coerceAtLeast(1.4f)
        if (shadow) {
            val o = size * 0.07f + 1f
            c.save()
            c.translate(x + o, y + o * 1.3f)
            body(c, shape, size, 0x55000000, 0x55000000, w * 1.15f)
            c.restore()
        }
        c.save()
        c.translate(x, y)
        body(c, shape, size, main, edge, w)
        c.restore()
    }

    private fun body(c: Canvas, shape: Int, size: Float, main: Int, edge: Int, w: Float) {
        fillP.color = main
        lineP.color = edge
        fillP.pathEffect = null
        lineP.pathEffect = null
        val s = size / 19.2f
        when (shape) {
            0, 1, 2 -> {
                val pts = if (shape == 1) MAC else WIN
                path.reset()
                path.moveTo(pts[0] * s, pts[1] * s)
                var i = 2
                while (i < pts.size) {
                    path.lineTo(pts[i] * s, pts[i + 1] * s)
                    i += 2
                }
                path.close()
                if (shape == 1) {
                    val fx = CornerPathEffect(size * 0.07f)
                    fillP.pathEffect = fx
                    lineP.pathEffect = fx
                }
                if (shape == 2) {
                    lineP.strokeWidth = w * 3.4f
                    c.drawPath(path, lineP)
                    lineP.color = main
                    lineP.strokeWidth = w * 1.5f
                    c.drawPath(path, lineP)
                } else {
                    lineP.strokeWidth = w * 2f
                    c.drawPath(path, lineP)
                    c.drawPath(path, fillP)
                }
            }
            3 -> {
                val rects = arrayOf(
                    floatArrayOf(-1.7f, 0f, 1.7f, 11f, 1.7f),
                    floatArrayOf(-5.6f, 8.2f, 7.4f, 18.8f, 3f),
                    floatArrayOf(1.4f, 5.8f, 4.5f, 11f, 1.5f),
                    floatArrayOf(4.2f, 7.2f, 7.2f, 12f, 1.5f),
                    floatArrayOf(-8f, 10.4f, -4f, 15.6f, 2f)
                )
                lineP.strokeWidth = w * 2f
                for (r in rects) {
                    rr.set(r[0] * s, r[1] * s, r[2] * s, r[3] * s)
                    c.drawRoundRect(rr, r[4] * s, r[4] * s, lineP)
                }
                for (r in rects) {
                    rr.set(r[0] * s, r[1] * s, r[2] * s, r[3] * s)
                    c.drawRoundRect(rr, r[4] * s, r[4] * s, fillP)
                }
            }
            4 -> {
                val r = size * 0.3f
                lineP.strokeWidth = w * 2f
                c.drawCircle(0f, 0f, r, lineP)
                c.drawCircle(0f, 0f, r, fillP)
                fillP.color = edge
                c.drawCircle(0f, 0f, r * 0.22f, fillP)
            }
            5 -> {
                val r = size * 0.17f
                lineP.strokeWidth = w * 2f
                c.drawCircle(0f, 0f, r, lineP)
                c.drawCircle(0f, 0f, r, fillP)
            }
            6 -> {
                val l = size * 0.5f
                lineP.strokeWidth = w * 3.4f
                c.drawLine(-l, 0f, l, 0f, lineP)
                c.drawLine(0f, -l, 0f, l, lineP)
                lineP.color = main
                lineP.strokeWidth = w * 1.2f
                c.drawLine(-l, 0f, l, 0f, lineP)
                c.drawLine(0f, -l, 0f, l, lineP)
            }
            else -> {
                val r = size * 0.34f
                lineP.strokeWidth = w * 3.6f
                c.drawCircle(0f, 0f, r, lineP)
                lineP.color = main
                lineP.strokeWidth = w * 1.6f
                c.drawCircle(0f, 0f, r, lineP)
                fillP.color = edge
                c.drawCircle(0f, 0f, size * 0.1f, fillP)
                fillP.color = main
                c.drawCircle(0f, 0f, size * 0.065f, fillP)
            }
        }
    }
}

/** پنجره‌ی تمام‌صفحه‌ی غیرقابل‌لمس که فقط نشانگر را می‌کشد. */
class CursorView(ctx: Context) : View(ctx) {
    var shape = 0
    var black = false
    var shadow = true
    var sizePx = 60f
    var animStyle = 1
    var animColor = 0xFF7AA2FF.toInt()

    private var cx = 0f
    private var cy = 0f
    private var pulse = 0f
    private val loc = IntArray(2)
    private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }
    private val disc = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

    override fun onDraw(canvas: Canvas) {
        getLocationOnScreen(loc)
        val x = cx - loc[0]
        val y = cy - loc[1]
        if (pulse > 0f && animStyle != 0) {
            val a = (190 * (1f - pulse)).toInt()
            val rgb = animColor and 0x00FFFFFF
            if (animStyle == 1) {
                ring.color = rgb or (a shl 24)
                canvas.drawCircle(x, y, sizePx * (0.3f + pulse * 0.9f), ring)
            } else {
                disc.color = rgb or ((a * 0.7f).toInt() shl 24)
                canvas.drawCircle(x, y, sizePx * (0.15f + pulse * 0.6f), disc)
            }
        }
        CursorPainter.draw(canvas, shape, black, shadow, sizePx, x, y)
    }

    fun moveTo(x: Float, y: Float) {
        cx = x
        cy = y
        invalidate()
    }

    fun click() {
        if (animStyle == 0) return
        val a = ValueAnimator.ofFloat(0f, 1f)
        a.duration = 320
        a.addUpdateListener {
            val v = it.animatedValue as Float
            pulse = if (v >= 1f) 0f else v
            invalidate()
        }
        a.start()
    }
}

interface PadListener {
    fun onMove(dx: Float, dy: Float, dtMs: Long)
    fun onTap()
    fun onTwoFingerTap()
    fun onRightClick()
    fun onDragStart()
    fun onDragEnd()
    fun onScrollGesture(dx: Float, dy: Float, durMs: Long)
    fun onPinchGesture(ratio: Float)
    fun onEdge(action: Int)
    fun onPadDragStart()
    fun onPadDrag(totalDx: Float, totalDy: Float)
    fun onPadDragEnd()
}

/** خود تاچ‌پد: همه‌ی منطق حرکات اینجاست. */
class TouchpadView(ctx: Context, private val lst: PadListener) : View(ctx) {
    var edgeEnabled = true
        set(v) {
            field = v
            invalidate()
        }
    var locked = false
        set(v) {
            field = v
            invalidate()
        }
    var rightBtn = true
        set(v) {
            field = v
            invalidate()
        }
    var themeIndex = 0
        set(v) {
            field = v.coerceIn(0, Themes.pad.size - 1)
            rebuildShader()
            invalidate()
        }

    private val d = ctx.resources.displayMetrics.density
    private val slop = 7f * d
    private val handleH = 26f * d

    private val bg = Paint(Paint.ANTI_ALIAS_FLAG)
    private val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.5f * d
    }
    private val soft = Paint(Paint.ANTI_ALIAS_FLAG)
    private val hint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glow = Paint(Paint.ANTI_ALIAS_FLAG)
    private val btnP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val btnStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.2f * d
    }
    private val txt = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = 11f * d
    }
    private val rect = RectF()

    private val fx = FloatArray(6)
    private val fy = FloatArray(6)
    private var fn = 0

    private var mode = IDLE
    private var downTime = 0L
    private var sx = 0f
    private var sy = 0f
    private var lx = 0f
    private var ly = 0f
    private var lt = 0L
    private var rawSX = 0f
    private var rawSY = 0f
    private var lastTapUp = 0L
    private var lastTapX = 0f
    private var lastTapY = 0f
    private var edgeType = 0
    private var lastEdgeMove = 0L
    private var btnDown = false

    private var twoType = 0
    private var twoT0 = 0L
    private var c0x = 0f
    private var c0y = 0f
    private var s0 = 1f
    private var twoCx = 0f
    private var twoCy = 0f
    private var twoSp = 1f

    private fun rebuildShader() {
        if (height <= 0) return
        val t = Themes.pad[themeIndex]
        bg.shader = LinearGradient(
            0f, 0f, 0f, height.toFloat(), t.top, t.bottom, Shader.TileMode.CLAMP
        )
        border.color = t.border
        soft.color = if (t.light) 0x66000000 else 0x55FFFFFF
        hint.color = if (t.light) 0x33000000 else 0x30FFFFFF
    }

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        rebuildShader()
    }

    private fun btnLeft() = width - 84f * d
    private fun btnRight() = width - 14f * d
    private fun btnTop() = height - 44f * d
    private fun btnBottom() = height - 14f * d

    private fun inBtn(x: Float, y: Float): Boolean =
        rightBtn && x >= btnLeft() && x <= btnRight() && y >= btnTop() && y <= btnBottom()

    override fun onDraw(canvas: Canvas) {
        val t = Themes.pad[themeIndex]
        val w = width.toFloat()
        val h = height.toFloat()
        val r = 22f * d
        rect.set(d, d, w - d, h - d)
        canvas.drawRoundRect(rect, r, r, bg)

        if (!locked) {
            rect.set(w / 2 - 22f * d, 9f * d, w / 2 + 22f * d, 13f * d)
            canvas.drawRoundRect(rect, 2f * d, 2f * d, soft)
            canvas.drawRect(18f * d, handleH, w - 18f * d, handleH + d * 0.8f, hint)
        }

        if (edgeEnabled) {
            rect.set(w / 2 - 24f * d, h - 8f * d, w / 2 + 24f * d, h - 5.5f * d)
            canvas.drawRoundRect(rect, 2f * d, 2f * d, hint)
            val my = (h + handleH) / 2
            rect.set(4.5f * d, my - 18f * d, 7f * d, my + 18f * d)
            canvas.drawRoundRect(rect, 2f * d, 2f * d, hint)
            rect.set(w - 7f * d, my - 18f * d, w - 4.5f * d, my + 18f * d)
            canvas.drawRoundRect(rect, 2f * d, 2f * d, hint)
        }

        if (rightBtn) {
            val rgb = t.accent and 0x00FFFFFF
            rect.set(btnLeft(), btnTop(), btnRight(), btnBottom())
            btnP.color = rgb or (if (btnDown) 0x77000000 else 0x33000000)
            canvas.drawRoundRect(rect, 10f * d, 10f * d, btnP)
            btnStroke.color = rgb or 0x99000000.toInt()
            canvas.drawRoundRect(rect, 10f * d, 10f * d, btnStroke)
            txt.color = if (t.light) 0xFF222222.toInt() else Color.WHITE
            canvas.drawText("کلیک راست", rect.centerX(), rect.centerY() + 4f * d, txt)
        }

        val gc0 = t.accent and 0x00FFFFFF
        val gc = gc0 or 0x77000000
        for (i in 0 until fn) {
            glow.shader = RadialGradient(fx[i], fy[i], 56f * d, gc, gc0, Shader.TileMode.CLAMP)
            canvas.drawCircle(fx[i], fy[i], 56f * d, glow)
        }

        rect.set(d, d, w - d, h - d)
        canvas.drawRoundRect(rect, r, r, border)
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> down(e)
            MotionEvent.ACTION_POINTER_DOWN -> pointerDown(e)
            MotionEvent.ACTION_MOVE -> move(e)
            MotionEvent.ACTION_POINTER_UP -> pointerUp(e)
            MotionEvent.ACTION_UP -> up(e, false)
            MotionEvent.ACTION_CANCEL -> up(e, true)
        }
        when (e.actionMasked) {
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> fn = 0
            MotionEvent.ACTION_POINTER_UP -> updateGlow(e, e.actionIndex)
            else -> updateGlow(e, -1)
        }
        invalidate()
        return true
    }

    private fun updateGlow(e: MotionEvent, skip: Int) {
        var n = 0
        for (i in 0 until e.pointerCount) {
            if (i == skip || n >= 6) continue
            fx[n] = e.getX(i)
            fy[n] = e.getY(i)
            n++
        }
        fn = n
    }

    private fun edgeZone(): Boolean {
        edgeType = when {
            sx < 18f * d -> 1
            sx > width - 18f * d -> 2
            sy > height - 18f * d -> 3
            else -> 0
        }
        return edgeType != 0
    }

    private fun down(e: MotionEvent) {
        downTime = e.eventTime
        sx = e.x
        sy = e.y
        lx = sx
        ly = sy
        lt = e.eventTime
        btnDown = false
        mode = when {
            !locked && sy < handleH -> {
                rawSX = e.rawX
                rawSY = e.rawY
                lst.onPadDragStart()
                PADMOVE
            }
            inBtn(sx, sy) -> {
                btnDown = true
                BUTTON
            }
            edgeEnabled && edgeZone() -> {
                lastEdgeMove = e.eventTime
                EDGE
            }
            e.eventTime - lastTapUp < 280 &&
                hypot(sx - lastTapX, sy - lastTapY) < 80f * d -> {
                lastTapUp = 0
                lst.onDragStart()
                DRAG
            }
            else -> UNDECIDED
        }
    }

    private fun centroidX(e: MotionEvent): Float {
        var s = 0f
        for (i in 0 until e.pointerCount) s += e.getX(i)
        return s / e.pointerCount
    }

    private fun centroidY(e: MotionEvent): Float {
        var s = 0f
        for (i in 0 until e.pointerCount) s += e.getY(i)
        return s / e.pointerCount
    }

    private fun spread(e: MotionEvent): Float {
        if (e.pointerCount < 2) return 1f
        return hypot(e.getX(0) - e.getX(1), e.getY(0) - e.getY(1))
    }

    private fun pointerDown(e: MotionEvent) {
        if (mode == UNDECIDED || mode == MOVING) {
            mode = TWO
            twoType = 0
            twoT0 = e.eventTime
            c0x = centroidX(e)
            c0y = centroidY(e)
            s0 = spread(e).coerceAtLeast(1f)
            twoCx = c0x
            twoCy = c0y
            twoSp = s0
        }
    }

    private fun move(e: MotionEvent) {
        when (mode) {
            PADMOVE -> lst.onPadDrag(e.rawX - rawSX, e.rawY - rawSY)
            UNDECIDED -> {
                if (hypot(e.x - sx, e.y - sy) > slop) {
                    mode = MOVING
                    lx = e.x
                    ly = e.y
                    lt = e.eventTime
                }
            }
            MOVING, DRAG -> {
                val dx = e.x - lx
                val dy = e.y - ly
                val dt = e.eventTime - lt
                lx = e.x
                ly = e.y
                lt = e.eventTime
                if (dx != 0f || dy != 0f) lst.onMove(dx, dy, dt)
            }
            TWO -> {
                if (e.pointerCount >= 2) {
                    twoCx = centroidX(e)
                    twoCy = centroidY(e)
                    twoSp = spread(e)
                    if (twoType == 0) {
                        val ct = hypot(twoCx - c0x, twoCy - c0y)
                        val sd = abs(twoSp - s0)
                        if (maxOf(ct, sd) > slop * 1.4f) {
                            twoType = if (sd > ct) 2 else 1
                        }
                    }
                }
            }
            EDGE -> {
                if (hypot(e.x - lx, e.y - ly) > 4f * d) {
                    lx = e.x
                    ly = e.y
                    lastEdgeMove = e.eventTime
                }
            }
        }
    }

    private fun pointerUp(e: MotionEvent) {
        if (mode == TWO) {
            val dur = e.eventTime - twoT0
            when (twoType) {
                0 -> {
                    if (dur < 300) lst.onTwoFingerTap()
                }
                1 -> lst.onScrollGesture(twoCx - c0x, twoCy - c0y, dur)
                else -> lst.onPinchGesture(twoSp / s0)
            }
            mode = IGNORE
        }
    }

    private fun up(e: MotionEvent, cancelled: Boolean) {
        when (mode) {
            UNDECIDED -> {
                if (!cancelled && e.eventTime - downTime < 300) {
                    lastTapUp = e.eventTime
                    lastTapX = sx
                    lastTapY = sy
                    lst.onTap()
                }
            }
            DRAG -> lst.onDragEnd()
            EDGE -> {
                if (!cancelled) finishEdge(e)
            }
            PADMOVE -> lst.onPadDragEnd()
            BUTTON -> {
                if (!cancelled && inBtn(e.x, e.y) && e.eventTime - downTime < 500) {
                    lst.onRightClick()
                }
            }
        }
        btnDown = false
        mode = IDLE
    }

    private fun finishEdge(e: MotionEvent) {
        val dx = e.x - sx
        val dy = e.y - sy
        val thr = 45f * d
        when (edgeType) {
            1 -> {
                if (dx > thr) lst.onEdge(0)
            }
            2 -> {
                if (-dx > thr) lst.onEdge(0)
            }
            3 -> {
                if (-dy > thr) lst.onEdge(if (e.eventTime - lastEdgeMove > 350) 2 else 1)
            }
        }
    }

    companion object {
        private const val IDLE = 0
        private const val UNDECIDED = 1
        private const val MOVING = 2
        private const val DRAG = 3
        private const val TWO = 4
        private const val EDGE = 5
        private const val PADMOVE = 6
        private const val BUTTON = 7
        private const val IGNORE = 8
    }
}
