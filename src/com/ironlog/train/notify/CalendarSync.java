package com.ironlog.train.notify;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract;

import com.ironlog.train.data.Routine;
import com.ironlog.train.data.RoutineExercise;
import com.ironlog.train.data.Store;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.TimeZone;

/** Writes planned sessions into the user's own calendar so the system can remind them. */
public final class CalendarSync {

    private static final String MARK = "[IronLog]";

    private CalendarSync() {
    }

    public static final int ERR_NO_CALENDAR = -1;
    public static final int ERR_FAILED = -2;

    /**
     * Writes the next {@code weeks} weeks of planned routines as calendar events.
     * Replaces previously exported IronLog events that are still in the future.
     *
     * @return number of events written, or ERR_NO_CALENDAR / ERR_FAILED
     */
    public static int export(Context c, int weeks) {
        try {
            long calId = pickWritableCalendar(c);
            if (calId < 0) {
                return ERR_NO_CALENDAR;
            }
            Store store = Store.get(c);
            ContentResolver cr = c.getContentResolver();

            // remove our own future events first so re-running does not duplicate
            try {
                cr.delete(CalendarContract.Events.CONTENT_URI,
                        CalendarContract.Events.DESCRIPTION + " LIKE ? AND "
                                + CalendarContract.Events.DTSTART + " >= ?",
                        new String[]{"%" + MARK + "%", String.valueOf(System.currentTimeMillis())});
            } catch (Exception ignored) {
            }

            int hour = Math.max(0, Math.min(23, store.settings.reminderHour));
            ZoneId zone = ZoneId.systemDefault();
            LocalDate today = LocalDate.now();
            int written = 0;

            for (int offset = 0; offset < weeks * 7; offset++) {
                LocalDate day = today.plusDays(offset);
                int dow = day.getDayOfWeek().getValue();
                for (Routine r : store.routines) {
                    if (!r.onDay(dow) || r.items.isEmpty()) {
                        continue;
                    }
                    long begin = day.atTime(hour, 0).atZone(zone).toInstant().toEpochMilli();
                    long end = begin + 60L * 60L * 1000L;

                    ContentValues cv = new ContentValues();
                    cv.put(CalendarContract.Events.CALENDAR_ID, calId);
                    cv.put(CalendarContract.Events.TITLE, "训练 · " + r.name);
                    cv.put(CalendarContract.Events.DESCRIPTION, MARK + " " + describe(store, r));
                    cv.put(CalendarContract.Events.DTSTART, begin);
                    cv.put(CalendarContract.Events.DTEND, end);
                    cv.put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().getID());
                    cv.put(CalendarContract.Events.HAS_ALARM, 1);

                    Uri uri = cr.insert(CalendarContract.Events.CONTENT_URI, cv);
                    if (uri == null) {
                        continue;
                    }
                    long eventId = Long.parseLong(uri.getLastPathSegment());
                    written++;

                    ContentValues rem = new ContentValues();
                    rem.put(CalendarContract.Reminders.EVENT_ID, eventId);
                    rem.put(CalendarContract.Reminders.MINUTES, 15);
                    rem.put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT);
                    try {
                        cr.insert(CalendarContract.Reminders.CONTENT_URI, rem);
                    } catch (Exception ignored) {
                    }
                }
            }
            return written;
        } catch (Throwable t) {
            return ERR_FAILED;
        }
    }

    private static String describe(Store store, Routine r) {
        StringBuilder sb = new StringBuilder();
        for (RoutineExercise ri : r.items) {
            if (sb.length() > 0) {
                sb.append(" / ");
            }
            sb.append(store.exerciseName(ri.exerciseId))
                    .append(" ").append(ri.sets).append("×").append(ri.reps);
        }
        return sb.toString();
    }

    private static long pickWritableCalendar(Context c) {
        Cursor cur = null;
        try {
            Uri uri = CalendarContract.Calendars.CONTENT_URI;
            String[] projection = {
                    CalendarContract.Calendars._ID,
                    CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL,
                    CalendarContract.Calendars.VISIBLE
            };
            cur = c.getContentResolver().query(uri, projection, null, null, null);
            if (cur == null) {
                return -1;
            }
            while (cur.moveToNext()) {
                long id = cur.getLong(0);
                int access = cur.getInt(1);
                int visible = cur.getInt(2);
                if (visible == 1 && access >= CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR) {
                    return id;
                }
            }
            return -1;
        } catch (Throwable t) {
            return -1;
        } finally {
            if (cur != null) {
                cur.close();
            }
        }
    }
}
