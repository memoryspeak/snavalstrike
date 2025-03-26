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
        diffResult.dispatchUpdatesTo(this) // this → ваш адаптер
    }

    // Добавление
    fun addPlayer(player: Player) {
        players.add(player)
        notifyItemInserted(players.size - 1)
    }

    // Удаление
    fun removePlayer(position: Int) {
        players.removeAt(position)
        notifyItemRemoved(position)
    }

    // Обновление
    fun updatePlayer(position: Int, newPlayer: Player) {
        players[position] = newPlayer
        notifyItemChanged(position)
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
        val id = user.id
        val icon = user.icon
        val username = user.username
        val elo = user.elo

        holder.playerIcon.setImageResource(icon)
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