package com.niki914.zafiro.app.ui.content

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.termux.terminal.TerminalSession
import com.termux.terminal.TerminalSessionClient
import com.termux.view.TerminalView
import com.termux.view.TerminalViewClient
import java.io.File

private const val TERMINAL_LOG_TAG = "neckcrack_terminal"

/**
 * NECK CRACK terminal: real PTY shell (/system/bin/sh) rendered by Termux's TerminalView.
 * Sessions run as the app user inside the app sandbox (HOME = files/home).
 */
@Composable
fun TerminalSettingsContent() {
    val context = LocalContext.current
    val controller = remember { TerminalController(context) }
    DisposableEffect(Unit) {
        onDispose { controller.dispose() }
    }
    AndroidView(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        factory = { ctx ->
            TerminalView(ctx, null).apply {
                isFocusable = true
                isFocusableInTouchMode = true
                setTerminalViewClient(controller)
                setTextSize(13)
                post {
                    controller.start(this)
                    requestFocus()
                    (ctx.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)
                        ?.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
                }
            }
        },
    )
}

private class TerminalController(private val context: Context) : TerminalSessionClient, TerminalViewClient {

    private var view: TerminalView? = null
    private var session: TerminalSession? = null
    private var textSize = 13

    fun start(terminalView: TerminalView) {
        view = terminalView
        val existing = session
        if (existing != null) {
            terminalView.attachSession(existing)
            terminalView.updateSize()
            return
        }
        val home = File(context.filesDir, "home").apply { mkdirs() }
        val environment = arrayOf(
            "HOME=" + home.absolutePath,
            "PWD=" + home.absolutePath,
            "TERM=xterm-256color",
            "PATH=/sbin:/system/sbin:/system/bin:/system/xbin:/vendor/bin",
            "LANG=en_US.UTF-8",
        )
        val created = try {
            TerminalSession("/system/bin/sh", home.absolutePath, arrayOf("-i"), environment, 2000, this)
        } catch (e: Exception) {
            Log.e(TERMINAL_LOG_TAG, "failed to create shell session", e)
            null
        }
        if (created == null) return
        session = created
        terminalView.attachSession(created)
        terminalView.updateSize()
    }

    fun dispose() {
        session?.let { runCatching { it.finishIfRunning() } }
        session = null
        view = null
    }

    override fun onTextChanged(changedSession: TerminalSession) {
        view?.onScreenUpdated()
    }

    override fun onTitleChanged(changedSession: TerminalSession) = Unit

    override fun onSessionFinished(finishedSession: TerminalSession) {
        Log.i(TERMINAL_LOG_TAG, "session finished, exit=" + finishedSession.exitStatus)
        view?.onScreenUpdated()
    }

    override fun onCopyTextToClipboard(session: TerminalSession, text: String) {
        val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
        manager.setPrimaryClip(ClipData.newPlainText("terminal", text))
    }

    override fun onPasteTextFromClipboard(session: TerminalSession) {
        val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
        val text = manager.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(context)?.toString() ?: return
        if (text.isEmpty()) return
        val bytes = text.toByteArray()
        session.write(bytes, 0, bytes.size)
    }

    override fun onBell(session: TerminalSession) = Unit

    override fun onColorsChanged(session: TerminalSession) {
        view?.onScreenUpdated()
    }

    override fun onTerminalCursorStateChange(state: Boolean) = Unit

    override fun onScale(scale: Float): Float {
        val next = (textSize * scale).toInt().coerceIn(6, 40)
        if (next == textSize) return 1.0f
        val previous = textSize
        textSize = next
        view?.setTextSize(next)
        return next.toFloat() / previous.toFloat()
    }

    override fun onScaleBegin() = Unit

    override fun onScaleEnd() = Unit

    override fun onSingleTapUp(e: MotionEvent) {
        val target = view ?: return
        target.requestFocus()
        (context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)
            ?.showSoftInput(target, InputMethodManager.SHOW_IMPLICIT)
    }

    override fun shouldBackButtonBeMappedToEscape(): Boolean = false

    override fun shouldEnforceCharBasedInput(): Boolean = true

    override fun shouldUseCtrlSpaceWorkaround(): Boolean = false

    override fun isTerminalViewSelected(): Boolean = true

    override fun copyModeChanged(copyMode: Boolean) = Unit

    override fun onKeyDown(keyCode: Int, e: KeyEvent, session: TerminalSession): Boolean = false

    override fun onKeyUp(keyCode: Int, e: KeyEvent): Boolean = false

    override fun onLongPress(event: MotionEvent): Boolean = false

    override fun readControlKey(): Boolean = false

    override fun readAltKey(): Boolean = false

    override fun readShiftKey(): Boolean = false

    override fun readFnKey(): Boolean = false

    override fun onCodePoint(codePoint: Int, ctrlDown: Boolean, session: TerminalSession): Boolean = false

    override fun onEmulatorSet() {
        view?.updateSize()
    }

    override fun logError(tag: String, message: String) {
        Log.e(tag, message)
    }

    override fun logWarn(tag: String, message: String) {
        Log.w(tag, message)
    }

    override fun logInfo(tag: String, message: String) {
        Log.i(tag, message)
    }

    override fun logDebug(tag: String, message: String) {
        Log.d(tag, message)
    }

    override fun logVerbose(tag: String, message: String) {
        Log.v(tag, message)
    }

    override fun logStackTraceWithMessage(tag: String, message: String, e: Exception) {
        Log.e(tag, message, e)
    }

    override fun logStackTrace(tag: String, e: Exception) {
        Log.e(tag, "stack trace", e)
    }
}
