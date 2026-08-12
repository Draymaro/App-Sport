package com.example.notification

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity

class WorkoutReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val routineId = intent.getLongExtra("ROUTINE_ID", 0L)
        val routineTitle = intent.getStringExtra("ROUTINE_TITLE") ?: "C'est l'heure de votre séance !"

        NotificationHelper.createNotificationChannel(context)

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("LAUNCH_ROUTINE_ID", routineId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            routineId.toInt(),
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, NotificationHelper.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Rappel FitProgress 🏋️‍♂️")
            .setContentText("Séance programmée : $routineTitle. Prêt à tout donner ?")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(routineId.toInt() + 1000, builder.build())
        } catch (e: SecurityException) {
            // Notification permission might be missing on Android 13+
        }
    }
}
