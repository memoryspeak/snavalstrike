package ru.snavalstrike.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView

class PlayersRecyclerViewAdapter(
    private val players: MutableList<Player>,
    private val fragmentManager: FragmentManager,
    private val sendMessageToServer: (String) -> Unit
) : RecyclerView.Adapter<PlayersRecyclerViewAdapter.PlayersViewHolder>() {

    fun updatePlayers(newPlayers: List<Player>) {
        val diffResult = DiffUtil.calculateDiff(PlayerDiffCallback(players, newPlayers))
        players.clear()
        players.addAll(newPlayers)
        diffResult.dispatchUpdatesTo(this)
    }

    fun addPlayer(player: Player) {
        players.add(player)
        notifyItemInserted(players.size - 1)
        updatePlayers(sortedPlayersList())
    }

    fun removePlayer(username: String) {
        val position = players.indexOfFirst { it.username == username }
        if (position != -1) {
            players.removeAt(position)
            notifyItemRemoved(position)
            updatePlayers(sortedPlayersList())
        }
    }

    fun updatePlayer(username: String, elo: Int, isOnline: Boolean, isBusy: Boolean) {
        val position = players.indexOfFirst { it.username == username }
        if (position != -1) {
            val id = players[position].id
            players[position] = Player(id, username, elo, isOnline, isBusy)
            notifyItemChanged(position)
            updatePlayers(sortedPlayersList())
        }
    }

    private fun sortedPlayersList(): List<Player> {
        // Определяем порядок сортировки символов
        val order = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz-_"
        val charOrder = order.associate { it to order.indexOf(it) }

        // Компаратор для сравнения username
        val usernameComparator = object : Comparator<String> {
            override fun compare(s1: String, s2: String): Int {
                val minLength = minOf(s1.length, s2.length)
                for (i in 0 until minLength) {
                    val order1 = charOrder[s1[i]] ?: error("Invalid char: ${s1[i]}")
                    val order2 = charOrder[s2[i]] ?: error("Invalid char: ${s2[i]}")
                    if (order1 != order2) return order1 - order2
                }
                return s1.length - s2.length
            }
        }

        // Создаём глубокую копию
        val sortedList = players.map { it.copy() }.toMutableList()

        // Сортируем по статусу, занятости и имени
        sortedList.sortWith(Comparator { p1, p2 ->
            when {
                // Сначала онлайн игроки
                p1.isOnline != p2.isOnline -> if (p1.isOnline) -1 else 1
                // Затем сортировка по занятости и имени
                else -> {
                    when {
                        p1.isBusy != p2.isBusy -> if (p2.isBusy) -1 else 1
                        else -> usernameComparator.compare(p1.username, p2.username)
                    }
                }
            }
        })

        return sortedList
    }

    class PlayersViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val playerItem: LinearLayout = itemView.findViewById(R.id.player_item)
        val playerIcon: ImageView = itemView.findViewById(R.id.player_icon)
        val playerUsername: TextView = itemView.findViewById(R.id.player_username)
        val playerElo: TextView = itemView.findViewById(R.id.player_elo)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlayersViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.players_recyclerview_item, parent, false)
        return PlayersViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: PlayersViewHolder, position: Int) {
        val user = players[position]
        val username = user.username
        val elo = user.elo
        val isOnline = user.isOnline
        val isBusy = user.isBusy

        if (isOnline) {
            if (isBusy) {
                holder.playerIcon.setImageResource(R.drawable.player_busy)
            } else {
                holder.playerIcon.setImageResource(R.drawable.player_online)
            }
        } else {
            holder.playerIcon.setImageResource(R.drawable.player_offline)
        }
        holder.playerUsername.text = username
        holder.playerElo.text = elo.toString()

        holder.playerItem.setOnClickListener { view ->
            if (isOnline && !isBusy) {
                //val dialog = DialogNewGame(username, elo, isPositiveButton = false, sendMessageToServer)
                //dialog.show(fragmentManager, "NEW_GAME")
                sendMessageToServer("REQUEST_GAME $username END")
            }
        }
    }

    override fun getItemCount() = players.size
}