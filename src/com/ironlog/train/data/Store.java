package com.ironlog.train.data;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Single-file JSON persistence. Everything lives on the device: no account, no network.
 */
public class Store {

    private static final String TAG = "IronLogStore";
    private static final String FILE = "ironlog-data.json";
    private static final String FORMAT_KEY = "format";
    private static final int FORMAT = 1;

    private static Store instance;

    public Settings settings = new Settings();
    public List<Exercise> customExercises = new ArrayList<>();
    public List<Routine> routines = new ArrayList<>();
    public List<WorkoutSession> sessions = new ArrayList<>();
    public List<BodyMetric> metrics = new ArrayList<>();

    private final Context app;
    private boolean loading;

    public static class Settings {
        public String unit = "kg";
        public String displayName = "";
        public boolean restSound = true;
        public boolean keepAwake = true;
        public boolean darkMode = true;
        public boolean reminderEnabled = false;
        public int reminderHour = 19;
        public int reminderMinute = 0;
    }

    private Store(Context ctx) {
        this.app = ctx.getApplicationContext();
    }

    public static synchronized Store get(Context ctx) {
        if (instance == null) {
            instance = new Store(ctx);
            instance.load();
        }
        return instance;
    }

    public static synchronized Store peek() {
        return instance;
    }

    public static String newId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    // ------------------------------------------------------------------ io

    private File file() {
        return new File(app.getFilesDir(), FILE);
    }

    public void load() {
        loading = true;
        try {
            File f = file();
            if (!f.exists()) {
                seedDefaults();
                return;
            }
            String text = readAll(new FileInputStream(f));
            JSONObject root = new JSONObject(text);
            settings = Json.fromJson(root.optJSONObject("settings"), Settings.class);
            if (settings == null) {
                settings = new Settings();
            }
            customExercises = Json.fromJsonArray(root.optJSONArray("customExercises"), Exercise.class);
            routines = Json.fromJsonArray(root.optJSONArray("routines"), Routine.class);
            sessions = Json.fromJsonArray(root.optJSONArray("sessions"), WorkoutSession.class);
            metrics = Json.fromJsonArray(root.optJSONArray("metrics"), BodyMetric.class);
        } catch (Exception e) {
            Log.w(TAG, "load failed, starting empty", e);
            seedDefaults();
        } finally {
            loading = false;
        }
        normalize();
    }

    private void normalize() {
        if (sessions == null) sessions = new ArrayList<>();
        if (routines == null) routines = new ArrayList<>();
        if (metrics == null) metrics = new ArrayList<>();
        if (customExercises == null) customExercises = new ArrayList<>();
        if (settings == null) settings = new Settings();
        // Drop drafts that were started but never received a single exercise.
        for (int i = sessions.size() - 1; i >= 0; i--) {
            WorkoutSession s = sessions.get(i);
            if (s.isActive() && s.startedAt == 0L && (s.exercises == null || s.exercises.isEmpty())) {
                sessions.remove(i);
            }
        }
        Collections.sort(sessions, new Comparator<WorkoutSession>() {
            @Override
            public int compare(WorkoutSession a, WorkoutSession b) {
                return Long.compare(b.startedAt, a.startedAt);
            }
        });
    }

    private void seedDefaults() {
        settings = new Settings();
        customExercises = new ArrayList<>();
        routines = new ArrayList<>();
        sessions = new ArrayList<>();
        metrics = new ArrayList<>();
    }

    public void save() {
        if (loading) {
            return;
        }
        try {
            JSONObject root = new JSONObject();
            root.put(FORMAT_KEY, FORMAT);
            root.put("settings", Json.toJson(settings));
            root.put("customExercises", Json.toJsonArray(customExercises));
            root.put("routines", Json.toJsonArray(routines));
            root.put("sessions", Json.toJsonArray(sessions));
            root.put("metrics", Json.toJsonArray(metrics));
            writeAll(root.toString(2));
        } catch (Exception e) {
            Log.w(TAG, "save failed", e);
        }
    }

