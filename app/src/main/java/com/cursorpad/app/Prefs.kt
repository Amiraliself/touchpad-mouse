package com.cursorpad.app

import android.content.Context
import android.content.SharedPreferences

class Prefs(ctx: Context) {
    val prefs: SharedPreferences =
        ctx.applicationContext.getSharedPreferences("cursorpad", Context.MODE_PRIVATE)

    var enabled: Boolean
        get() = prefs.getBoolean("enabled", true)
        set(v) { prefs.edit().putBoolean("enabled", v).apply() }

    var shape: Int
        get() = prefs.getInt("shape", 0)
        set(v) { prefs.edit().putInt("shape", v).apply() }

    var black: Boolean
        get() = prefs.getBoolean("black", false)
        set(v) { prefs.edit().putBoolean("black", v).apply() }

    var sizeDp: Int
        get() = prefs.getInt("sizeDp", 24)
        set(v) { prefs.edit().putInt("sizeDp", v).apply() }

    var speed: Float
        get() = prefs.getFloat("speed", 2.5f)
        set(v) { prefs.edit().putFloat("speed", v).apply() }

    var accel: Boolean
        get() = prefs.getBoolean("accel", true)
        set(v) { prefs.edit().putBoolean("accel", v).apply() }

    var haptic: Boolean
        get() = prefs.getBoolean("haptic", true)
        set(v) { prefs.edit().putBoolean("haptic", v).apply() }

    var edge: Boolean
        get() = prefs.getBoolean("edge", true)
        set(v) { prefs.edit().putBoolean("edge", v).apply() }

    var padAlpha: Int
        get() = prefs.getInt("padAlpha", 75)
        set(v) { prefs.edit().putInt("padAlpha", v).apply() }

    var padWidthDp: Int
        get() = prefs.getInt("padWidthDp", 260)
        set(v) { prefs.edit().putInt("padWidthDp", v).apply() }

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
