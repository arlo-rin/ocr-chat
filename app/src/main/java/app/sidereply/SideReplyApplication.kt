package app.sidereply

import android.app.Application
import com.google.android.material.color.DynamicColors

class SideReplyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Android 12+ only. API 30 keeps the static tonal palette in the theme.
        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}
