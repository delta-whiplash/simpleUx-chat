package chat.simplex.matrix

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Spike-only debug screen (#130). Plain framework views on purpose: no Compose,
 * no design system, no MR.strings - this surface never ships to users and is
 * merged exclusively into debug builds. Started via
 * `adb shell am start -n chat.simplex.ux.debug/chat.simplex.matrix.MatrixDebugActivity`.
 *
 * Hardcoded English strings here are a deliberate, documented deviation from
 * the MR.strings rule: the screen is a measurement instrument, not a product
 * surface (it is replaced wholesale in P1).
 */
class MatrixDebugActivity : Activity() {

    private val scope: CoroutineScope = MainScope()
    private lateinit var logView: TextView
    private lateinit var homeserverField: EditText
    private lateinit var usernameField: EditText
    private lateinit var passwordField: EditText
    private lateinit var roomField: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildContent())
        MatrixSpike.onLog = { runOnUiThread { renderLog() } }
        renderLog()
        MatrixSpike.log("spike screen ready - matrix-rust-sdk 26.09.3")
        maybeRunFromIntent()
    }

    /**
     * Test rig: `am start` with extras prefills the fields and, with auto=1,
     * runs login -> sync -> open room unattended (extras never contain real
     * credentials, only local bench accounts).
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
            runCatching {
                if (!MatrixSpike.loggedIn && !MatrixSpike.restoreSession(applicationContext)) {
                    MatrixSpike.loginPassword(applicationContext, hs!!, user!!, pass!!)
                }
                MatrixSpike.startSync()
                if (room != null) {
                    runCatching { MatrixSpike.listDms() }
                    MatrixSpike.openRoom(room)
                }
            }.onFailure { MatrixSpike.log("AUTO-ERROR: ${it.message}") }
        }
    }

    override fun onDestroy() {
        MatrixSpike.onLog = null
        scope.cancel()
        if (isFinishing) with(MatrixSpike) {
            // keep client alive across screen rotations; full shutdown only on back-out
            shutdown()
        }
        super.onDestroy()
    }

    private fun renderLog() {
        if (::logView.isInitialized) logView.text = MatrixSpike.logText()
    }

    private fun buildContent(): ScrollView {
        val pad = (resources.displayMetrics.density * 12).toInt()
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
        }

        fun edit(hint: String, singleLine: Boolean = true, password: Boolean = false): EditText =
            EditText(this).apply {
                this.hint = hint
                setSingleLine(singleLine)
                if (password) inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                setTextColor(Color.WHITE)
                setHintTextColor(Color.GRAY)
            }

        fun button(label: String, onClick: suspend () -> Unit): Button =
            Button(this).apply {
                text = label
                setOnClickListener {
                    scope.launch {
                        runCatching { onClick() }.onFailure { MatrixSpike.log("ERROR: ${it.message}") }
                    }
                }
            }

        val homeserver = edit("homeserver URL, e.g. https://matrix.org").apply { setText("https://matrix.org") }
        val username = edit("username, e.g. delta_test")
        val password = edit("password", password = true)
        val roomId = edit("room id, e.g. !abc:matrix.org (see List DMs)")

        col.addView(homeserver)
        col.addView(username)
        col.addView(password)
        homeserverField = homeserver
        usernameField = username
        passwordField = password
        col.addView(button("Login (password)") {
            MatrixSpike.loginPassword(applicationContext, homeserver.text.toString().trim(), username.text.toString().trim(), password.text.toString())
        })
        col.addView(button("Restore previous session") {
            if (!MatrixSpike.restoreSession(applicationContext)) MatrixSpike.log("no restorable session")
        })
        col.addView(button("Logout + wipe local data") {
            MatrixSpike.logout(applicationContext)
        })
        col.addView(button("Start sync") { MatrixSpike.startSync() })
        col.addView(button("Stop sync") { MatrixSpike.stopSync() })
        col.addView(button("List DMs") { MatrixSpike.listDms() })
        col.addView(roomId)
        roomField = roomId
        col.addView(button("Open room timeline (E2EE decrypt test)") {
            MatrixSpike.openRoom(roomId.text.toString().trim())
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
