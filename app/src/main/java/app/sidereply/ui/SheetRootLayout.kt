package app.sidereply.ui

import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import android.widget.FrameLayout

/**
 * Full-screen transparent root for the assistant window. It closes itself when the
 * system Back key is pressed; touches outside the card are handled by the service.
 */
class SheetRootLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    var onBackPressed: (() -> Unit)? = null

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_BACK) {
            if (event.action == KeyEvent.ACTION_UP && !event.isCanceled) {
                onBackPressed?.invoke()
            }
            return true
        }
        return super.dispatchKeyEvent(event)
    }
}
