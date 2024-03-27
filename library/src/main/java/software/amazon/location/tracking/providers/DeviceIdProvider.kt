package software.amazon.location.tracking.providers

import android.content.Context
import com.amazonaws.internal.keyvaluestore.AWSKeyValueStore
import software.amazon.location.tracking.util.StoreKey
import java.util.UUID

/**
 *  Class providing Device Id generated with UUID.
 *
 * @constructor Creates a DeviceIdProvider object with the Context.
 *
 * @param context The application context.
 */
class DeviceIdProvider(context: Context) {
    private var awsKeyValueStore: AWSKeyValueStore = AWSKeyValueStore(
        context,
        "software.amazon.location.tracking.deviceId",
        true
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
        setDeviceID()
    }

    /**
     *  Method to get the deviceId
     */
    fun getDeviceID(): String = awsKeyValueStore.get(StoreKey.DEVICE_ID)

    /**
     * Method to clear the deviceId
     */
    fun resetDeviceID() = awsKeyValueStore.clear()

    /**
     * Method to set the deviceId, generates one if not provided
     * @param newDeviceID The new deviceId to be set
     */
    private fun setDeviceID(newDeviceID: String? = null) {
        if (awsKeyValueStore.contains(StoreKey.DEVICE_ID)) return
        awsKeyValueStore.put(StoreKey.DEVICE_ID, newDeviceID ?: generateDeviceID())
    }

    /**
     * Helper method to generate a new deviceId
     */
    private fun generateDeviceID(): String = UUID.randomUUID().toString()
}
