package local.homereturn;

import android.accessibilityservice.AccessibilityService;
import android.content.ComponentName;
import android.content.Intent;
import android.os.SystemClock;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

/**
 * Fire OS keeps Home for Amazon's launcher, so when its home screen appears, open the custom launcher instead.
 * If Amazon's home appears again within ESCAPE_MS of a redirect (Home pressed twice), leave it on screen, and keep
 * leaving it there while the user moves around Amazon's own menus (sidebar, settings) and comes back to it.
 * The next Home press made from Amazon's home itself, or a return to it from any other app, redirects again.
 *
 * Remote app buttons go through the Appstore's AppLaunchActivity. For an app that is disabled or missing (the
 * Amazon Music button) the Appstore then shows that app's details page; when that page follows a button launch
 * within BUTTON_MS, open YouTube instead. Buttons for installed apps never reach the details page.
 */
public class HomeReturnService extends AccessibilityService {
    private static final String TAG = "HomeReturn";
    private static final String AMAZON_PKG = "com.amazon.tv.launcher";
    private static final String AMAZON_HOME = "com.amazon.tv.launcher.ui.HomeActivity_vNext";
    private static final String APP_LAUNCH = "com.amazon.venezia.pdi.AppLaunchActivity";
    private static final String APP_DETAILS = "com.amazon.venezia.details.AppDetailsActivity";
    private static final ComponentName LAUNCHER = new ComponentName("com.gathin.tvhome", "com.gathin.tvhome.MainActivity");
    private static final ComponentName BUTTON_APP = new ComponentName("com.amazon.firetv.youtube", "dev.cobalt.app.MainActivity");
    private static final long ESCAPE_MS = 3000;
    /** Amazon's home reports its window more than once per appearance (Fire OS 8 on ginza). */
    private static final long DUP_MS = 800;
    private static final long BUTTON_MS = 5000;

    private long lastRedirect;
    private long lastHomeEvent;
    private long buttonLaunch;
    /** The user chose to stay on Amazon's home (double Home). */
    private boolean escaped;
    /** The last window seen was one of Amazon's launcher screens other than home (sidebar, settings menu). */
    private boolean inAmazonMenu;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return;
        String cls = event.getClassName() == null ? "" : event.getClassName().toString();
        String pkg = event.getPackageName() == null ? "" : event.getPackageName().toString();
        long now = SystemClock.elapsedRealtime();
        if (APP_LAUNCH.equals(cls)) {
            buttonLaunch = now;
            inAmazonMenu = false;
        } else if (APP_DETAILS.equals(cls)) {
            inAmazonMenu = false;
            if (buttonLaunch != 0 && now - buttonLaunch < BUTTON_MS) {
                buttonLaunch = 0;
                open(BUTTON_APP);
            }
        } else if (AMAZON_HOME.equals(cls)) {
            boolean backFromMenu = inAmazonMenu;
            inAmazonMenu = false;
            long gap = now - lastHomeEvent;
            lastHomeEvent = now;
            if (gap < DUP_MS) return;
            if (escaped && backFromMenu) {
                Log.i(TAG, "back on Amazon home from its own menu: staying on it");
                return;
            }
            if (now - lastRedirect < ESCAPE_MS) {
                lastRedirect = 0;
                escaped = true;
                Log.i(TAG, "Amazon home again within " + ESCAPE_MS + " ms: staying on it");
                return;
            }
            escaped = false;
            lastRedirect = now;
            Log.i(TAG, "Amazon home: opening the launcher");
            open(LAUNCHER);
        } else if (AMAZON_PKG.equals(pkg) && cls.startsWith(AMAZON_PKG + ".")) {
            // Sidebar (navigation.NavigationActivity), settings menu (ui.MainSettingsActivity) and the like.
            inAmazonMenu = true;
        }
    }

    private void open(ComponentName target) {
        try {
            startActivity(new Intent(Intent.ACTION_MAIN).setComponent(target)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED));
        } catch (RuntimeException e) {
            // Target uninstalled or disabled: leave the current screen alone.
            Log.w(TAG, "could not open " + target.flattenToShortString() + ": " + e);
        }
    }

    @Override
    public void onInterrupt() {
    }
}
