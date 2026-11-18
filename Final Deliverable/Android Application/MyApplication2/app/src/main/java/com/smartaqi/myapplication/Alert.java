/* This Java class send notifications to the citizens*/

package com.smartaqi.myapplication;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class Alert {

    private static Context context;
    private static final String CHANNEL_ID = "AQI_CHANNEL";

    public Alert(Context context) {
        this.context = context;
    }

    // ✅ Create channel (call once)
    public void createNotificationChannel() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            CharSequence name = "AQI Channel";
            String description = "AQI Notifications";
            int importance = NotificationManager.IMPORTANCE_HIGH;

            NotificationChannel channel =
                    new NotificationChannel(CHANNEL_ID, name, importance);

            channel.setDescription(description);

            NotificationManager manager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            manager.createNotificationChannel(channel);
        }
    }

    // ✅ Send notification
    // Inside Alert.java
    public static void sendAlert(float aqi, String category, String impact, String action) {

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_launcher_foreground)
                        .setContentTitle("AQI Alert 🚨 (" + category + ")")
                        .setContentText("AQI Level: " + aqi +
                                "\n Impact: " + impact +
                                "\n Recommendation: " + action)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setDefaults(NotificationCompat.DEFAULT_ALL)
                        .setAutoCancel(true);

        if (Build.VERSION.SDK_INT >= 33) {
            if (context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.e("NOTIFICATION", "Permission NOT granted");
                return;
            }
        }

        NotificationManagerCompat.from(context)
                .notify((int) System.currentTimeMillis(), builder.build()); // ✅ unique ID

        Log.d("NOTIFICATION", "sendAlert called");
    }
}