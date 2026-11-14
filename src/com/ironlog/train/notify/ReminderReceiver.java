package com.ironlog.train.notify;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.ironlog.train.data.Store;

/** Fires once a day: nudges only when a planned routine is still unfinished. */
public class ReminderReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            Store store = Store.get(context);
            if (!store.settings.reminderEnabled) {
                return;
            }
            String pending = PlanReminders.pendingToday(store);
            if (pending == null) {
                return;
            }
            PlanReminders.post(context, "今天还有计划没完成",
                    pending + " —— 现在练还来得及。");
        } catch (Throwable ignored) {
        }
    }
}
