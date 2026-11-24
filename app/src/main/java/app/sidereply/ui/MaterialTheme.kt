package app.sidereply.ui

import android.content.Context
import android.view.ContextThemeWrapper
import app.sidereply.R
import com.google.android.material.color.DynamicColors

/**
 * Overlay windows are not activities, so they do not receive the dynamic-color
 * theme overlay applied in [DynamicColors.applyToActivitiesIfAvailable]. This
 * wraps the Material 3 theme and then merges that same overlay, which only
 * replaces color roles and leaves component styles intact.
 */
fun Context.materialDynamicContext(): Context {
    val themed = ContextThemeWrapper(this, R.style.Theme_SideReply)
    if (!DynamicColors.isDynamicColorAvailable()) return themed
    val attrs = themed.obtainStyledAttributes(
        intArrayOf(com.google.android.material.R.attr.dynamicColorThemeOverlay),
    )
    val overlay = attrs.getResourceId(0, 0)
    attrs.recycle()
    if (overlay != 0) {
        themed.theme.applyStyle(overlay, true)
    }
    return themed
}
