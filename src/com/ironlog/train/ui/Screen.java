package com.ironlog.train.ui;

import android.content.Context;
import android.view.ViewGroup;

/** A full-screen page. Subclasses build their view tree in {@link #build()}. */
public abstract class Screen extends android.widget.FrameLayout {

    protected final MainActivity act;
    protected final Context ctx;
    private boolean mounted;

    public Screen(MainActivity a) {
        super(a);
        this.act = a;
        this.ctx = a;
        setBackgroundColor(Ui.BG);
        setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    public final void mount() {
        if (mounted) {
            return;
        }
        mounted = true;
        safeBuild();
    }

    private void safeBuild() {
        try {
            build();
        } catch (Throwable t) {
            showBuildError(t);
        }
    }

    private void showBuildError(Throwable t) {
        removeAllViews();
        String msg = t.getClass().getSimpleName();
        if (t.getMessage() != null && !t.getMessage().isEmpty()) {
            msg += ": " + t.getMessage();
        }
        android.widget.TextView title = Ui.t(getContext(), "这个页面没能加载出来", 16f, Ui.RED);
        android.widget.TextView detail = Ui.t(getContext(), msg, 12f, Ui.TEXT2);
        detail.setPadding(0, Ui.dp(getContext(), 10), 0, 0);
        android.widget.TextView hint = Ui.t(getContext(),
                "把这段信息发给我，我按它定位问题。其他页面仍可正常切换。", 12f, Ui.TEXT3);
        hint.setPadding(0, Ui.dp(getContext(), 14), 0, 0);

        android.widget.LinearLayout box = Ui.col(getContext());
        box.setPadding(Ui.dp(getContext(), 24), Ui.dp(getContext(), 60),
                Ui.dp(getContext(), 24), Ui.dp(getContext(), 24));
        box.addView(title);
        box.addView(detail);
        box.addView(hint);
        addView(box);
    }

    protected abstract void build();

    protected void rebuild() {
        removeAllViews();
        safeBuild();
    }

    /** Return true if the screen consumed the back press. */
    public boolean onBack() {
        return false;
    }

    public void onShown() {
    }

    /** Called when data may have changed underneath. */
    public void refresh() {
        rebuild();
    }
}
