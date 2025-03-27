package ru.snavalstrike.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView

class PlayersRecyclerViewAdapter(private val players: MutableList<Player>) : RecyclerView.Adapter<PlayersRecyclerViewAdapter.PlayersViewHolder>() {

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
        players.removeAt(position)
        notifyItemRemoved(position)
        updatePlayers(sortedPlayersList())
    }

    fun updatePlayer(username: String, elo: Int, isOnline: Boolean) {
        val position = players.indexOfFirst { it.username == username }
        val id = players[position].id
        players[position] = Player(id, isOnline, username, elo)
        notifyItemChanged(position)
        updatePlayers(sortedPlayersList())
    }

    private fun sortedPlayersList(): List<Player> {
        // Определяем порядок сортировки символов
        val order = "-_abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
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

        // Сортируем по статусу и имени
        sortedList.sortWith(Comparator { p1, p2 ->
            when {
                // Сначала онлайн игроки
                p1.isOnline != p2.isOnline -> if (p1.isOnline) -1 else 1
                // Затем сортировка по имени
                else -> usernameComparator.compare(p1.username, p2.username)
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
        //val id = user.id
        //val icon = user.icon
        val isOnline = user.isOnline
        val username = user.username
        val elo = user.elo

        if (isOnline) {
            holder.playerIcon.setImageResource(R.drawable.player_online)
        } else {
            holder.playerIcon.setImageResource(R.drawable.player_offline)
        }
        holder.playerUsername.text = username
        holder.playerElo.text = elo.toString()

        holder.playerItem.setOnClickListener { view ->
            //val intent = Intent(view.context, ContentActivity::class.java)
            //intent.putExtra("icon", icon);
            //intent.putExtra("name", name);
            //view.context.startActivity(intent)
        }
    }

    override fun getItemCount() = players.size
}