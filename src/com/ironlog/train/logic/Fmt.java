package com.ironlog.train.logic;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class Fmt {

    private static final DateTimeFormatter SHORT = DateTimeFormatter.ofPattern("MM/dd");
    private static final DateTimeFormatter ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final String[] WEEK = {"一", "二", "三", "四", "五", "六", "日"};

    private Fmt() {
    }

    public static LocalDate date(long millis) {
        return Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate();
    }

    public static long millis(LocalDate d) {
        return d.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    public static String shortDate(long millis) {
        return SHORT.format(date(millis));
    }

    public static String iso(long millis) {
        return ISO.format(date(millis));
    }

    public static String monthTitle(LocalDate d) {
        return d.getYear() + "年" + d.getMonthValue() + "月";
    }

    public static String monthTitle(java.time.YearMonth ym) {
        return ym.getYear() + "年" + ym.getMonthValue() + "月";
    }

    public static String weekday(long millis) {
        return "周" + WEEK[date(millis).getDayOfWeek().getValue() - 1];
    }

    public static String num(double v) {
        if (Math.abs(v - Math.rint(v)) < 0.01) {
            return String.valueOf((long) Math.rint(v));
        }
        return String.format(Locale.US, "%.1f", v);
    }

    public static String num1(double v) {
        return String.format(Locale.US, "%.1f", v);
    }

    public static String vol(double v) {
        if (v >= 100000) {
            return String.format(Locale.US, "%.0fk", v / 1000);
        }
        if (v >= 10000) {
            return String.format(Locale.US, "%.1fk", v / 1000);
        }
        return String.format(Locale.US, "%,.0f", v);
    }

    public static String duration(long sec) {
        if (sec < 0) {
            sec = 0;
        }
        long h = sec / 3600;
        long m = (sec % 3600) / 60;
        long s = sec % 60;
        if (h > 0) {
            return String.format(Locale.US, "%d:%02d:%02d", h, m, s);
        }
        return String.format(Locale.US, "%02d:%02d", m, s);
    }

    public static String clock(long sec) {
        long m = Math.max(0, sec) / 60;
        long s = Math.max(0, sec) % 60;
        return String.format(Locale.US, "%d:%02d", m, s);
    }
}
