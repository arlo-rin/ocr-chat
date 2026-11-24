package app.sidereply.root

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Runs a deliberately small set of commands through the device's `su` binary.
 *
 * Screenshots are read from stdout into memory and are never written to disk. stdout and
 * stderr are drained on separate threads so a large PNG cannot fill the process pipe.
 */
object RootShell {

    enum class Error {
        SU_NOT_FOUND,
        TIMEOUT,
        COMMAND_FAILED,
    }

    class RootException(
        val error: Error,
        message: String,
        cause: Throwable? = null,
    ) : Exception(message, cause)

    data class Result(
        val exitCode: Int,
        val stdout: ByteArray,
        val stderr: ByteArray,
    ) {
        fun stdoutText(): String = String(stdout, Charsets.UTF_8).trim()
        fun stderrText(): String = String(stderr, Charsets.UTF_8).trim()
    }

    fun run(command: String, timeoutMs: Long = 20_000L): Result {
        val primary = runOnce(arrayOf("su", "-c", command), timeoutMs)
        if (primary.exitCode == 0) return primary

        // AOSP's userdebug su takes a uid before the command; Magisk/KernelSU use -c.
        // Only try that syntax when the first attempt looks like an option/usage error,
        // so a genuine permission denial does not trigger a second su prompt.
        val firstError = primary.stderrText().lowercase()
        val looksLikeAospSu = firstError.contains("invalid uid/gid") ||
            firstError.contains("unknown option") ||
            firstError.contains("usage: su")
        if (!looksLikeAospSu) return primary

        val fallback = runOnce(arrayOf("su", "0", "sh", "-c", command), timeoutMs)
        return if (fallback.exitCode == 0) fallback else primary
    }

    private fun runOnce(args: Array<String>, timeoutMs: Long): Result {
        val process = try {
            ProcessBuilder(*args)
                .redirectErrorStream(false)
                .start()
        } catch (e: IOException) {
            throw RootException(
                Error.SU_NOT_FOUND,
                "未找到 su。请确认手机已 Root，并在 Root 管理器中允许侧答。",
                e,
            )
        }

        val stdout = ByteArrayOutputStream()
        val stderr = ByteArrayOutputStream()
        val stdoutThread = Thread({
            try {
                process.inputStream.use { it.copyTo(stdout) }
            } catch (_: IOException) {
                // Process destroyed after a timeout.
            }
        }, "side-reply-root-out").apply {
            isDaemon = true
            start()
        }
        val stderrThread = Thread({
            try {
                process.errorStream.use { it.copyTo(stderr) }
            } catch (_: IOException) {
                // Process destroyed after a timeout.
            }
        }, "side-reply-root-err").apply {
            isDaemon = true
            start()
        }

        val finished = try {
            process.waitFor(timeoutMs, TimeUnit.MILLISECONDS)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            false
        }

        if (!finished) {
            process.destroyForcibly()
            stdoutThread.join(1_000)
            stderrThread.join(1_000)
            throw RootException(
                Error.TIMEOUT,
                "等待 Root 授权超时。请在 Root 管理器中允许侧答后重试。",
            )
        }

        stdoutThread.join(2_000)
        stderrThread.join(2_000)
        return Result(process.exitValue(), stdout.toByteArray(), stderr.toByteArray())
    }

    fun runText(command: String, timeoutMs: Long = 30_000L): String {
        val result = run(command, timeoutMs)
        if (result.exitCode != 0) {
            val detail = result.stderrText()
                .ifBlank { result.stdoutText() }
                .take(200)
            throw RootException(
                Error.COMMAND_FAILED,
                if (detail.isBlank()) "Root 命令执行失败" else detail,
            )
        }
        return result.stdoutText()
    }

    fun capturePng(timeoutMs: Long = 15_000L): ByteArray {
        val result = run("screencap -p", timeoutMs)
        if (result.exitCode != 0) {
            val detail = result.stderrText()
                .ifBlank { "请确认 Root 权限仍然有效" }
                .take(200)
            throw RootException(Error.COMMAND_FAILED, "Root 截图失败：$detail")
        }
        return extractPng(result.stdout)
            ?: throw RootException(
                Error.COMMAND_FAILED,
                "Root 截图没有返回有效图像，请确认系统允许截屏",
            )
    }

    private fun extractPng(bytes: ByteArray): ByteArray? {
        val signature = PNG_SIGNATURE
        if (bytes.size < signature.size) return null
        if (signature.indices.all { bytes[it] == signature[it] }) return bytes

        // Some su implementations print a short notice before the binary payload.
        val lateStart = bytes.size - signature.size
        val limit = minOf(lateStart, 1024)
        for (start in 0..limit) {
            if (signature.indices.all { bytes[start + it] == signature[it] }) {
                return bytes.copyOfRange(start, bytes.size)
            }
        }
        return null
    }

    private val PNG_SIGNATURE = byteArrayOf(
        0x89.toByte(),
        0x50,
        0x4E,
        0x47,
        0x0D,
        0x0A,
        0x1A,
        0x0A,
    )
}
