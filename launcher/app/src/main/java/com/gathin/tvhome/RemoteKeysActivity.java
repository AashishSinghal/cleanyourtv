package com.gathin.tvhome;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * "Remote buttons": press a button on the remote, then pick the app it opens. The buttons come from the key
 * services (RemoteKeyLearn), which never hand over the ones RemoteKeyPolicy protects (D-pad, Back, Home, power,
 * volume, playback and the like). The list also shows the buttons a theme assigned; picking another app for one of
 * those makes an on-TV choice that wins over the theme.
 */
public final class RemoteKeysActivity extends Activity {
    private TextView learnRow;
    private TextView learnNote;
    private TextView emptyNote;
    private LinearLayout rows;
    private boolean listening;

    private static final class App {
        final String label;
        final String pkg;

        App(String label, String pkg) {
            this.label = label;
            this.pkg = pkg;
        }
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_remote_keys);
        learnRow = findViewById(R.id.learnRow);
        learnNote = findViewById(R.id.learnNote);
        emptyNote = findViewById(R.id.emptyNote);
        rows = findViewById(R.id.assignedRows);
        learnRow.setOnClickListener(v -> {
            if (listening) stopListening();
            else startListening();
        });
        refresh();
        learnRow.requestFocus();
    }

    @Override
    protected void onPause() {
        stopListening();
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        if (listening) {
            stopListening();
            return;
        }
        super.onBackPressed();
    }

    private void startListening() {
        if (!RemoteKeyLearn.helperRunning()) {
            Toast.makeText(this, R.string.remote_keys_helper_off, Toast.LENGTH_LONG).show();
            return;
        }
        listening = true;
        learnRow.setText(R.string.remote_keys_cancel);
        learnNote.setText(R.string.remote_keys_listening);
        RemoteKeyLearn.start(this::onButton);
    }

    private void stopListening() {
        RemoteKeyLearn.stop();
        if (!listening) return;
        listening = false;
        learnRow.setText(R.string.remote_keys_add);
        learnNote.setText(R.string.remote_keys_protected_hint);
    }

    /** The key services took a button that is allowed to be assigned. */
    private void onButton(int keyCode, int scanCode) {
        stopListening();
        String id = RemoteKeyChoices.id(keyCode, scanCode);
        chooseApp(id, buttonLabel(keyCode, scanCode), RemoteKeyChoices.load(this).containsKey(id));
    }

    private static String buttonLabel(int keyCode, int scanCode) {
        String name = keyCode > 0 ? keyName(keyCode) : "Button";
        return scanCode > 0 ? name + " (scancode " + scanCode + ")" : name;
    }

    /** A theme's scancode entry: the four coloured buttons have standard Linux scancodes, anything else a number. */
    private static String scanLabel(int scanCode) {
        switch (scanCode) {
            case 398: return "Red button (scancode 398)";
            case 399: return "Green button (scancode 399)";
            case 400: return "Yellow button (scancode 400)";
            case 401: return "Blue button (scancode 401)";
            default: return "scancode " + scanCode;
        }
    }

    private static String keyName(int keyCode) {
        return KeyEvent.keyCodeToString(keyCode).replace("KEYCODE_", "");
    }

    private void chooseApp(String id, String label, boolean removable) {
        List<App> apps = tvApps();
        if (apps.isEmpty()) {
            Toast.makeText(this, R.string.remote_keys_no_apps, Toast.LENGTH_LONG).show();
            return;
        }
        String[] names = new String[apps.size()];
        for (int i = 0; i < names.length; i++) names[i] = apps.get(i).label;
        AlertDialog.Builder dialog = new AlertDialog.Builder(this, R.style.RemoteKeysDialog)
                .setTitle(getString(R.string.remote_keys_choose, label))
                .setItems(names, (d, which) -> {
                    App app = apps.get(which);
                    if (RemoteKeyChoices.put(this, id, label, app.pkg)) {
                        Toast.makeText(this, getString(R.string.remote_keys_saved, label, app.label),
                                Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, R.string.remote_keys_cant, Toast.LENGTH_LONG).show();
                    }
                    refresh();
                })
                .setNegativeButton(android.R.string.cancel, null);
        if (removable) {
            dialog.setNeutralButton(R.string.remote_keys_remove, (d, which) -> {
                RemoteKeyChoices.remove(this, id);
                Toast.makeText(this, R.string.remote_keys_removed, Toast.LENGTH_SHORT).show();
                refresh();
            });
        }
        dialog.show();
    }

    /** TV apps by name, without this launcher. */
    private List<App> tvApps() {
        PackageManager pm = getPackageManager();
        Intent main = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
        Set<String> seen = new HashSet<>();
        List<App> apps = new ArrayList<>();
        for (ResolveInfo info : pm.queryIntentActivities(main, 0)) {
            String pkg = info.activityInfo.packageName;
            if (pkg.equals(getPackageName()) || !seen.add(pkg)) continue;
            apps.add(new App(String.valueOf(info.loadLabel(pm)), pkg));
        }
        Collator collator = Collator.getInstance();
        Collections.sort(apps, (a, b) -> collator.compare(a.label, b.label));
        return apps;
    }

    private String appLabel(String pkg) {
        try {
            PackageManager pm = getPackageManager();
            return String.valueOf(pm.getApplicationInfo(pkg, 0).loadLabel(pm));
        } catch (PackageManager.NameNotFoundException e) {
            return pkg + " (" + getString(R.string.remote_keys_not_installed) + ")";
        }
    }

    private void refresh() {
        rows.removeAllViews();
        Map<String, RemoteKeyChoices.Choice> mine = RemoteKeyChoices.load(this);
        int count = 0;
        for (RemoteKeyChoices.Choice c : mine.values()) {
            addRow(c.id, c.label.isEmpty() ? c.id : c.label, c.pkg, true);
            count++;
        }
        // Buttons a theme assigned, unless an on-TV choice already replaces them.
        Config config = Config.load(this);
        for (Map.Entry<Integer, String> e : new TreeMap<>(RemoteKeys.parseAppScancodes(config.appKeys)).entrySet()) {
            String id = "scan:" + e.getKey();
            if (mine.containsKey(id)) continue;
            addRow(id, scanLabel(e.getKey()), e.getValue(), false);
            count++;
        }
        for (Map.Entry<Integer, String> e : new TreeMap<>(RemoteKeys.parseAppKeys(config.appKeys)).entrySet()) {
            String id = String.valueOf(e.getKey());
            if (mine.containsKey(id)) continue;
            addRow(id, keyName(e.getKey()), e.getValue(), false);
            count++;
        }
        emptyNote.setVisibility(count == 0 ? View.VISIBLE : View.GONE);
        learnRow.requestFocus();
    }

    private void addRow(String id, String label, String pkg, boolean mine) {
        TextView row = new TextView(this, null, 0, R.style.SettingsRow);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(52));
        params.topMargin = dp(10);
        row.setLayoutParams(params);
        row.setSingleLine(true);
        row.setEllipsize(TextUtils.TruncateAt.END);
        row.setText(label + "   →   " + appLabel(pkg)
                + (mine ? "" : "   (" + getString(R.string.remote_keys_from_theme) + ")"));
        row.setOnClickListener(v -> chooseApp(id, label, mine));
        rows.addView(row);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
