package com.ironlog.train.logic;

import com.ironlog.train.data.SessionExercise;
import com.ironlog.train.data.SetEntry;
import com.ironlog.train.data.WorkoutSession;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Pure statistics over finished sessions. No Android dependencies, unit-testable on the JVM. */
public final class Stats {

    private Stats() {
    }

    /** Epley estimate of a one-rep max. */
    public static double e1rm(double weight, int reps) {
        if (weight <= 0 || reps <= 0) {
            return 0;
        }
        if (reps == 1) {
            return weight;
        }
        return weight * (1.0 + reps / 30.0);
    }

    /** Best estimated 1RM inside one exercise block. */
    public static double bestE1rm(SessionExercise se) {
        double best = 0;
        for (SetEntry s : se.sets) {
            if (!s.done || s.type == SetEntry.WARMUP || s.reps <= 0) {
                continue;
            }
            best = Math.max(best, e1rm(s.weight, s.reps));
        }
        return best;
    }

    public static double topWeight(SessionExercise se) {
        double best = 0;
        for (SetEntry s : se.sets) {
            if (s.done && s.type != SetEntry.WARMUP) {
                best = Math.max(best, s.weight);
            }
        }
        return best;
    }

    /** Heaviest estimated 1RM ever per exercise id. */
    public static Map<String, Double> prMap(List<WorkoutSession> sessions) {
        Map<String, Double> map = new LinkedHashMap<>();
        for (WorkoutSession w : sessions) {
            for (SessionExercise se : w.exercises) {
                double e = bestE1rm(se);
                if (e <= 0) {
                    continue;
                }
                Double cur = map.get(se.exerciseId);
                if (cur == null || e > cur) {
                    map.put(se.exerciseId, e);
                }
            }
        }
        return map;
    }

    public static double totalVolume(List<WorkoutSession> sessions) {
        double v = 0;
        for (WorkoutSession w : sessions) {
            v += w.volume();
        }
        return v;
    }

    public static int totalSets(List<WorkoutSession> sessions) {
        int n = 0;
        for (WorkoutSession w : sessions) {
            n += w.totalSets();
        }
        return n;
    }

    /** Volume for each of the last {@code weeks} weeks, oldest first; last entry is this week. */
    public static double[] weeklyVolume(List<WorkoutSession> sessions, int weeks) {
        double[] out = new double[weeks];
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.minusDays(today.getDayOfWeek().getValue() - 1L);
        for (WorkoutSession w : sessions) {
            if (w.startedAt <= 0) {
                continue;
            }
            LocalDate d = Fmt.date(w.startedAt);
            LocalDate ws = d.minusDays(d.getDayOfWeek().getValue() - 1L);
            long diffWeeks = java.time.temporal.ChronoUnit.WEEKS.between(ws, weekStart);
            int idx = weeks - 1 - (int) diffWeeks;
            if (diffWeeks >= 0 && idx >= 0 && idx < weeks) {
                out[idx] += w.volume();
            }
        }
        return out;
    }

    /** Sessions per week, same ordering as {@link #weeklyVolume}. */
    public static int[] weeklyCount(List<WorkoutSession> sessions, int weeks) {
        int[] out = new int[weeks];
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.minusDays(today.getDayOfWeek().getValue() - 1L);
        for (WorkoutSession w : sessions) {
            if (w.startedAt <= 0) {
                continue;
            }
            LocalDate d = Fmt.date(w.startedAt);
            LocalDate ws = d.minusDays(d.getDayOfWeek().getValue() - 1L);
            long diffWeeks = java.time.temporal.ChronoUnit.WEEKS.between(ws, weekStart);
            int idx = weeks - 1 - (int) diffWeeks;
            if (diffWeeks >= 0 && idx >= 0 && idx < weeks) {
                out[idx]++;
            }
        }
        return out;
    }

