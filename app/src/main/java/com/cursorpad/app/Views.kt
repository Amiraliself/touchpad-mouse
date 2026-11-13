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
import kotlin.math.hypot

object CursorPainter {
    private val fillP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val lineP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }
    private val path = Path()

    private val WIN = floatArrayOf(
        0f, 0f, 0f, 16.5f, 3.9f, 12.9f, 6.6f, 19.2f, 9.4f, 18f, 6.7f, 11.8f, 11.8f, 11.8f
    )
    private val MAC = floatArrayOf(
        0f, 0f, 0f, 15.8f, 3.5f, 12.6f, 5.9f, 18.2f, 8.5f, 17.1f, 6.2f, 11.7f, 11f, 11.4f
    )

    fun draw(c: Canvas, shape: Int, black: Boolean, size: Float, x: Float, y: Float) {
        val main = if (black) Color.BLACK else Color.WHITE
        val edge = if (black) Color.WHITE else Color.BLACK
        val w = (size * 0.07f).coerceAtLeast(1.4f)
        fillP.color = main
        lineP.color = edge
        c.save()
        c.translate(x, y)
        when (shape) {
            0, 1 -> {
                val s = size / 19.2f
                val pts = if (shape == 0) WIN else MAC
                path.reset()
                path.moveTo(pts[0] * s, pts[1] * s)
                var i = 2
                while (i < pts.size) {
                    path.lineTo(pts[i] * s, pts[i + 1] * s)
                    i += 2
                }
                path.close()
                val fx = if (shape == 1) CornerPathEffect(size * 0.07f) else null
                fillP.pathEffect = fx
                lineP.pathEffect = fx
                lineP.strokeWidth = w * 2f
                c.drawPath(path, lineP)
                c.drawPath(path, fillP)
            }
            2 -> {
                fillP.pathEffect = null
                lineP.pathEffect = null
                val r = size * 0.3f
                lineP.strokeWidth = w * 2f
                c.drawCircle(0f, 0f, r, lineP)
                c.drawCircle(0f, 0f, r, fillP)
                fillP.color = edge
                c.drawCircle(0f, 0f, r * 0.22f, fillP)
            }
            else -> {
                fillP.pathEffect = null
                lineP.pathEffect = null
                val l = size * 0.5f
                lineP.strokeWidth = w * 3.4f
                c.drawLine(-l, 0f, l, 0f, lineP)
                c.drawLine(0f, -l, 0f, l, lineP)
                lineP.color = main
                lineP.strokeWidth = w * 1.2f
                c.drawLine(-l, 0f, l, 0f, lineP)
                c.drawLine(0f, -l, 0f, l, lineP)
            }
        }
        c.restore()
    }
}

class CursorView(ctx: Context) : View(ctx) {
    var shape = 0
    var black = false
    var sizePx = 60f
    private var cx = 0f
    private var cy = 0f
    private var pulse = 0f
    private val loc = IntArray(2)
    private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }

    override fun onDraw(canvas: Canvas) {
        getLocationOnScreen(loc)
        val x = cx - loc[0]
        val y = cy - loc[1]
        if (pulse > 0f) {
            ring.color = Color.argb((170 * (1f - pulse)).toInt(), 90, 160, 255)
            canvas.drawCircle(x, y, sizePx * (0.3f + pulse * 0.9f), ring)
        }
        CursorPainter.draw(canvas, shape, black, sizePx, x, y)
    }

    fun moveTo(x: Float, y: Float) {
        cx = x
        cy = y
        invalidate()
    }

    fun click() {
        val a = ValueAnimator.ofFloat(0f, 1f)
        a.duration = 280
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
    fun onDragStart()
    fun onDragEnd()
    fun onScrollStart()
    fun onScroll(dx: Float, dy: Float)
    fun onScrollEnd()
    fun onEdge(action: Int)
    fun onPadDragStart()
    fun onPadDrag(totalDx: Float, totalDy: Float)
    fun onPadDragEnd()
}

class TouchpadView(ctx: Context, private val lst: PadListener) : View(ctx) {
    var edgeEnabled = true

    private val d = ctx.resources.displayMetrics.density
    private val slop = 7f * d
    private val handleH = 26f * d

