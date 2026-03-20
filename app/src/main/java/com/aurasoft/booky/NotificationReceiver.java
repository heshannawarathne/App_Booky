package com.aurasoft.booky;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class NotificationReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "bus_alerts_channel";

    @Override
    public void onReceive(Context context, Intent intent) {
        String title = intent.getStringExtra("title");
        String message = intent.getStringExtra("message");
        String userId = intent.getStringExtra("userId");

        Toast.makeText(context, "Alarm Fired!", Toast.LENGTH_LONG).show();
        Log.i("Notification","Alarm Fired!");

        showNotification(context, title, message);
        saveToFirestore(userId, title, message);
    }

    private void showNotification(Context context, String title, String message) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        String CHANNEL_ID = "bus_alerts_channel";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // 1. Channel එකේ Importance එක High ම දාන්න
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Bus Trip Alerts",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.enableLights(true);
            channel.enableVibration(true);
            channel.setLockscreenVisibility(android.app.Notification.VISIBILITY_PUBLIC);

            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }

        // 2. Notification එක Click කරාම MainActivity එකට යන එක
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, (int) System.currentTimeMillis(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // 3. Notification Builder (Heads-up notification සඳහා)
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info) // ටෙස්ට් කරන්න Android අයිකන් එකක්ම දෙමු
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH) // Android 7.1 සහ අඩු සඳහා
                .setDefaults(NotificationCompat.DEFAULT_ALL) // සද්දය සහ වයිබ්‍රේෂන්
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

        if (notificationManager != null) {
            // ID එකට අහඹු අංකයක් දෙන්න
            notificationManager.notify((int) System.currentTimeMillis(), builder.build());
        }
    }

    private void saveToFirestore(String userId, String title, String message) {
        if (userId == null) {
            Log.e("Notification", "User ID is null, cannot save to Firestore");
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> notification = new HashMap<>();
        notification.put("userId", userId);
        notification.put("title", title);
        notification.put("message", message);
        notification.put("timestamp", System.currentTimeMillis());

        // වැදගත්: මේක add කරන්න කලින් Map එකට දාන්න ඕනේ
        notification.put("isRead", false);

        db.collection("Notifications")
                .add(notification)
                .addOnSuccessListener(documentReference -> {
                    Log.d("Notification", "Successfully saved to Firestore: " + documentReference.getId());
                })
                .addOnFailureListener(e -> {
                    Log.e("Notification", "Error saving to Firestore", e);
                });
    }
}