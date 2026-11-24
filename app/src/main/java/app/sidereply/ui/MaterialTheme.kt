package app.sidereply.ui

import android.content.Context
import android.view.ContextThemeWrapper
import app.sidereply.R

/**
 * Overlay windows are not activities, so they do not automatically use the
 * application theme's resolved night mode together with our widget styles.
 * This wrapper applies [R.style.Theme_SideReply] only. Dynamic color is not
 * added: the static tonal palette is the whole app's color system.
 */
fun Context.materialContext(): Context = ContextThemeWrapper(this, R.style.Theme_SideReply)
