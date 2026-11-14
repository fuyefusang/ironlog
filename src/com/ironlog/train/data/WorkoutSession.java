package com.ironlog.train.data;

import java.util.ArrayList;
import java.util.List;

public class WorkoutSession {
    public String id;
    public String name;
    public String note;
    /** Set when the session was started from a saved routine. */
    public String routineId;
    public long startedAt;
    public long endedAt;
    /** Total milliseconds spent paused so far. */
    public long pausedAccumMs;
    /** When > 0 the session is currently paused. */
    public long pausedAt;
    public List<SessionExercise> exercises = new ArrayList<>();

    public WorkoutSession() {
    }

    public boolean isActive() {
        return endedAt == 0L;
    }

    public boolean isPaused() {
        return pausedAt > 0L;
    }

    public long durationSec() {
        if (startedAt == 0) {
            return 0;
        }
        long end = endedAt > 0 ? endedAt : System.currentTimeMillis();
        long extra = isPaused() ? Math.max(0, end - pausedAt) : 0;
        long ms = end - startedAt - pausedAccumMs - extra;
        return Math.max(0, ms / 1000L);
    }

    public double volume() {
        double v = 0;
        for (SessionExercise e : exercises) {
            v += e.volume();
        }
        return v;
    }

    public int totalSets() {
        int n = 0;
        for (SessionExercise e : exercises) {
            n += e.doneSets();
        }
        return n;
    }
}
