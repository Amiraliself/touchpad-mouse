package com.cursorpad.app

import android.content.Context
import android.content.SharedPreferences

class Prefs(ctx: Context) {
    val prefs: SharedPreferences =
        ctx.applicationContext.getSharedPreferences("cursorpad", Context.MODE_PRIVATE)

    private fun putB(k: String, v: Boolean) {
        prefs.edit().putBoolean(k, v).apply()
    }

    private fun putI(k: String, v: Int) {
        prefs.edit().putInt(k, v).apply()
    }

    private fun putF(k: String, v: Float) {
        prefs.edit().putFloat(k, v).apply()
    }

    var enabled: Boolean
        get() = prefs.getBoolean("enabled", true)
        set(v) { putB("enabled", v) }

    var shape: Int
        get() = prefs.getInt("shape", 0)
        set(v) { putI("shape", v) }

    var black: Boolean
        get() = prefs.getBoolean("black", false)
        set(v) { putB("black", v) }

    var sizeDp: Int
        get() = prefs.getInt("sizeDp", 24)
        set(v) { putI("sizeDp", v) }

    var cursorAlpha: Int
        get() = prefs.getInt("cursorAlpha", 100)
        set(v) { putI("cursorAlpha", v) }

    var shadow: Boolean
        get() = prefs.getBoolean("shadow", true)
        set(v) { putB("shadow", v) }

    var clickAnim: Int
        get() = prefs.getInt("clickAnim", 1)
        set(v) { putI("clickAnim", v) }

    var clickColor: Int
        get() = prefs.getInt("clickColor", 0)
        set(v) { putI("clickColor", v) }

    var speed: Float
        get() = prefs.getFloat("speed", 2.5f)
        set(v) { putF("speed", v) }

    var accel: Boolean
        get() = prefs.getBoolean("accel", true)
        set(v) { putB("accel", v) }

    var haptic: Boolean
        get() = prefs.getBoolean("haptic", true)
        set(v) { putB("haptic", v) }

    var edge: Boolean
        get() = prefs.getBoolean("edge", true)
        set(v) { putB("edge", v) }

    var rightBtn: Boolean
        get() = prefs.getBoolean("rightBtn", true)
        set(v) { putB("rightBtn", v) }

    var dragHold: Int
        get() = prefs.getInt("dragHold", 450)
        set(v) { putI("dragHold", v) }

    var scrollReverse: Boolean
        get() = prefs.getBoolean("scrollReverse", false)
        set(v) { putB("scrollReverse", v) }

    var scrollSens: Float
        get() = prefs.getFloat("scrollSens", 1f)
        set(v) { putF("scrollSens", v) }

    var padAlpha: Int
        get() = prefs.getInt("padAlpha", 75)
        set(v) { putI("padAlpha", v) }

    var padWidthDp: Int
        get() = prefs.getInt("padWidthDp", 260)
        set(v) { putI("padWidthDp", v) }

    var padTheme: Int
        get() = prefs.getInt("padTheme", 0)
        set(v) { putI("padTheme", v) }

    var padLocked: Boolean
        get() = prefs.getBoolean("padLocked", false)
        set(v) { putB("padLocked", v) }

    var notif: Boolean
        get() = prefs.getBoolean("notif", true)
        set(v) { putB("notif", v) }

    fun padX(landscape: Boolean): Int = prefs.getInt(if (landscape) "pxl" else "pxp", -1)

    fun padY(landscape: Boolean): Int = prefs.getInt(if (landscape) "pyl" else "pyp", -1)

    fun setPad(landscape: Boolean, x: Int, y: Int) {
        prefs.edit()
            .putInt(if (landscape) "pxl" else "pxp", x)
            .putInt(if (landscape) "pyl" else "pyp", y)
            .apply()
    }

    fun reset() {
        prefs.edit().clear().apply()
    }
}
