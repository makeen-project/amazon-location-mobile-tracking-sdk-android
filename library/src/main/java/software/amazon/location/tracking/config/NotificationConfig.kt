package software.amazon.location.tracking.config

import software.amazon.location.tracking.R

data class NotificationConfig(
    var notificationId: Int = 19921,
    var notificationChannelId: String = "aws_tracking_notification",
    var notificationChannelName: String = "aws_tracking_notification_name",
    var notificationTitle: String = "Tracking your location",
    var notificationDescription: String = "Amazon tracking SDK app is tracking your location",
    var notificationImageId: Int = R.drawable.my_location
)