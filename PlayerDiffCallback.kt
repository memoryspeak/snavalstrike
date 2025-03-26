package ru.snavalstrike.app

import androidx.recyclerview.widget.DiffUtil

class PlayerDiffCallback(
    private val oldList: List<Player>,
    private val newList: List<Player>
) : DiffUtil.Callback() {

    override fun getOldListSize(): Int = oldList.size
    override fun getNewListSize(): Int = newList.size

    // Проверка, совпадают ли объекты (например, по id)
    override fun areItemsTheSame(oldPos: Int, newPos: Int): Boolean {
        return oldList[oldPos].id == newList[newPos].id
    }

    // Проверка, совпадает ли содержимое объекта
    override fun areContentsTheSame(oldPos: Int, newPos: Int): Boolean {
        return oldList[oldPos] == newList[newPos]
    }
}