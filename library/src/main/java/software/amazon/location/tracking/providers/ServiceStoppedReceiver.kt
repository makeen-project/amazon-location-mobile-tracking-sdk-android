package software.amazon.location.tracking.providers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ServiceStoppedReceiver(private val listener: ServiceStoppedListener) : BroadcastReceiver() {
    interface ServiceStoppedListener {
        fun onServiceStopped()
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        listener.onServiceStopped()
    }
}