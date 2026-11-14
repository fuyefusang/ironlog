package com.ironlog.train.data;

import java.util.ArrayList;
import java.util.List;

public class Routine {
    public String id;
    public String name;
    public String note;
    public long updatedAt;
    /** Weekly schedule bitmask: bit 0 = Monday ... bit 6 = Sunday. 0 means unscheduled. */
    public int days;
    public List<RoutineExercise> items = new ArrayList<>();

    public Routine() {
    }

    public boolean onDay(int isoDayOfWeek) {
        if (isoDayOfWeek < 1 || isoDayOfWeek > 7) {
            return false;
        }
        return (days & (1 << (isoDayOfWeek - 1))) != 0;
    }

    public void toggleDay(int isoDayOfWeek) {
        days ^= (1 << (isoDayOfWeek - 1));
    }

    private static final String[] WEEK_LABEL = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};

    public String daysLabel() {
        if (days == 0) {
            return "未安排";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 7; i++) {
            if ((days & (1 << i)) != 0) {
                if (sb.length() > 0) {
                    sb.append(" ");
                }
                sb.append(WEEK_LABEL[i]);
            }
        }
        return sb.toString();
    }

    public int plannedSets() {
        int n = 0;
        for (RoutineExercise r : items) {
            n += r.sets;
        }
        return n;
    }
}
