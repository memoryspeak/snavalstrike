package ru.snavalstrike.app

import android.os.Bundle
import android.content.Context
import android.content.pm.ActivityInfo
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
import android.view.View
import android.widget.Toast
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.util.concurrent.LinkedBlockingQueue
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var socketThread: SocketThread? = null
    private val playerRecyclerViewAdapter: PlayersRecyclerViewAdapter = PlayersRecyclerViewAdapter(mutableListOf(), supportFragmentManager, ::sendMessageToServer)
    private var gameView: GameView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        Singleton.sharedPreferences = getSharedPreferences("snavalStrikeSettings", Context.MODE_PRIVATE)
        Singleton.editor = Singleton.sharedPreferences.edit() ?: return
        super.onCreate(savedInstanceState)
        Singleton.scene = "splash"
        reDraw(Singleton.scene)
        startSocketConnection()
    }

    override fun onResume() {
        super.onResume()
        if (Singleton.scene == "game") {
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (Singleton.scene == "main") menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (Singleton.scene != "main") return super.onOptionsItemSelected(item)
        return when (item.itemId) {
            R.id.action_exit_account -> {
                sendMessageToServer("DELETE_USER ${Singleton.username} END")
                sharedPreferencesClear()
                setContentView(R.layout.activity_login)
                Singleton.scene = "login"
                reDraw(Singleton.scene)
                true
            }
            R.id.action_about_game -> {
                // TODO
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun reDraw(scene: String) {
        val dialogFragment = supportFragmentManager.findFragmentByTag("NEW_GAME") as? DialogNewGame
        dialogFragment?.dismiss()
        when (scene) {
            "main" -> {
                setContentView(R.layout.activity_main)
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
                val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar_main)
                setSupportActionBar(toolbar)
                val subtitle = "${Singleton.username} (${Singleton.elo})"
                toolbar.subtitle = subtitle
                supportActionBar?.subtitle = subtitle
                toolbar.invalidate()

                val recyclerView: RecyclerView = findViewById(R.id.playersRecyclerView)
                recyclerView.layoutManager = LinearLayoutManager(this)
                recyclerView.adapter = playerRecyclerViewAdapter
                playerRecyclerViewAdapter.updatePlayers(listOf())
                sendMessageToServer("GET_PLAYERS END")
            }
            "login" -> {
                setContentView(R.layout.activity_login)
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
                val editUsername = findViewById<EditText>(R.id.activity_login_edit_username)
                val loginButton = findViewById<Button>(R.id.activity_login_button)
                loginButton.setOnClickListener {
                    val username = editUsername.text.toString()
                    if (username == "") return@setOnClickListener
                    sendMessageToServer("ADD_USER $username END")
                }
            }
            "reconnect" -> {
                setContentView(R.layout.activity_reconnect)
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
                val reconnectButton = findViewById<Button>(R.id.activity_reconnect_button)
                reconnectButton.setOnClickListener {
                    Singleton.scene = "splash"
                    reDraw(Singleton.scene)
                    reconnectManually()
                }
            }
            "splash" -> {
                setContentView(R.layout.activity_splash)
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
            }
            "game" -> {
                setContentView(R.layout.activity_game)
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
                gameView = findViewById(R.id.game_view)
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

        private fun reDrawLoginPage() {
            uiHandler.post {
                sharedPreferencesClear()
                Singleton.scene = "login"
                reDraw(Singleton.scene)
            }
        }

        private fun startReader() {
            Thread {
                try {
                    while (running) {
                        readMessage()?.let { message ->
                            if (Singleton.DEBUG) notifyMessage(message)
                            when (message.split(" ")[0]) { // METHOD
                                "HEARTBEAT" -> todo()
                                "ADD_USER" -> {
                                    val status = message.split(" ")[1]
                                    val username = message.split(" ")[2]
                                    when (status) {
                                        "ok" -> uiHandler.post {
                                            Singleton.editor.putString("username", username)
                                            Singleton.editor.apply()
                                            Singleton.editor.putInt("elo", 1500)
                                            Singleton.editor.apply()
                                            Singleton.username = username
                                            Singleton.elo = 1500
                                            Singleton.scene = "main"
                                            reDraw(Singleton.scene)
                                        }
                                        "alreadyinuse" -> {
                                            reDrawLoginPage()
                                            notifyMessage("Username `${username}` already in use")
                                        }
                                        "error" -> {
                                            reDrawLoginPage()
                                            notifyMessage("Error add user `${username}`")
                                        }
                                        else -> {
                                            reDrawLoginPage()
                                            notifyMessage("Username `${username}` add user undefined status")
                                        }
                                    }
                                }
                                "DELETE_USER" -> {
                                    val status = message.split(" ")[1]
                                    val username = message.split(" ")[2]
                                    when (status) {
                                        "ok" -> if (Singleton.DEBUG) notifyMessage("Delete user `${username}` successfully") else todo()
                                        "dontfind" -> notifyMessage("Username `${username}` don't find to delete")
                                        "error" -> notifyMessage("Error delete user `${username}`")
                                        else -> notifyMessage("Username `${username}` delete user undefined status")
                                    }
                                }
                                "LOGIN" -> {
                                    val status = message.split(" ")[1]
                                    val username = message.split(" ")[2]
                                    val elo = message.split(" ")[3].toInt()
                                    when (status) {
                                        "ok" -> uiHandler.post {
                                            Singleton.editor.putString("username", username)
                                            Singleton.editor.apply()
                                            Singleton.editor.putInt("elo", elo)
                                            Singleton.editor.apply()
                                            Singleton.username = username
                                            Singleton.elo = elo
                                            Singleton.scene = "main"
                                            reDraw(Singleton.scene)
                                        }
                                        "doesnotexist" -> {
                                            reDrawLoginPage()
                                            notifyMessage("Username `${username}` does not exist")
                                        }
                                        "error" -> {
                                            reDrawLoginPage()
                                            notifyMessage("Error login `${username}`")
                                        }
                                        else -> {
                                            reDrawLoginPage()
                                            notifyMessage("Username `${username}` login undefined status")
                                        }
                                    }
                                }
                                "ADD_PLAYER" -> runOnUiThread {
                                    playerRecyclerViewAdapter.addPlayer(Player(
                                        id = message.split(" ")[1],
                                        username = message.split(" ")[2],
                                        elo = message.split(" ")[3].toInt(),
                                        isOnline = message.split(" ")[4].toInt() == 1,
                                        isBusy = message.split(" ")[5].toInt() == 1)
                                    )
                                }
                                "DELETE_PLAYER" -> runOnUiThread {
                                    playerRecyclerViewAdapter.removePlayer(
                                        username = message.split(" ")[1]
                                    )
                                }
                                "UPDATE_PLAYER" -> runOnUiThread {
                                    playerRecyclerViewAdapter.updatePlayer(
                                        username = message.split(" ")[1],
                                        elo = message.split(" ")[2].toInt(),
                                        isOnline = message.split(" ")[3].toInt() == 1,
                                        isBusy = message.split(" ")[4].toInt() == 1
                                    )
                                }
                                "REQUEST_GAME" -> {
                                    val usernameA = message.split(" ")[1]
                                    val eloA = message.split(" ")[2].toInt()
                                    val usernameB = message.split(" ")[3]
                                    val eloB = message.split(" ")[4].toInt()
                                    when (Singleton.username) {
                                        usernameA -> DialogNewGame(usernameB, eloB, isPositiveButton = false, ::sendMessageToServer).show(supportFragmentManager, "NEW_GAME")
                                        usernameB -> DialogNewGame(usernameA, eloA, isPositiveButton = true, ::sendMessageToServer).show(supportFragmentManager, "NEW_GAME")
                                        else -> notifyMessage("REQUEST_GAME. I'm not usernameA and usernameB. It's impossible!!!")
                                    }
                                }
                                "RESPONSE_GAME" -> {
                                    val usernameA = message.split(" ")[1]
                                    val eloA = message.split(" ")[2].toInt()
                                    val usernameB = message.split(" ")[3]
                                    val eloB = message.split(" ")[4].toInt()
                                    when (message.split(" ")[5]) { // status
                                        "ok" -> uiHandler.post {
                                            Singleton.scene = "game"
                                            reDraw(Singleton.scene)
                                        }
                                        "bad" -> {
                                            if (Singleton.scene == "game") {
                                                gameView?.currentScene = "game_over"
                                            } else {
                                                val dialogFragment = supportFragmentManager.findFragmentByTag("NEW_GAME") as? DialogNewGame
                                                dialogFragment?.dismiss() ?: todo()
                                            }
                                        }
                                        else -> notifyMessage("RESPONSE_GAME. Status is not OK or BAD. It's impossible!!!")
                                    }
                                }
                                else -> if (Singleton.DEBUG) notifyMessage("Unknown method") else todo()
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
            sendHeartbeat()
            "HEARTBEAT"
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
                        message,
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
                        error ?: "error",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        private fun todo() {
            uiHandler.post {}
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
                sendMessageToServer("LOGIN ${Singleton.username} END")
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


    @Deprecated("Deprecated in Java", ReplaceWith("super.onBackPressed()", "androidx.appcompat.app.AppCompatActivity"))
    override fun onBackPressed() {
        if (Singleton.scene == "game") {
            gameView?.currentScene = "exit"
        } else super.onBackPressed()
    }
}