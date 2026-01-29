/*
 * This is the source code of Telegram for Android v. 1.3.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2018.
 */

package org.telegram.messenger;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.IBinder;
import android.util.Log; // Import Log

import androidx.core.app.NotificationCompat;

public class NotificationsService extends Service {

    private static final String TAG = "NotificationsService"; // Add TAG for logging
    public static final String NOTIFICATION_CHANNEL_ID = "org.telegram.messenger.NOTIFICATION_CHANNEL_ID";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate: NotificationsService started"); // Log entry
        ApplicationLoader.postInitApplication();
        Log.d(TAG, "onCreate: ApplicationLoader.postInitApplication() called"); // Log after postInit

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    "Notifications",
                    NotificationManager.IMPORTANCE_LOW
            );
            notificationChannel.setDescription("Telegram notifications");
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(notificationChannel);
                Log.d(TAG, "onCreate: NotificationChannel created"); // Log channel creation
            }
        }

        Notification notification = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setContentTitle("Telegram")
                .setContentText("Running in background")
                .setSmallIcon(org.telegram.messenger.R.drawable.ic_launcher_dr)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();

        startForeground(1, notification);
        Log.d(TAG, "onCreate: startForeground() called"); // Log after startForeground
    }

    private static final String CHANNEL_ID = "keep_alive_channel";
    private static final int NOTIFICATION_ID = 1001;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand: NotificationsService received command");

        // 1. Створюємо канал сповіщень (потрібно для Android 8.0+)
        createNotificationChannel();

        // 2. Створюємо саме сповіщення
        // Можна додати PendingIntent, щоб при натисканні відкривався додаток
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("BodyaGram")
                .setContentText("Background sync")
                .setSmallIcon(R.drawable.notification) // переконайтеся, що іконка існує
                .setPriority(NotificationCompat.PRIORITY_LOW) // мінімальне втручання
                .setOngoing(false) // користувач не зможе його просто змахнути
                .build();

        // 3. ПЕРЕВЕДЕННЯ СЕРВІСУ В РЕЖИМ FOREGROUND
        // Це найголовніший рядок, який не дає додатку вмерти
        startForeground(NOTIFICATION_ID, notification);

        return START_STICKY;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Background Service Channel",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void onDestroy() {
        Log.d(TAG, "onDestroy: NotificationsService destroyed"); // Log onDestroy
        super.onDestroy();
        SharedPreferences preferences = MessagesController.getGlobalNotificationsSettings();
        if (preferences.getBoolean("pushService", true)) {
            Intent intent = new Intent("org.telegram.start");
            intent.setPackage(getPackageName());
            sendBroadcast(intent);
        }
    }
}