    private val bg = Paint(Paint.ANTI_ALIAS_FLAG)
    private val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.5f * d
        color = 0x66AFC3FF
    }
    private val soft = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x40FFFFFF }
    private val hint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x30FFFFFF }
    private val glow = Paint(Paint.ANTI_ALIAS_FLAG)
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
    private var cx = 0f
    private var cy = 0f
    private var scrollTravel = 0f
    private var scrollStarted = false
    private var scrollT0 = 0L

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        bg.shader = LinearGradient(
            0f, 0f, 0f, h.toFloat(),
            0xF0343C58.toInt(), 0xF0131724.toInt(), Shader.TileMode.CLAMP
        )
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val r = 22f * d
        rect.set(d, d, w - d, h - d)
        canvas.drawRoundRect(rect, r, r, bg)

        rect.set(w / 2 - 22f * d, 9f * d, w / 2 + 22f * d, 13f * d)
        canvas.drawRoundRect(rect, 2f * d, 2f * d, soft)
        canvas.drawRect(18f * d, handleH, w - 18f * d, handleH + d * 0.8f, hint)

        if (edgeEnabled) {
            rect.set(w / 2 - 24f * d, h - 8f * d, w / 2 + 24f * d, h - 5.5f * d)
            canvas.drawRoundRect(rect, 2f * d, 2f * d, hint)
            val my = (h + handleH) / 2
            rect.set(4.5f * d, my - 18f * d, 7f * d, my + 18f * d)
            canvas.drawRoundRect(rect, 2f * d, 2f * d, hint)
            rect.set(w - 7f * d, my - 18f * d, w - 4.5f * d, my + 18f * d)
            canvas.drawRoundRect(rect, 2f * d, 2f * d, hint)
        }

        for (i in 0 until fn) {
            glow.shader = RadialGradient(
                fx[i], fy[i], 56f * d,
                0x777AA2FF, 0x007AA2FF, Shader.TileMode.CLAMP
            )
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
        scrollTravel = 0f
        scrollStarted = false
        mode = when {
            sy < handleH -> {
                rawSX = e.rawX
                rawSY = e.rawY
                lst.onPadDragStart()
                PADMOVE
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

    private fun pointerDown(e: MotionEvent) {
        if (mode == UNDECIDED || mode == MOVING) {
            mode = SCROLL
            scrollTravel = 0f
            scrollStarted = false
            scrollT0 = e.eventTime
            cx = centroidX(e)
            cy = centroidY(e)
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
            SCROLL -> {
                if (e.pointerCount >= 2) {
                    val nx = centroidX(e)
                    val ny = centroidY(e)
                    val dx = nx - cx
                    val dy = ny - cy
                    cx = nx
                    cy = ny
                    scrollTravel += hypot(dx, dy)
                    if (!scrollStarted) {
                        if (scrollTravel > slop) {
                            scrollStarted = true
                            lst.onScrollStart()
                        }
                    } else {
                        lst.onScroll(dx, dy)
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
        if (mode == SCROLL) {
            if (scrollStarted) {
                lst.onScrollEnd()
            } else if (e.eventTime - scrollT0 < 300) {
                lst.onTwoFingerTap()
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
            SCROLL -> if (scrollStarted) lst.onScrollEnd()
            EDGE -> if (!cancelled) finishEdge(e)
            PADMOVE -> lst.onPadDragEnd()
        }
        mode = IDLE
    }

    private fun finishEdge(e: MotionEvent) {
        val dx = e.x - sx
        val dy = e.y - sy
        val thr = 45f * d
        when (edgeType) {
            1 -> if (dx > thr) lst.onEdge(0)
            2 -> if (-dx > thr) lst.onEdge(0)
            3 -> if (-dy > thr) lst.onEdge(if (e.eventTime - lastEdgeMove > 350) 2 else 1)
        }
    }

    companion object {
        private const val IDLE = 0
        private const val UNDECIDED = 1
        private const val MOVING = 2
        private const val DRAG = 3
        private const val SCROLL = 4
        private const val EDGE = 5
        private const val PADMOVE = 6
        private const val IGNORE = 7
    }
}