    public String exportJson() {
        try {
            JSONObject root = new JSONObject();
            root.put(FORMAT_KEY, FORMAT);
            root.put("settings", Json.toJson(settings));
            root.put("customExercises", Json.toJsonArray(customExercises));
            root.put("routines", Json.toJsonArray(routines));
            root.put("sessions", Json.toJsonArray(sessions));
            root.put("metrics", Json.toJsonArray(metrics));
            return root.toString(2);
        } catch (Exception e) {
            return "{}";
        }
    }

    public boolean importJson(String text) {
        try {
            JSONObject root = new JSONObject(text);
            if (!root.has("sessions") && !root.has("routines")) {
                return false;
            }
            settings = Json.fromJson(root.optJSONObject("settings"), Settings.class);
            if (settings == null) settings = new Settings();
            customExercises = Json.fromJsonArray(root.optJSONArray("customExercises"), Exercise.class);
            routines = Json.fromJsonArray(root.optJSONArray("routines"), Routine.class);
            sessions = Json.fromJsonArray(root.optJSONArray("sessions"), WorkoutSession.class);
            metrics = Json.fromJsonArray(root.optJSONArray("metrics"), BodyMetric.class);
            normalize();
            save();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void writeAll(String text) throws Exception {
        FileOutputStream fos = new FileOutputStream(file());
        try {
            fos.write(text.getBytes(Charset.forName("UTF-8")));
            fos.flush();
        } finally {
            fos.close();
        }
    }

    private String readAll(InputStream in) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) {
            bos.write(buf, 0, n);
        }
        in.close();
        return new String(bos.toByteArray(), Charset.forName("UTF-8"));
    }

    public void wipe() {
        seedDefaults();
        save();
    }

    // -------------------------------------------------------------- lookups

    public List<Exercise> allExercises() {
        List<Exercise> all = new ArrayList<>(Catalog.builtIn());
        all.addAll(customExercises);
        return all;
    }

    public Exercise exercise(String id) {
        if (id == null) {
            return null;
        }
        for (Exercise e : Catalog.builtIn()) {
            if (id.equals(e.id)) {
                return e;
            }
        }
        for (Exercise e : customExercises) {
            if (id.equals(e.id)) {
                return e;
            }
        }
        return null;
    }

    public String exerciseName(String id) {
        Exercise e = exercise(id);
        return e == null ? "已删除动作" : e.name;
    }

    // -------------------------------------------------------------- sessions

    public WorkoutSession activeSession() {
        for (WorkoutSession s : sessions) {
            if (s.isActive()) {
                return s;
            }
        }
        return null;
    }

    public List<WorkoutSession> finishedSessions() {
        List<WorkoutSession> out = new ArrayList<>();
        for (WorkoutSession s : sessions) {
            if (!s.isActive()) {
                out.add(s);
            }
        }
        return out;
    }

    public void addSession(WorkoutSession s) {
        sessions.add(0, s);
        save();
    }

    public void removeSession(WorkoutSession s) {
        sessions.remove(s);
        save();
    }

    /** Last time this exercise was trained, newest first. */
    public List<WorkoutSession> sessionsWithExercise(String exerciseId) {
        List<WorkoutSession> out = new ArrayList<>();
        for (WorkoutSession s : finishedSessions()) {
            for (SessionExercise se : s.exercises) {
                if (exerciseId.equals(se.exerciseId) && se.doneSets() > 0) {
                    out.add(s);
                    break;
                }
            }
        }
        return out;
    }

    public SessionExercise lastEntryFor(String exerciseId) {
        List<WorkoutSession> list = sessionsWithExercise(exerciseId);
        if (list.isEmpty()) {
            return null;
        }
        for (SessionExercise se : list.get(0).exercises) {
            if (exerciseId.equals(se.exerciseId)) {
                return se;
            }
        }
        return null;
    }
}
