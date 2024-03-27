package software.amazon.location.tracking.util

import android.util.Log

object Logger {
    var logLevel = TrackingSdkLogLevel.DEBUG
    var tag = "Logger"

    /**
     * Log a message with an optional exception.
     *
     * @param message The log message.
     * @param exception An optional exception to include in the log entry.
     */
    fun log(message: String, exception: Exception? = null) {
        if (logLevel == TrackingSdkLogLevel.NONE) {
            return
        }

        if (logLevel == TrackingSdkLogLevel.ERROR && exception != null) {
            Log.e(tag, message, exception)
        } else {
            Log.d(tag, message)
        }
    }
}
