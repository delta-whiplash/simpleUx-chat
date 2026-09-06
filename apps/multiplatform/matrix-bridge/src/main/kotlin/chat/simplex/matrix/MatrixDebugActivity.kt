package chat.simplex.matrix

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.InputType
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import chat.simplex.matrix.adapter.MatrixAdapter
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Debug bench screen (P0 #130, now driving the P1 production-shape engine).
 * Plain framework views on purpose: no Compose, no design system, no
 * MR.strings - this surface never ships to users and lives in debug builds.
 * Started via `adb shell am start -n chat.simplex.ux.debug/chat.simplex.matrix.MatrixDebugActivity`.
 *
 * Hardcoded English strings here are a deliberate, documented deviation from
 * the MR.strings rule: the screen is a measurement instrument, not a product
 * surface.
 */
class MatrixDebugActivity : Activity() {

    private val scope = MainScope()
    private val logBuf = ArrayDeque<String>(500)
    private lateinit var logView: TextView
    private lateinit var homeserverField: EditText
    private lateinit var usernameField: EditText
    private lateinit var passwordField: EditText
    private lateinit var roomField: EditText

    private fun log(line: String) {
        // Mirror to logcat: the in-app log dies with activity recreation, and
        // bench runs span activity switches.
        android.util.Log.d("MatrixBench", line)
        if (logBuf.size == 500) logBuf.removeFirst()
        logBuf.addLast(line)
        runOnUiThread { if (::logView.isInitialized) renderLog() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildContent())
        MatrixBridge.onLog = { line -> log(line) }
        log("bench screen ready - matrix-rust-sdk 26.09.3, P1 engine")
        maybeRunFromIntent()
    }

    /**
     * Test rig: `am start` with extras prefills the fields and, with auto=1,
     * runs login -> sync -> ingest + open room unattended (extras never
     * contain real credentials, only local bench accounts).
     */
    private fun maybeRunFromIntent() {
        val extras = intent.extras ?: return
        val hs = extras.getString("homeserver")
        val user = extras.getString("user")
        val pass = extras.getString("pass")
        val room = extras.getString("room")
        hs?.let { homeserverField.setText(it) }
        user?.let { usernameField.setText(it) }
        pass?.let { passwordField.setText(it) }
        room?.let { roomField.setText(it) }
        if (extras.getString("auto") != "1") return
        scope.launch {
            if (!MatrixBridge.started) {
                if (!MatrixBridge.start(applicationContext)) {
                    MatrixBridge.login(applicationContext, hs!!, user!!, pass!!)
                }
            }
            if (room != null) MatrixBridge.openRoom(room)
        }
    }

    override fun onDestroy() {
        MatrixBridge.onLog = {}
        scope.cancel()
        // The bridge lifecycle belongs to MatrixAppHook (app start), not to this
        // bench screen: leaving it must never stop sync mid-test. Use the on-screen
        // "Stop sync" button for a deliberate stop.
        super.onDestroy()
    }

    private fun renderLog() {
        if (::logView.isInitialized) logView.text = logBuf.joinToString("\n")
    }

    private fun buildContent(): ScrollView {
        val pad = (resources.displayMetrics.density * 12).toInt()
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
        }

        fun edit(hint: String, password: Boolean = false): EditText =
            EditText(this).apply {
                this.hint = hint
                setSingleLine(true)
                if (password) inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                setTextColor(Color.WHITE)
                setHintTextColor(Color.GRAY)
            }

        fun button(label: String, onClick: () -> Unit): Button =
            Button(this).apply {
                text = label
                setOnClickListener { onClick() }
            }

        val homeserver = edit("homeserver URL, e.g. https://matrix.org").apply { setText("https://matrix.org") }
        val username = edit("username, e.g. delta_test")
        val password = edit("password", password = true)
        val roomId = edit("room id, e.g. !abc:matrix.org (ids logged on login)")

        homeserverField = homeserver
        usernameField = username
        passwordField = password
        col.addView(homeserver)
        col.addView(username)
        col.addView(password)
        col.addView(button("Login (password) + ingest") {
            MatrixBridge.login(applicationContext, homeserver.text.toString().trim(), username.text.toString().trim(), password.text.toString())
        })
        col.addView(button("Restore previous session + ingest") {
            if (!MatrixBridge.start(applicationContext)) log("no restorable session")
        })
        col.addView(button("Stop sync") { MatrixBridge.stop() })
        col.addView(button("Logout + wipe local data") { MatrixBridge.logout(applicationContext) })
        col.addView(roomId)
        roomField = roomId
        col.addView(button("Open room timeline (E2EE decrypt + ingestion)") {
            val id = roomId.text.toString().trim()
            MatrixBridge.openRoom(id)
            scope.launch {
                kotlinx.coroutines.delay(2000)
                log("isOpen($id) = ${MatrixBridge.isOpen(id)}; chat id = ${MatrixAdapter.matrixChatId(id)}")
            }
        })
        col.addView(button("Log chat-list ids") {
            val ids = chat.simplex.common.model.ChatModel.chats.value.map { it.id }.filter { it.startsWith("mx|") }
            log("matrix chats in list: ${ids.size} -> ${ids.joinToString()}")
        })

        logView = TextView(this).apply {
            typeface = Typeface.MONOSPACE
            setTextColor(Color.rgb(180, 230, 190))
            textSize = 12f
            setTextIsSelectable(true)
        }
        col.addView(logView, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        return ScrollView(this).apply {
            setBackgroundColor(Color.rgb(16, 20, 18))
            addView(col)
        }
    }
}
