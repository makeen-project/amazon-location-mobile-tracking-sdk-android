package software.amazon.location.tracking.providers

import android.content.Context
import software.amazon.location.tracking.util.StoreKey
import java.util.UUID
import software.amazon.location.auth.EncryptedSharedPreferences

/**
 *  Class providing Device Id generated with UUID.
 *
 * @constructor Creates a DeviceIdProvider object with the Context.
 *
 * @param context The application context.
 */
class DeviceIdProvider(context: Context) {
    private var securePreferences: EncryptedSharedPreferences = EncryptedSharedPreferences(
        context,
        "software.amazon.location.tracking.deviceId"
    )

    companion object {

        @Volatile
        private var instance: DeviceIdProvider? = null

        fun getInstance(context: Context): DeviceIdProvider {
            return instance ?: synchronized(this) {
                instance ?: DeviceIdProvider(context).also { instance = it }
            }
        }
    }

    init {
        securePreferences.initEncryptedSharedPreferences()
        setDeviceID()
    }

    /**
     *  Method to get the deviceId
     */
    fun getDeviceID(): String = securePreferences.get(StoreKey.DEVICE_ID) ?: ""

    /**
     * Method to clear the deviceId
     */
    fun resetDeviceID() = securePreferences.clear()

    /**
     * Method to set the deviceId, generates one if not provided
     * @param newDeviceID The new deviceId to be set
     */
    private fun setDeviceID(newDeviceID: String? = null) {
        if (securePreferences.contains(StoreKey.DEVICE_ID)) return
        securePreferences.put(StoreKey.DEVICE_ID, newDeviceID ?: generateDeviceID())
    }

    /**
     * Helper method to generate a new deviceId
     */
    private fun generateDeviceID(): String = UUID.randomUUID().toString()
}