    /** Working volume grouped by muscle group, descending. */
    public static Map<String, Double> muscleVolume(List<WorkoutSession> sessions,
                                                   Map<String, String> muscleOfExercise) {
        Map<String, Double> map = new LinkedHashMap<>();
        for (WorkoutSession w : sessions) {
            for (SessionExercise se : w.exercises) {
                double v = se.volume();
                if (v <= 0) {
                    continue;
                }
                String m = muscleOfExercise.get(se.exerciseId);
                if (m == null) {
                    m = "其他";
                }
                Double cur = map.get(m);
                map.put(m, cur == null ? v : cur + v);
            }
        }
        List<Map.Entry<String, Double>> list = new ArrayList<>(map.entrySet());
        Collections.sort(list, new Comparator<Map.Entry<String, Double>>() {
            @Override
            public int compare(Map.Entry<String, Double> a, Map.Entry<String, Double> b) {
                return Double.compare(b.getValue(), a.getValue());
            }
        });
        Map<String, Double> sorted = new LinkedHashMap<>();
        for (Map.Entry<String, Double> e : list) {
            sorted.put(e.getKey(), e.getValue());
        }
        return sorted;
    }

    public static class Point {
        public final long date;
        public final double e1rm;
        public final double topWeight;
        public final double volume;

        public Point(long date, double e1rm, double topWeight, double volume) {
            this.date = date;
            this.e1rm = e1rm;
            this.topWeight = topWeight;
            this.volume = volume;
        }
    }

    /** Chronological strength trend for one exercise. */
    public static List<Point> exerciseTrend(List<WorkoutSession> sessions, String exerciseId) {
        List<WorkoutSession> asc = new ArrayList<>();
        for (WorkoutSession w : sessions) {
            asc.add(w);
        }
        Collections.sort(asc, new Comparator<WorkoutSession>() {
            @Override
            public int compare(WorkoutSession a, WorkoutSession b) {
                return Long.compare(a.startedAt, b.startedAt);
            }
        });
        List<Point> out = new ArrayList<>();
        for (WorkoutSession w : asc) {
            for (SessionExercise se : w.exercises) {
                if (!exerciseId.equals(se.exerciseId) || se.doneSets() == 0) {
                    continue;
                }
                out.add(new Point(w.startedAt, bestE1rm(se), topWeight(se), se.volume()));
            }
        }
        return out;
    }

    /**
     * Consecutive weeks that contain at least one session.
     * A week that is still in progress (this week) does not break the streak when it is empty,
     * otherwise a Monday morning would show "0 weeks" after a perfect month.
     */
    public static int weekStreak(List<WorkoutSession> sessions) {
        if (sessions.isEmpty()) {
            return 0;
        }
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.minusDays(today.getDayOfWeek().getValue() - 1L);
        boolean[] hit = new boolean[104];
        for (WorkoutSession w : sessions) {
            if (w.startedAt <= 0) {
                continue;
            }
            LocalDate d = Fmt.date(w.startedAt);
            LocalDate ws = d.minusDays(d.getDayOfWeek().getValue() - 1L);
            long diff = java.time.temporal.ChronoUnit.WEEKS.between(ws, weekStart);
            if (diff >= 0 && diff < hit.length) {
                hit[(int) diff] = true;
            }
        }
        int i = hit[0] ? 0 : 1;
        int n = 0;
        while (i < hit.length && hit[i]) {
            n++;
            i++;
        }
        return n;
    }

    public static int countOn(List<WorkoutSession> sessions, LocalDate day) {
        long from = Fmt.millis(day);
        long to = Fmt.millis(day.plusDays(1));
        int n = 0;
        for (WorkoutSession w : sessions) {
            if (w.startedAt >= from && w.startedAt < to) {
                n++;
            }
        }
        return n;
    }

    public static double volumeOn(List<WorkoutSession> sessions, LocalDate day) {
        long from = Fmt.millis(day);
        long to = Fmt.millis(day.plusDays(1));
        double v = 0;
        for (WorkoutSession w : sessions) {
            if (w.startedAt >= from && w.startedAt < to) {
                v += w.volume();
            }
        }
        return v;
    }

    public static List<WorkoutSession> onDay(List<WorkoutSession> sessions, LocalDate day) {
        long from = Fmt.millis(day);
        long to = Fmt.millis(day.plusDays(1));
        List<WorkoutSession> out = new ArrayList<>();
        for (WorkoutSession w : sessions) {
            if (w.startedAt >= from && w.startedAt < to) {
                out.add(w);
            }
        }
        return out;
    }
}
