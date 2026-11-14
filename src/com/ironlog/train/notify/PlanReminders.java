package com.ironlog.train.notify;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.ironlog.train.data.Routine;
import com.ironlog.train.data.Store;
import com.ironlog.train.ui.MainActivity;

import java.util.Calendar;

/** Schedules the daily "you still have a plan today" nudge. */
public final class PlanReminders {

    public static final String CHANNEL_ID = "plan_reminder";
    private static final int ALARM_ID = 9001;
    public static final int NOTIFICATION_ID = 9101;

    private PlanReminders() {
    }

    public static void ensureChannel(Context c) {
        if (Build.VERSION.SDK_INT < 26) {
            return;
        }
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) {
            return;
        }
        if (nm.getNotificationChannel(CHANNEL_ID) != null) {
            return;
        }
        NotificationChannel ch = new NotificationChannel(CHANNEL_ID, "训练计划提醒",
                NotificationManager.IMPORTANCE_DEFAULT);
        ch.setDescription("当天有计划但还没开始训练时提醒你");
        ch.enableVibration(true);
        nm.createNotificationChannel(ch);
    }

    /** (Re)schedules the daily alarm, or cancels it when disabled. */
    public static void schedule(Context c) {
        Store store = Store.get(c);
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am == null) {
            return;
        }
        Intent intent = new Intent(c, ReminderReceiver.class);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pi = PendingIntent.getBroadcast(c, ALARM_ID, intent, flags);

        am.cancel(pi);
        if (!store.settings.reminderEnabled) {
            return;
        }

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, store.settings.reminderHour);
        cal.set(Calendar.MINUTE, store.settings.reminderMinute);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        if (cal.getTimeInMillis() <= System.currentTimeMillis()) {
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }
        am.setInexactRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY, pi);
    }

    public static void post(Context c, String title, String text) {
        ensureChannel(c);
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) {
            return;
        }
        Intent open = new Intent(c, MainActivity.class);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent tap = PendingIntent.getActivity(c, 0, open, flags);

        Notification.Builder b;
        if (Build.VERSION.SDK_INT >= 26) {
            b = new Notification.Builder(c, CHANNEL_ID);
        } else {
            b = new Notification.Builder(c);
        }
        b.setContentTitle(title)
                .setContentText(text)
                .setStyle(new Notification.BigTextStyle().bigText(text))
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setAutoCancel(true)
                .setContentIntent(tap);
        nm.notify(NOTIFICATION_ID, b.build());
    }

    /** Human readable summary of today's unfinished plans, or null when nothing is due. */
    public static String pendingToday(Store store) {
        java.time.LocalDate today = java.time.LocalDate.now();
        int dow = today.getDayOfWeek().getValue();
        StringBuilder sb = new StringBuilder();
        int n = 0;
        for (Routine r : store.routines) {
            if (!r.onDay(dow)) {
                continue;
            }
            if (isDoneToday(store, r, today)) {
                continue;
            }
            if (n > 0) {
                sb.append("、");
            }
            sb.append(r.name);
            n++;
        }
        if (n == 0) {
            return null;
        }
        return sb.toString();
    }

    private static boolean isDoneToday(Store store, Routine r, java.time.LocalDate day) {
        for (com.ironlog.train.data.WorkoutSession s : store.finishedSessions()) {
            java.time.LocalDate d = com.ironlog.train.logic.Fmt.date(s.startedAt);
            if (!day.equals(d)) {
                continue;
            }
            if (r.id.equals(s.routineId) || (s.name != null && s.name.equals(r.name))) {
                return true;
            }
        }
        return false;
    }
}
