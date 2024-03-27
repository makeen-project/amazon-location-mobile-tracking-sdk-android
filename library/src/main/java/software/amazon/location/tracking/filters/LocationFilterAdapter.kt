package software.amazon.location.tracking.filters

import software.amazon.location.tracking.util.PropertyKey
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import java.lang.reflect.Type

class LocationFilterAdapter : JsonSerializer<LocationFilter>, JsonDeserializer<LocationFilter> {
    override fun serialize(
      filter: LocationFilter,
      type: Type,
      context: JsonSerializationContext
    ): JsonElement {
        val jsonObject = JsonObject()
        when (filter) {
            is TimeLocationFilter -> {
                jsonObject.addProperty(PropertyKey.TYPE, TIME_LOCATION_FILTER)
                jsonObject.addProperty(PropertyKey.TIME_INTERVAL, filter.timeInterval)
            }

            is AccuracyLocationFilter -> {
                jsonObject.addProperty(PropertyKey.TYPE, ACCURACY_LOCATION_FILTER)
            }

            is DistanceLocationFilter -> {
                jsonObject.addProperty(PropertyKey.TYPE, DISTANCE_LOCATION_FILTER)
                jsonObject.addProperty(PropertyKey.DISTANCE_THRESHOLD, filter.distanceThreshold)
            }
        }
        return jsonObject
    }

    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): LocationFilter {
        val jsonObject = json.asJsonObject
        return when (jsonObject.get(PropertyKey.TYPE).asString) {
            TIME_LOCATION_FILTER -> {
                TimeLocationFilter(jsonObject.get(PropertyKey.TIME_INTERVAL).asLong)
            }

            ACCURACY_LOCATION_FILTER -> {
                AccuracyLocationFilter()
            }

            DISTANCE_LOCATION_FILTER -> {
                DistanceLocationFilter(jsonObject.get(PropertyKey.DISTANCE_THRESHOLD).asDouble)
            }

            else -> throw IllegalArgumentException("Unknown type")
        }
    }

    companion object {
        private const val TIME_LOCATION_FILTER = "TimeLocationFilter"
        private const val ACCURACY_LOCATION_FILTER = "AccuracyLocationFilter"
        private const val DISTANCE_LOCATION_FILTER = "DistanceLocationFilter"
    }
}