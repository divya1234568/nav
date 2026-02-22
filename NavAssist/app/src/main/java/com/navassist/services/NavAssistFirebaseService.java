package com.navassist.services;

import android.app.*;
import android.content.Intent;
import android.os.*;
import androidx.core.app.NotificationCompat;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.navassist.*;

public class NavAssistFirebaseService extends FirebaseMessagingService {

    private static final String CHANNEL_SOS = "sos_alerts";
    private static final String CHANNEL_NAV = "navigation";

    @Override
    public void onMessageReceived(RemoteMessage message) {
        super.onMessageReceived(message);

        String type = message.getData().get("type");
        if ("SOS".equals(type)) {
            String location = message.getData().get("location");
            String userName = message.getData().get("user_name");
            sendSOSNotification(userName, location);

            // Flash + vibrate for deaf guardian
            Vibrator v = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            if (v != null) {
                long[] pattern = {0, 800, 200, 800, 200, 800};
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v.vibrate(VibrationEffect.createWaveform(pattern, -1));
                } else {
                    v.vibrate(pattern, -1);
                }
            }
        }
    }

    private void sendSOSNotification(String userName, String location) {
        createNotificationChannel();

        Intent intent = new Intent(this, GuardianActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_SOS)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("🚨 SOS EMERGENCY ALERT")
            .setContentText(userName + " needs immediate help! Tap to view location.")
            .setStyle(new NotificationCompat.BigTextStyle()
                .bigText(userName + " has triggered an SOS emergency!\n📍 Location: " + location + "\nTap to open Guardian app."))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setFullScreenIntent(pendingIntent, true)  // Shows on locked screen
            .setSound(android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI)
            .setContentIntent(pendingIntent);

        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (nm != null) nm.notify(1001, builder.build());
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel sosChannel = new NotificationChannel(
                CHANNEL_SOS, "SOS Emergency Alerts",
                NotificationManager.IMPORTANCE_HIGH);
            sosChannel.setDescription("Critical alerts when disabled person needs help");
            sosChannel.enableVibration(true);
            sosChannel.setVibrationPattern(new long[]{0, 500, 200, 500});

            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(sosChannel);
        }
    }

    @Override
    public void onNewToken(String token) {
        // Send token to server to associate with guardian/user
        // In production: save to Firebase or your backend
        super.onNewToken(token);
    }
}
