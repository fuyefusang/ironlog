package com.ironlog.train.data;

import java.util.ArrayList;
import java.util.List;

public class RoutineExercise {
    public String exerciseId;
    public int sets = 3;
    public int reps = 10;
    public double weight;
    public int restSec = 90;
    public String note;

    public RoutineExercise() {
    }

    public RoutineExercise(String exerciseId, int sets, int reps, double weight) {
        this.exerciseId = exerciseId;
        this.sets = sets;
        this.reps = reps;
        this.weight = weight;
    }
}
