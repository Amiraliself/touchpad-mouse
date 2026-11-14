package com.cursorpad.app

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

class RecordTileService : TileService() {

    override fun onStartListening() {
        refresh()
    }

    override fun onClick() {
        val p = Prefs(this)
        p.recordMode = !p.recordMode
        refresh()
    }

    private fun refresh() {
        val t = qsTile ?: return
        t.state = if (Prefs(this).recordMode) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        t.updateTile()
    }
}
