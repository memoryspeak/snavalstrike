package ru.snavalstrike.app

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment

class DialogNewGame(
    private val username: String,
    private val elo: Int,
    private val isPositiveButton: Boolean,
    private val sendMessageToServer: (String) -> Unit) : DialogFragment() {

    private fun onClickPositiveButton() {
        sendMessageToServer("RESPONSE_GAME $username ok END")
    }

    private fun onClickNegativeButton() {
        sendMessageToServer("RESPONSE_GAME $username bad END")
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            val inflater = requireActivity().layoutInflater
            val view = inflater.inflate(R.layout.dialog_fragment_new_game, null)
            builder
                .setTitle("(${elo}) $username")
                .setIcon(R.drawable.player_busy)
                .setView(view)
                .setNegativeButton(android.R.string.cancel) { dialog, _ -> onClickNegativeButton() } // onClickNegativeButton()
            if (isPositiveButton) builder.setPositiveButton(android.R.string.ok) { dialog, _ -> onClickPositiveButton() } // onClickPositiveButton()
            val dialog = builder.create()
            //dialog.window?.setBackgroundDrawableResource(R.color.blue_basic_light_min)
            dialog
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        onClickNegativeButton()
    }
}