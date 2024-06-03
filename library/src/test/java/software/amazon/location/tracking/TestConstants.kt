package software.amazon.location.tracking

object TestConstants {
    const val timeInterval = 1000L
    const val latitude = 123.456
    const val longitude = 789.012
    const val accuracy = 13.0f
    const val TRACKER_NAME = "test"
    const val TEST_LATITUDE = 37.7749
    const val TEST_LONGITUDE = -122.4194
    const val TEST_IDENTITY_POOL_ID = "us-east-1:dummyIdentityPoolId"
    const val LAST_LOCATION = "{\"id\":100,\"latitude\":10.0,\"longitude\":20.0,\"time\":1000,\"accuracy\":15}"
    const val TEST_CLIENT_CONFIG = "{\"accuracy\":100,\"frequency\":5000,\"latency\":1000,\"locationFilters\":[{\"type\":\"TimeLocationFilter\",\"timeInterval\":30000},{\"type\":\"DistanceLocationFilter\",\"distanceThreshold\":30.0}],\"logLevel\":\"DEBUG\",\"minUpdateIntervalMillis\":5000,\"persistentNotificationConfig\":{\"notificationChannelId\":\"test\",\"notificationChannelName\":\"test\",\"notificationDescription\":\"test\",\"notificationId\":22,\"notificationImageId\":111,\"notificationTitle\":\"Tracking your location\"},\"trackerName\":\"test\",\"waitForAccurateLocation\":false}"
    const val METHOD = "method"
    const val IDENTITY_POOL_ID = "identityPoolId"
}
