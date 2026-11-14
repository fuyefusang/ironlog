package com.ironlog.train.ui;

import android.app.Activity;
import android.graphics.Insets;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;

/**
 * Edge-to-edge plumbing kept in its own class so that devices below API 30 never
 * load it (the framework classes it touches only exist on 30+).
 */
public final class InsetsHelper {

    private InsetsHelper() {
    }

    public static void applyEdgeToEdge(Activity activity, final View root) {
        Window window = activity.getWindow();
        window.setDecorFitsSystemWindows(false);
        root.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
            @Override
            public WindowInsets onApplyWindowInsets(View v, WindowInsets insets) {
                Insets bars = insets.getInsets(WindowInsets.Type.systemBars());
                Insets ime = insets.getInsets(WindowInsets.Type.ime());
                v.setPadding(0, bars.top, 0, Math.max(bars.bottom, ime.bottom));
                return insets;
            }
        });
    }
}
