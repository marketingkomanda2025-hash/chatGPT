package com.floatingdock.visual;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;

/**
 * Displays one explicit, user-requested local test notification.
 * There is no server, tracking or background polling.
 */
public final class HryundelNotificationReceiver extends BroadcastReceiver {
    private static final String CHANNEL_ID = "hryundel_test_v1";
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Build.VERSION.SDK_INT >= 33 &&
                context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) return;

        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;

        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, "Хрюндель 🐽", NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription("Тестовые сообщения от Хрюнделя");
        channel.enableVibration(true);
        channel.setLightColor(Color.rgb(165, 171, 255));
        manager.createNotificationChannel(channel);

        Intent openChat = new Intent(context, MainActivity.class);
        openChat.putExtra(MainActivity.EXTRA_OPEN_HRYUNDEL, true);
        openChat.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent tap = PendingIntent.getActivity(context, 7402, openChat,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String message = "Я тут! 🐽 Заходи в чат — проверим, как работает Floating Dock.";
        Notification notification = new Notification.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_hryundel_notification)
                .setContentTitle("Хрюндель 🐽")
                .setContentText(message)
                .setStyle(new Notification.BigTextStyle().bigText(message))
                .setContentIntent(tap)
                .setAutoCancel(true)
                .setVisibility(Notification.VISIBILITY_PRIVATE)
                .build();
        manager.notify(7402, notification);
    }
}
