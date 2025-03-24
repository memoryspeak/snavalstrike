package ru.snavalstrike.app

import android.content.SharedPreferences

object Singleton {
    var DEBUG = true

    lateinit var sharedPreferences: SharedPreferences
    lateinit var editor: SharedPreferences.Editor
    var username: String = ""
    var elo: Int = 1500
    var scene: String = "main"

    const val SERVER_IP: String = "192.168.10.9"
    const val SERVER_PORT: Int = 32951
}