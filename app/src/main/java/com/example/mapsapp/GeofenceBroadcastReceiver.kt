import android.Manifest
import android.app.IntentService
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.mapsapp.MainActivity
import com.example.mapsapp.R
import com.google.android.gms.location.Geofence

class GeofenceBroadcastReceiver : IntentService("GeofenceBroadcastReceiver") {

    override fun onHandleIntent(intent: Intent?) {
        val geofenceTransition = intent?.let {
            GeofenceTransitionsJobIntentService.getGeofenceTransition(
                it
            )
        }
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            val geofenceId = GeofenceTransitionsJobIntentService.getGeofenceRequestId(intent!!)
            sendNotification(this, "Potwór jest w pobliżu - $geofenceId")
        }
    }
    companion object {
        const val CHANNEL_ID = "geofence_channel"
        const val NOTIFICATION_ID = 1

        private fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val name = "Geofence Channel"
                val descriptionText = "Channel for geofence notifications"
                val importance = NotificationManager.IMPORTANCE_HIGH

                // Uzyskaj obiekt NotificationManager
                val notificationManager: NotificationManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

                // Stwórz kanał powiadomień
                val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                    description = descriptionText
                }

                // Zarejestruj kanał w NotificationManager
                notificationManager.createNotificationChannel(channel)
            }
        }

    }

    private fun sendNotification(context: Context, message: String) {
        createNotificationChannel(context)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Powiadomienie o potworze")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        val notificationIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT
        )
        builder.setContentIntent(pendingIntent)

        // Uzyskaj obiekt NotificationManager z NotificationManagerCompat
        val notificationManager: NotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        with(NotificationManagerCompat.from(context)) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Rozważ wywołanie
                //    ActivityCompat#requestPermissions
                // tutaj, aby poprosić o brakujące uprawnienia, a następnie przesłonić
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // do obsługi przypadku, gdy użytkownik udziela uprawnienia. Zobacz dokumentację
                // dla ActivityCompat#requestPermissions, aby uzyskać więcej informacji.
                return
            }
            notificationManager.notify(NOTIFICATION_ID, builder.build())
        }
    }



}
