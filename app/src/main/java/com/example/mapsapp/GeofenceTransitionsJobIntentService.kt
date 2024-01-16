import android.app.IntentService
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent

class GeofenceTransitionsJobIntentService : IntentService("GeofenceTransitionsJobIntentService") {

    override fun onHandleIntent(intent: Intent?) {
        Log.d("GeofenceReceiver", "GeofenceBroadcastReceiver received intent")
        val geofencingEvent = intent?.let { GeofencingEvent.fromIntent(it) }
        if (geofencingEvent != null) {
            if (geofencingEvent.hasError()) {
                val errorMessage = GeofenceErrorMessages.getErrorString(this, geofencingEvent.errorCode)
                Log.e(TAG, errorMessage)
                return
            }
        }

        val geofenceTransition = geofencingEvent?.geofenceTransition

        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            val triggeringGeofences = geofencingEvent.triggeringGeofences
            if (triggeringGeofences != null) {
                handleEnterTransition(triggeringGeofences)
            }
        }
    }

    private fun handleEnterTransition(triggeringGeofences: List<Geofence>) {
        for (geofence in triggeringGeofences) {
            val requestId = geofence.requestId
            Log.d(TAG, "Geofence entered: $requestId")

            // Tutaj możesz wykonać dodatkowe czynności, takie jak wyświetlanie powiadomienia.
        }
    }

    companion object {
        private const val TAG = "GeofenceTransitions"

        fun getGeofenceTransition(intent: Intent): Int {
            return GeofencingEvent.fromIntent(intent)?.geofenceTransition ?: 0
        }

        fun getGeofenceRequestId(intent: Intent): String {
            val triggeringGeofences = GeofencingEvent.fromIntent(intent)?.triggeringGeofences
            return triggeringGeofences?.get(0)?.requestId ?: ""
        }
    }
}
