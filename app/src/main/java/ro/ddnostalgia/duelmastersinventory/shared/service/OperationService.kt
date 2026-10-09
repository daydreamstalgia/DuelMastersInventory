package ro.ddnostalgia.duelmastersinventory.shared.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class OperationService : LifecycleService() {

    companion object {
        private const val CHANNEL_ID = "sync_channel"
        private const val NOTIFICATION_ID = 1

        fun runInNotifications(context: Context, action: suspend () -> Unit) {
            val intent = Intent(context, OperationService::class.java)
            context.startForegroundService(intent)
            ActionHolder.action = action
        }
    }

    private object ActionHolder {
        var action: (suspend () -> Unit)? = null
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private val job = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + job)


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        startForeground(NOTIFICATION_ID, buildNotification("Sync in progress..."))

        serviceScope.launch {
            try {
                ActionHolder.action?.invoke()
                updateNotification("Sync completed")
            } catch (e: Exception) {
                updateNotification("Sync failed: ${e.message}")
            } finally {
                delay(1500)
                stopForeground(true)
                stopSelf()
            }
        }

        return START_NOT_STICKY

    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID, "Sync Operations", NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(content: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("App Sync")
            .setContentText(content)
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(content: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification(content))
    }
}
