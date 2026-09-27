package app.carpe

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import app.carpe.core.NotificationPressure

class CarpeNotificationListener:NotificationListenerService(){
    override fun onNotificationPosted(sbn:StatusBarNotification){
        if(sbn.packageName!=packageName) NotificationPressure(this).record(sbn.packageName)
    }
}
