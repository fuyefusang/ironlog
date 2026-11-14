package com.ironlog.train.data;

import java.util.ArrayList;
import java.util.List;

public class SessionExercise {
    public String exerciseId;
    public String note;
    public int restSec = 90;
    public List<SetEntry> sets = new ArrayList<>();

    public SessionExercise() {
    }

    public SessionExercise(String exerciseId) {
        this.exerciseId = exerciseId;
    }

    public double volume() {
        double v = 0;
        for (SetEntry s : sets) {
            if (s.done && s.type != SetEntry.WARMUP) {
                v += s.weight * s.reps;
            }
        }
        return v;
    }

    public int doneSets() {
        int n = 0;
        for (SetEntry s : sets) {
            if (s.done) {
                n++;
            }
        }
        return n;
    }
}
