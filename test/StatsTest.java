import com.ironlog.train.data.SessionExercise;
import com.ironlog.train.data.SetEntry;
import com.ironlog.train.data.WorkoutSession;
import com.ironlog.train.logic.Fmt;
import com.ironlog.train.logic.Stats;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StatsTest {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        testE1rm();
        testVolume();
        testPr();
        testWeekly();
        testMuscle();
        testTrend();
        testStreak();
        testDayLookup();
        testDurationFormat();
        testPause();

        System.out.println();
        System.out.println("passed=" + passed + " failed=" + failed);
        if (failed > 0) {
            System.exit(1);
        }
        System.out.println("ALL STATS TESTS PASSED");
    }

    private static void check(String name, boolean ok, String detail) {
        if (ok) {
            passed++;
        } else {
            failed++;
            System.out.println("FAIL: " + name + " -> " + detail);
        }
    }

    private static void near(String name, double actual, double expect) {
        check(name, Math.abs(actual - expect) < 0.01, "expected " + expect + " got " + actual);
    }

    private static SessionExercise block(String id, double[][] sets) {
        SessionExercise se = new SessionExercise(id);
        for (double[] s : sets) {
            SetEntry e = new SetEntry(s[0], (int) s[1], true);
            se.sets.add(e);
        }
        return se;
    }

    private static WorkoutSession session(String id, long ts, SessionExercise... blocks) {
        WorkoutSession w = new WorkoutSession();
        w.id = id;
        w.startedAt = ts;
        w.endedAt = ts + 3600_000L;
        for (SessionExercise b : blocks) {
            w.exercises.add(b);
        }
        return w;
    }

    private static void testE1rm() {
        near("e1rm 1 rep", Stats.e1rm(100, 1), 100);
        near("e1rm 5 reps", Stats.e1rm(100, 5), 116.666);
        near("e1rm zero weight", Stats.e1rm(0, 5), 0);
        near("e1rm zero reps", Stats.e1rm(100, 0), 0);
    }

    private static void testVolume() {
        SessionExercise se = block("bench", new double[][]{{100, 5}, {100, 5}});
        // warmup must not count
        SetEntry warm = new SetEntry(60, 10, true);
        warm.type = SetEntry.WARMUP;
        se.sets.add(warm);
        // not-done set must not count
        se.sets.add(new SetEntry(100, 5, false));

        near("volume", se.volume(), 1000);
        check("doneSets", se.doneSets() == 3, "got " + se.doneSets());

        WorkoutSession w = session("s1", Fmt.millis(LocalDate.of(2026, 11, 10)), se);
        near("session volume", w.volume(), 1000);
        check("session sets", w.totalSets() == 3, "got " + w.totalSets());
    }

    private static void testPr() {
        long t1 = Fmt.millis(LocalDate.of(2026, 11, 1));
        long t2 = Fmt.millis(LocalDate.of(2026, 11, 5));
        WorkoutSession a = session("a", t1, block("bench", new double[][]{{100, 5}}));
        WorkoutSession b = session("b", t2, block("bench", new double[][]{{95, 10}}));
        List<WorkoutSession> all = new ArrayList<>();
        all.add(a);
        all.add(b);

        Map<String, Double> pr = Stats.prMap(all);
        // 95x10 = 126.67 e1rm beats 100x5 = 116.67
        near("pr bench", pr.get("bench"), 126.666);
    }

    private static void testWeekly() {
        List<WorkoutSession> all = new ArrayList<>();
        LocalDate today = LocalDate.now();
        LocalDate monday = today.minusDays(today.getDayOfWeek().getValue() - 1L);
        all.add(session("thisweek", Fmt.millis(monday), block("squat", new double[][]{{100, 5}})));
        all.add(session("lastweek", Fmt.millis(monday.minusWeeks(1)), block("squat", new double[][]{{80, 5}})));
        all.add(session("old", Fmt.millis(monday.minusWeeks(10)), block("squat", new double[][]{{60, 5}})));

        double[] v = Stats.weeklyVolume(all, 4);
        check("weekly length", v.length == 4, "len " + v.length);
        near("last week volume", v[2], 400);
        near("this week volume", v[3], 500);
        near("out of window", v[0], 0);

        int[] c = Stats.weeklyCount(all, 4);
        check("this week count", c[3] == 1, "got " + c[3]);
        check("last week count", c[2] == 1, "got " + c[2]);
    }

    private static void testMuscle() {
        List<WorkoutSession> all = new ArrayList<>();
        long t = Fmt.millis(LocalDate.of(2026, 11, 2));
        all.add(session("m", t,
                block("bench", new double[][]{{100, 5}}),
                block("squat", new double[][]{{150, 5}})));

        Map<String, String> muscle = new HashMap<>();
        muscle.put("bench", "胸");
        muscle.put("squat", "腿");

        Map<String, Double> mv = Stats.muscleVolume(all, muscle);
        near("chest volume", mv.get("胸"), 500);
        near("leg volume", mv.get("腿"), 750);
        check("leg first (sorted desc)",
                new ArrayList<>(mv.keySet()).get(0).equals("腿"),
                "order " + mv.keySet());
    }

    private static void testTrend() {
        List<WorkoutSession> all = new ArrayList<>();
        long t1 = Fmt.millis(LocalDate.of(2026, 9, 1));
        long t2 = Fmt.millis(LocalDate.of(2026, 10, 1));
        all.add(session("newer", t2, block("bench", new double[][]{{100, 5}})));
        all.add(session("older", t1, block("bench", new double[][]{{90, 5}})));

        List<Stats.Point> pts = Stats.exerciseTrend(all, "bench");
        check("trend size", pts.size() == 2, "got " + pts.size());
        check("trend chronological", pts.get(0).date < pts.get(1).date, "not sorted");
        near("trend first e1rm", pts.get(0).e1rm, 105);
        near("trend last top weight", pts.get(1).topWeight, 100);
    }

    private static void testStreak() {
        LocalDate today = LocalDate.now();
        LocalDate monday = today.minusDays(today.getDayOfWeek().getValue() - 1L);
        List<WorkoutSession> all = new ArrayList<>();
        all.add(session("w0", Fmt.millis(monday), block("a", new double[][]{{10, 10}})));
        all.add(session("w1", Fmt.millis(monday.minusWeeks(1)), block("a", new double[][]{{10, 10}})));
        all.add(session("w2", Fmt.millis(monday.minusWeeks(2)), block("a", new double[][]{{10, 10}})));
        check("streak 3", Stats.weekStreak(all) == 3, "got " + Stats.weekStreak(all));

        List<WorkoutSession> gap = new ArrayList<>();
        gap.add(session("w0", Fmt.millis(monday), block("a", new double[][]{{10, 10}})));
        gap.add(session("w2", Fmt.millis(monday.minusWeeks(2)), block("a", new double[][]{{10, 10}})));
        check("streak stops at gap", Stats.weekStreak(gap) == 1, "got " + Stats.weekStreak(gap));
        check("empty streak", Stats.weekStreak(new ArrayList<WorkoutSession>()) == 0, "not 0");

        // Monday morning: this week has no session yet, the previous two weeks were on plan.
        List<WorkoutSession> noCurrent = new ArrayList<>();
        noCurrent.add(session("prev1", Fmt.millis(monday.minusWeeks(1)), block("a", new double[][]{{10, 10}})));
        noCurrent.add(session("prev2", Fmt.millis(monday.minusWeeks(2)), block("a", new double[][]{{10, 10}})));
        check("streak looks past an empty current week", Stats.weekStreak(noCurrent) == 2,
                "got " + Stats.weekStreak(noCurrent));

        // Two consecutive empty weeks do break it.
        List<WorkoutSession> stale = new ArrayList<>();
        stale.add(session("old", Fmt.millis(monday.minusWeeks(2)), block("a", new double[][]{{10, 10}})));
        check("streak breaks after a full empty week", Stats.weekStreak(stale) == 0,
                "got " + Stats.weekStreak(stale));
    }

    private static void testPause() {
        WorkoutSession w = new WorkoutSession();
        w.startedAt = 1_000_000L;
        w.endedAt = 1_600_000L;          // 10 minutes wall clock
        w.pausedAccumMs = 120_000L;      // 2 minutes were spent paused
        check("duration excludes paused time", w.durationSec() == 480,
                "got " + w.durationSec());

        WorkoutSession never = new WorkoutSession();
        never.startedAt = 0L;
        check("unstarted session has no duration", never.durationSec() == 0,
                "got " + never.durationSec());

        WorkoutSession paused = new WorkoutSession();
        long now = System.currentTimeMillis();
        paused.startedAt = now - 60_000L;      // started a minute ago
        paused.pausedAt = now - 10_000L;       // paused 10 seconds ago
        long d = paused.durationSec();
        // 60s elapsed minus the 10s currently paused => ~50s, clock is frozen there.
        check("paused session freezes the clock", d >= 49 && d <= 51, "got " + d);
        check("paused flag", paused.isPaused(), "not paused");
    }

    private static void testDayLookup() {
        LocalDate day = LocalDate.of(2026, 11, 13);
        List<WorkoutSession> all = new ArrayList<>();
        all.add(session("d", Fmt.millis(day) + 3600_000L, block("a", new double[][]{{50, 10}})));
        all.add(session("other", Fmt.millis(day.minusDays(1)), block("a", new double[][]{{50, 10}})));
        check("countOn", Stats.countOn(all, day) == 1, "got " + Stats.countOn(all, day));
        near("volumeOn", Stats.volumeOn(all, day), 500);
        check("onDay", Stats.onDay(all, day).size() == 1, "got " + Stats.onDay(all, day).size());
    }

    private static void testDurationFormat() {
        check("dur short", "05:30".equals(Fmt.duration(330)), Fmt.duration(330));
        check("dur hour", "1:00:00".equals(Fmt.duration(3600)), Fmt.duration(3600));
        check("dur negative", "00:00".equals(Fmt.duration(-5)), Fmt.duration(-5));
        check("num int", "60".equals(Fmt.num(60.0)), Fmt.num(60.0));
        check("num frac", "62.5".equals(Fmt.num(62.5)), Fmt.num(62.5));
        check("vol thousands", "1,040".equals(Fmt.vol(1040)), Fmt.vol(1040));
        check("vol k", "12.3k".equals(Fmt.vol(12345)), Fmt.vol(12345));
    }
}
