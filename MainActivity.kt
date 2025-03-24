package ru.snavalstrike.app

import android.os.Bundle
import android.content.Context
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import java.lang.Exception
import java.net.Socket
import java.net.SocketTimeoutException
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.util.concurrent.LinkedBlockingQueue

class MainActivity : AppCompatActivity() {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var socketThread: SocketThread? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        Singleton.sharedPreferences = getSharedPreferences("snavalStrikeSettings", Context.MODE_PRIVATE)
        Singleton.editor = Singleton.sharedPreferences.edit() ?: return
        super.onCreate(savedInstanceState)
        Singleton.scene = "splash"
        reDraw(Singleton.scene)
        startSocketConnection()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (Singleton.scene == "main") menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (Singleton.scene != "main") return super.onOptionsItemSelected(item)
        return when (item.itemId) {
            R.id.action_exit_account -> {
                Singleton.editor.putString("username", "")
                Singleton.editor.apply()

                Singleton.username = ""
                setContentView(R.layout.activity_login)
                Singleton.scene = "login"
                reDraw(Singleton.scene)
                true
            }
            R.id.action_about_game -> {
                // Действие для "О приложении"
                sendMessageToServer("about")
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun reDraw(scene: String) {
        when (scene) {
            "main" -> {
                setContentView(R.layout.activity_main)
                val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar_main)
                setSupportActionBar(toolbar)
                val subtitle = "${Singleton.username} (${Singleton.elo})"
                toolbar.subtitle = subtitle
                supportActionBar?.subtitle = subtitle
                toolbar.invalidate()
            }
            "login" -> {
                setContentView(R.layout.activity_login)
                val editUsername = findViewById<EditText>(R.id.activity_login_edit_username)
                val loginButton = findViewById<Button>(R.id.activity_login_button)
                loginButton.setOnClickListener {
                    val username = editUsername.text.toString()
                    if (username == "") return@setOnClickListener
                    sendMessageToServer("LOGIN $username")
                }
            }
            "reconnect" -> {
                setContentView(R.layout.activity_reconnect)
                val reconnectButton = findViewById<Button>(R.id.activity_reconnect_button)
                reconnectButton.setOnClickListener {
                    Singleton.scene = "splash"
                    reDraw(Singleton.scene)
                    reconnectManually()
                }
            }
            "splash" -> {
                setContentView(R.layout.activity_splash)
            }
        }
    }
    override fun onDestroy() {
        socketThread?.closeConnection()
        super.onDestroy()
    }

    private fun startSocketConnection() {
        socketThread?.closeConnection()
        socketThread = SocketThread(mainHandler).apply {
            start()
        }
    }

    inner class SocketThread(
        private val uiHandler: Handler
    ) : Thread() {
        private var socket: Socket? = null
        private var writer: PrintWriter? = null
        private var reader: BufferedReader? = null
        private val messageQueue = LinkedBlockingQueue<String>()
        private var running = true

        override fun run() {
            try {
                connectToServer()
                startReader()
                uiHandler.post {
                    sharedPreferencesInit()
                    reDraw(Singleton.scene)
                }
                processMessages()
            } catch (e: Exception) {
                notifyError("Connection error: ${e.message}")
            } finally {
                //closeResources()
                uiHandler.post {
                    Singleton.scene = "reconnect"
                    reDraw(Singleton.scene)
                }
            }
        }

        private fun connectToServer() {
            socket = Socket().apply {
                connect(InetSocketAddress(Singleton.SERVER_IP, Singleton.SERVER_PORT), 5000)
                soTimeout = 10000
                keepAlive = true
            }
            writer = PrintWriter(socket!!.getOutputStream(), true)
            reader = BufferedReader(InputStreamReader(socket!!.inputStream))
        }

        private fun startReader() {
            Thread {
                try {
                    while (running) {
                        readMessage()?.let { message ->
                            val method = message.split(" ")[0]
                            when (method) {
                                "HEARTBEAT" -> {
                                    val status = message.split(" ")[1]
                                    if (status == "self") {
                                        sendHeartbeat()
                                    } else notifyMessage(message)
                                }
                                "LOGIN" -> {
                                    val status = message.split(" ")[1]
                                    if (status == "ok") {
                                        val username = message.split(" ")[2]
                                        uiHandler.post {
                                            Singleton.editor.putString("username", username)
                                            Singleton.editor.apply()
                                            Singleton.editor.putInt("elo", 1500)
                                            Singleton.editor.apply()
                                            Singleton.username = username
                                            Singleton.elo = 1500
                                            Singleton.scene = "main"
                                            reDraw(Singleton.scene)
                                        }
                                    } else {
                                        uiHandler.post {
                                            sharedPreferencesClear()
                                            Singleton.scene = "login"
                                            reDraw(Singleton.scene)
                                        }
                                        notifyMessage("Username already in use")
                                    }
                                }
                                else -> notifyMessage("Unknown method")
                            }
                        } ?: break
                    }
                } catch (e: Exception) {
                    notifyError("Read error: ${e.message}")
                } finally {
                    uiHandler.post {
                        Singleton.scene = "reconnect"
                        reDraw(Singleton.scene)
                    }
                }
            }.start()
        }

        private fun readMessage(): String? = try {
            reader?.readLine()
        } catch (e: SocketTimeoutException) {
            "HEARTBEAT self"
        } catch (e: IOException) {
            null
        }

        private fun processMessages() {
            while (running) {
                try {
                    val message = messageQueue.take()
                    sendMessage(message)
                } catch (e: InterruptedException) {
                    break
                }
            }
        }

        fun send(message: String) {
            if (running) messageQueue.put(message)
        }

        private fun sendMessage(message: String) {
            try {
                writer?.run {
                    println(message)
                    flush()
                }
            } catch (e: Exception) {
                notifyError("Send error: ${e.message}")
                uiHandler.post {
                    Singleton.scene = "reconnect"
                    reDraw(Singleton.scene)
                }
            }
        }

        fun closeConnection() {
            running = false
            interrupt()
            closeResources()
        }

        private fun closeResources() {
            try {
                reader?.close()
                writer?.close()
                socket?.close()
            } catch (e: IOException) {
                uiHandler.post {
                    Singleton.scene = "reconnect"
                    reDraw(Singleton.scene)
                }
            }
        }

        private fun notifyMessage(message: String) {
            uiHandler.post {
                if (!isFinishing && !isDestroyed) {
                    Toast.makeText(
                        this@MainActivity,
                        "Received: $message",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        private fun notifyError(error: String?) {
            uiHandler.post {
                if (!isFinishing && !isDestroyed) {
                    Toast.makeText(
                        this@MainActivity,
                        "Error: ${error ?: "Unknown"}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun sendMessageToServer(message: String) {
        if (socketThread != null) {
            socketThread!!.send(message)
        } else {
            Singleton.scene = "reconnect"
            reDraw(Singleton.scene)
        }
    }

    private fun sendHeartbeat() {
        if (socketThread != null) {
            socketThread!!.send("HEARTBEAT")
        } else {
            Singleton.scene = "reconnect"
            reDraw(Singleton.scene)
        }
    }

    private fun sharedPreferencesInit() {
        if (Singleton.sharedPreferences.contains("username")) {
            Singleton.username = Singleton.sharedPreferences.getString("username", "").toString()
            if (Singleton.username == "") {
                Singleton.editor.putInt("elo", 1500)
                Singleton.editor.apply()
                Singleton.elo = 1500
                Singleton.scene = "login"
            } else {
                if (Singleton.sharedPreferences.contains("elo")) {
                    Singleton.elo = Singleton.sharedPreferences.getInt("elo", 1500)
                } else {
                    Singleton.editor.putInt("elo", 1500)
                    Singleton.editor.apply()
                    Singleton.elo = 1500
                }
                sendMessageToServer("LOGIN ${Singleton.username}")
            }
        } else {
            sharedPreferencesClear()
            Singleton.scene = "login"
        }
    }

    private fun sharedPreferencesClear() {
        Singleton.editor.putString("username", "")
        Singleton.editor.apply()
        Singleton.editor.putInt("elo", 1500)
        Singleton.editor.apply()
        Singleton.username = ""
        Singleton.elo = 1500
    }

    private fun reconnectManually() {
        socketThread?.closeConnection()
        startSocketConnection()
    }
}