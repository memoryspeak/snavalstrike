package ru.snavalstrike.app

data class Player(
    val id: String, // server database users table id
    //val icon: Int,
    val isOnline: Boolean,
    val username: String,
    val elo: Int
)
