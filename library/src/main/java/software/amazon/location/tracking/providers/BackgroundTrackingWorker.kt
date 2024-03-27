package software.amazon.location.tracking.providers

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import software.amazon.location.tracking.LocationTracker
import software.amazon.location.auth.LocationCredentialsProvider
import software.amazon.location.tracking.util.Logger
import java.util.concurrent.TimeUnit

class BackgroundTrackingWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val locationCredentialsProvider = LocationCredentialsProvider(applicationContext)
        val locationTracker = LocationTracker(applicationContext, locationCredentialsProvider)
        if (ActivityCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            Logger.log("doWork getDeviceLocation called $locationTracker")
            val location = locationTracker.getDeviceLocation(null)
            Logger.log("get location : $location")
        }
        return Result.success()
    }

    companion object {
        private const val BACKGROUND_WORK_TAG = "background_tracking_worker"
        fun enqueueWork(context: Context) {
            val constraints =
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            val request = PeriodicWorkRequestBuilder<BackgroundTrackingWorker>(
                repeatInterval = 15,
                repeatIntervalTimeUnit = TimeUnit.MINUTES
            ).setConstraints(constraints).addTag(BACKGROUND_WORK_TAG).build()
            WorkManager.getInstance(context).enqueue(request)
        }

        fun cancelWork(context: Context) {
            WorkManager.getInstance(context).cancelAllWorkByTag(BACKGROUND_WORK_TAG)
        }

        fun isWorkRunning(context: Context): Boolean {
            val workInfo = WorkManager.getInstance(context)
                .getWorkInfosByTag(BACKGROUND_WORK_TAG).get()
            return workInfo.any { it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.ENQUEUED }
        }
    }
}