package ru.snavalstrike.app

data class Player(
    val id: String, // server database users table id
    val username: String,
    val elo: Int,
    val isOnline: Boolean,
    val isBusy: Boolean
)
