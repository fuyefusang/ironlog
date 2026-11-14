package com.ironlog.train.ui;

import android.app.Activity;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.ironlog.train.data.Store;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private static final String[] TABS = {"训练", "数据", "我的"};
    private static final int[] TAB_ICONS = {
            com.ironlog.train.R.drawable.ic_dumbbell,
            com.ironlog.train.R.drawable.ic_chart,
            com.ironlog.train.R.drawable.ic_person
    };

    private FrameLayout content;
    private LinearLayout nav;
    private final List<Screen> stack = new ArrayList<>();
    private int tab = -1;

    private final ImageView[] icons = new ImageView[4];
    private final TextView[] labels = new TextView[4];

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        Store store = Store.get(this);
        Ui.applyTheme(store.settings.darkMode);
        setTheme(store.settings.darkMode
                ? com.ironlog.train.R.style.Theme_IronLog
                : com.ironlog.train.R.style.Theme_IronLog_Light);

        Window w = getWindow();
        w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        w.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Ui.BG));
        w.setStatusBarColor(Ui.BG);
        w.setNavigationBarColor(Ui.BG);
        applyBarIconMode(!store.settings.darkMode);

        LinearLayout root = Ui.col(this);
        root.setBackgroundColor(Ui.BG);

        content = new FrameLayout(this);
        content.setBackgroundColor(Ui.BG);
        root.addView(content, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        nav = Ui.row(this);
        nav.setBackgroundColor(Ui.NAV);
        nav.setPadding(0, Ui.dp(this, 6), 0, Ui.dp(this, 6));
        root.addView(nav, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 58)));

        for (int i = 0; i < TABS.length; i++) {
            nav.addView(buildTab(i), new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.MATCH_PARENT, 1f));
        }

        setContentView(root);

        // Android 15+ forces edge-to-edge for target 35+; pad the root ourselves.
        if (Build.VERSION.SDK_INT >= 30) {
            InsetsHelper.applyEdgeToEdge(this, root);
        }

        switchTab(b != null ? Math.max(0, Math.min(TABS.length - 1, b.getInt("tab", 0))) : 0);

        com.ironlog.train.notify.PlanReminders.ensureChannel(this);
        com.ironlog.train.notify.PlanReminders.schedule(this);
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putInt("tab", tab);
    }

    private View buildTab(final int index) {
        LinearLayout item = Ui.col(this);
        item.setGravity(Gravity.CENTER);
        item.setClickable(true);

        ImageView icon = Icons.iv(this, TAB_ICONS[index], 22, Ui.TEXT3);
        LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(
                Ui.dp(this, 22), Ui.dp(this, 22));
        ilp.bottomMargin = Ui.dp(this, 3);
        item.addView(icon, ilp);

        TextView label = Ui.t(this, TABS[index], 10.5f, Ui.TEXT3);
        label.setGravity(Gravity.CENTER);
        item.addView(label);

        item.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(index);
            }
        });

        icons[index] = icon;
        labels[index] = label;
        return item;
    }

    /** System bar icons must be dark on a light background and vice versa. */
    private void applyBarIconMode(boolean lightBackground) {
        View decor = getWindow().getDecorView();
        int flags = decor.getSystemUiVisibility();
        if (lightBackground) {
            flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            if (Build.VERSION.SDK_INT >= 26) {
                flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            }
        } else {
            flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            if (Build.VERSION.SDK_INT >= 26) {
                flags &= ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            }
        }
        decor.setSystemUiVisibility(flags);
    }

    public void switchTab(int index) {
        if (index == tab) {
            // tapping the active tab returns to its root
            while (stack.size() > 1) {
                popInternal();
            }
            return;
        }
        tab = index;
        for (int i = 0; i < TABS.length; i++) {
            boolean on = i == index;
            labels[i].setTextColor(on ? Ui.ACCENT : Ui.TEXT3);
            icons[i].setImageDrawable(Icons.d(this, TAB_ICONS[i], on ? Ui.ACCENT : Ui.TEXT3));
        }
        content.removeAllViews();
        stack.clear();
        push(root(index));
    }

    private Screen root(int index) {
        switch (index) {
            case 1:
                return new StatsScreen(this);
            case 2:
                return new ProfileScreen(this);
            case 0:
            default:
                return new HomeScreen(this);
        }
    }

    public void push(Screen s) {
        if (!stack.isEmpty()) {
            content.removeView(stack.get(stack.size() - 1));
        }
        stack.add(s);
        s.mount();
        content.addView(s);
    }

    private void popInternal() {
        if (stack.size() <= 1) {
            return;
        }
        Screen top = stack.remove(stack.size() - 1);
        content.removeView(top);
        Screen prev = stack.get(stack.size() - 1);
        if (prev.getParent() == null) {
            content.addView(prev);
        }
        prev.refresh();
    }

    /** Pop one screen; returns false when already at the tab root. */
    public boolean pop() {
        if (stack.size() <= 1) {
            return false;
        }
        popInternal();
        return true;
    }

    public void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    // ------------------------------------------------------- permissions

    private static final int REQ_PERM = 4310;
    private Runnable pendingPermissionAction;

    /** Runs {@code then} once every permission is granted, asking first if needed. */
    public void ensurePermissions(String[] permissions, Runnable then) {
        boolean allGranted = true;
        for (String p : permissions) {
            if (checkSelfPermission(p) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }
        if (allGranted) {
            then.run();
            return;
        }
        pendingPermissionAction = then;
        requestPermissions(permissions, REQ_PERM);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQ_PERM) {
            Runnable action = pendingPermissionAction;
            pendingPermissionAction = null;
            boolean all = grantResults.length > 0;
            for (int r : grantResults) {
                if (r != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    all = false;
                }
            }
            if (all && action != null) {
                action.run();
            } else {
                toast("没有拿到权限，这个功能暂时用不了");
            }
            return;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    // ------------------------------------------------------- activity results

    public interface ResultHandler {
        void onResult(int resultCode, android.content.Intent data);
    }

    private static final int REQ_PICK_FILE = 4201;
    private ResultHandler pendingResult;

    public void startForResult(android.content.Intent intent, ResultHandler handler) {
        pendingResult = handler;
        try {
            startActivityForResult(intent, REQ_PICK_FILE);
        } catch (Exception e) {
            pendingResult = null;
            toast("没有可用的文件选择器");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, android.content.Intent data) {
        if (requestCode == REQ_PICK_FILE) {
            ResultHandler h = pendingResult;
            pendingResult = null;
            if (h != null) {
                h.onResult(resultCode, data);
            }
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    /** Rebuild the current screen tree from scratch (after a data change). */
    public void reloadCurrent() {
        if (stack.isEmpty()) {
            return;
        }
        stack.get(stack.size() - 1).refresh();
    }

    @Override
    public void onBackPressed() {
        if (stack.isEmpty()) {
            super.onBackPressed();
            return;
        }
        Screen top = stack.get(stack.size() - 1);
        if (top.onBack()) {
            return;
        }
        if (pop()) {
            return;
        }
        if (tab != 0) {
            switchTab(0);
            return;
        }
        super.onBackPressed();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!stack.isEmpty()) {
            stack.get(stack.size() - 1).onShown();
        }
    }

    /** Convenience for building inline toast-like feedback. */
    public void flash(String msg) {
        Toast t = Toast.makeText(this, msg, Toast.LENGTH_SHORT);
        t.show();
    }

    public static int dim(int color, float factor) {
        int a = Color.alpha(color);
        int r = (int) (Color.red(color) * factor);
        int g = (int) (Color.green(color) * factor);
        int b = (int) (Color.blue(color) * factor);
        return Color.argb(a, Math.min(255, r), Math.min(255, g), Math.min(255, b));
    }
}
