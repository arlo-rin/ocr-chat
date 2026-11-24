package app.sidereply.root

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WindowFocusProbeTest {

    @Test
    fun readsTheFocusedActivityOfTheMainUser() {
        val dump = """
              Display: mDisplayId=0
                mCurrentFocus=Window{8f1b2c3 u0 com.tencent.mm/com.tencent.mm.ui.LauncherUI}
                mFocusedApp=ActivityRecord{e2c1a4 u0 com.tencent.mm/.ui.LauncherUI t42}
        """.trimIndent()

        val window = WindowFocusProbe.parse(dump)

        assertEquals(ForegroundWindow("com.tencent.mm", 0), window)
        assertTrue(window?.isDualSpace == false)
    }

    @Test
    fun recognizesACloneSpaceWindow() {
        // 「应用双开」 copies run in a separate Android user (999 on most ROMs) but keep the
        // package name, so only the user id reveals that this is the dual-space WeChat.
        val dump = "  mFocusedApp=ActivityRecord{91ab u999 com.tencent.mm/.ui.LauncherUI t7}"

        val window = WindowFocusProbe.parse(dump)

        assertEquals(ForegroundWindow("com.tencent.mm", 999), window)
        assertTrue(window?.isDualSpace == true)
    }

    @Test
    fun focusedActivityWinsOverTransientFocusWindows() {
        // A permission dialog or IME can own mCurrentFocus while the chat is still on screen.
        val dump = """
            mCurrentFocus=Window{3fa u0 com.android.permissioncontroller/com.android.permissioncontroller.permission.ui.GrantPermissionsActivity}
            mFocusedApp=ActivityRecord{5cd u999 com.tencent.mm/.ui.LauncherUI t7}
        """.trimIndent()

        assertEquals(ForegroundWindow("com.tencent.mm", 999), WindowFocusProbe.parse(dump))
    }

    @Test
    fun fallsBackToCurrentFocusWhenNoActivityIsRecorded() {
        val dump = "  mCurrentFocus=Window{77a u0 com.miui.home/com.miui.home.launcher.Launcher}"

        assertEquals(ForegroundWindow("com.miui.home", 0), WindowFocusProbe.parse(dump))
    }

    @Test
    fun handlesEmptyFocus() {
        assertNull(WindowFocusProbe.parse("mCurrentFocus=null\nmFocusedApp=null"))
    }

    @Test
    fun ignoresUnrelatedDumpOutput() {
        assertNull(WindowFocusProbe.parse("Display: mDisplayId=0\n  init=1080x2400 420dpi"))
        assertNull(WindowFocusProbe.parse(""))
    }
}
