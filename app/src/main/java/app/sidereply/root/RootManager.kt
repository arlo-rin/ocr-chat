package app.sidereply.root

import android.app.AppOpsManager
import android.content.Context
import android.os.Process
import android.provider.Settings

/**
 * Root is used for two things only:
 *  - the one-time appops grants for the overlay and UsageStats foreground detection;
 *  - the `screencap` command when the user taps the bubble.
 *
 * If the two appops grants already exist, the user can start the bubble without another
 * root prompt. Root is then re-used (and verified by the su manager) on the first capture.
 */
object RootManager {

    enum class SetupError {
        ROOT_DENIED,
        ROOT_TIMEOUT,
        OVERLAY_NOT_GRANTED,
        USAGE_NOT_GRANTED,
        SU_NOT_FOUND,
        COMMAND_FAILED,
    }

    class SetupException(
        val error: SetupError,
        cause: Throwable? = null,
    ) : Exception(error.name, cause)

    fun canDrawOverlays(context: Context): Boolean = Settings.canDrawOverlays(context)

    fun hasUsageAccess(context: Context): Boolean {
        val appOps = context.getSystemService(AppOpsManager::class.java) ?: return false
        val mode = appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName,
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun isReady(context: Context): Boolean =
        canDrawOverlays(context) && hasUsageAccess(context)

    /**
     * Grants SYSTEM_ALERT_WINDOW and GET_USAGE_STATS through `su`. This is only called
     * when one of those appops is missing; normal starts do not invoke su.
     */
    fun grantPermissionsViaRoot(context: Context) {
        val packageName = context.packageName
        val command = buildString {
            append("appops set ")
            append(packageName)
            append(" SYSTEM_ALERT_WINDOW allow; ")
            append("appops set ")
            append(packageName)
            append(" GET_USAGE_STATS allow; ")
            append("id")
        }

        val output = try {
            RootShell.runText(command, timeoutMs = 30_000L)
        } catch (e: RootShell.RootException) {
            val mapped = when (e.error) {
                RootShell.Error.SU_NOT_FOUND -> SetupError.SU_NOT_FOUND
                RootShell.Error.TIMEOUT -> SetupError.ROOT_TIMEOUT
                RootShell.Error.COMMAND_FAILED -> SetupError.COMMAND_FAILED
            }
            throw SetupException(mapped, e)
        }

        if (!output.contains("uid=0")) {
            throw SetupException(SetupError.ROOT_DENIED)
        }

        if (!waitFor { canDrawOverlays(context) }) {
            throw SetupException(SetupError.OVERLAY_NOT_GRANTED)
        }
        if (!waitFor { hasUsageAccess(context) }) {
            throw SetupException(SetupError.USAGE_NOT_GRANTED)
        }
    }

    private fun waitFor(timeoutMs: Long = 2_500L, condition: () -> Boolean): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (condition()) return true
            try {
                Thread.sleep(120)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                return false
            }
        }
        return condition()
    }
}
