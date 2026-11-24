package app.sidereply.root

/**
 * The window that currently owns the display focus, as reported by `dumpsys window`.
 *
 * [userId] is the Android user the window belongs to: `0` for normal apps, `999` (or another
 * clone-space id) for 「应用双开 / 应用分身」 copies. UsageStats only reports events for the
 * caller's own user, so a dual-space WeChat is invisible to it; the focused window is not.
 */
data class ForegroundWindow(val packageName: String, val userId: Int) {

    val isDualSpace: Boolean get() = userId != 0
}

/**
 * Parses `dumpsys window displays` output instead of scanning pixels, so it also sees windows
 * that belong to another user or to a clone container.
 *
 * `grep -m 2` makes dumpsys die from SIGPIPE once the two focus lines are written, so the cost
 * is a small fraction of a full dump.
 */
object WindowFocusProbe {

    const val COMMAND =
        "dumpsys window displays | grep -m 2 -E 'mCurrentFocus=|mFocusedApp='"

    /** `u0 com.tencent.mm/com.tencent.mm.ui.LauncherUI` → user 0, package com.tencent.mm. */
    private val COMPONENT = Regex("""u(\d+)\s+([A-Za-z][A-Za-z0-9_]*(?:\.[A-Za-z0-9_]+)+)/""")

    fun parse(dump: String): ForegroundWindow? {
        // The focused *activity* is preferred over mCurrentFocus: transient windows such as
        // dialogs, the permission controller or an IME can own the focus while the user is
        // still reading the chat behind them, and hiding the bubble for those would flicker.
        for (key in PREFERRED_KEYS) {
            val line = dump.lineSequence().firstOrNull { it.contains(key) } ?: continue
            val window = parseLine(line) ?: continue
            return window
        }
        return null
    }

    private fun parseLine(line: String): ForegroundWindow? {
        if (line.contains("=null")) return null
        val match = COMPONENT.find(line) ?: return null
        val userId = match.groupValues[1].toIntOrNull() ?: return null
        val packageName = match.groupValues[2]
        if (packageName.isBlank()) return null
        return ForegroundWindow(packageName, userId)
    }

    private val PREFERRED_KEYS = listOf("mFocusedApp=", "mCurrentFocus=")
}
