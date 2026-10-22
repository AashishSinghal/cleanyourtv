package local.homereturn;

import android.accessibilityservice.AccessibilityService;
import android.content.ComponentName;
import android.content.Intent;
import android.os.SystemClock;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

/**
 * Fire OS keeps Home for Amazon's launcher, so when its home screen appears, open the custom launcher instead.
 * If Amazon's home appears again within ESCAPE_MS of a redirect (Home pressed twice), leave it on screen.
 *
 * Remote app buttons go through the Appstore's AppLaunchActivity. For an app that is disabled or missing (the
 * Amazon Music button) the Appstore then shows that app's details page; when that page follows a button launch
 * within BUTTON_MS, open YouTube instead. Buttons for installed apps never reach the details page.
 */
public class HomeReturnService extends AccessibilityService {
    private static final String TAG = "HomeReturn";
    private static final String AMAZON_HOME = "com.amazon.tv.launcher.ui.HomeActivity_vNext";
    private static final String APP_LAUNCH = "com.amazon.venezia.pdi.AppLaunchActivity";
    private static final String APP_DETAILS = "com.amazon.venezia.details.AppDetailsActivity";
    private static final ComponentName LAUNCHER = new ComponentName("com.gathin.tvhome", "com.gathin.tvhome.MainActivity");
    private static final ComponentName BUTTON_APP = new ComponentName("com.amazon.firetv.youtube", "dev.cobalt.app.MainActivity");
    private static final long ESCAPE_MS = 3000;
    private static final long BUTTON_MS = 5000;

    private long lastRedirect;
    private long buttonLaunch;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return;
        String cls = event.getClassName() == null ? "" : event.getClassName().toString();
        long now = SystemClock.elapsedRealtime();
        if (APP_LAUNCH.equals(cls)) {
            buttonLaunch = now;
        } else if (APP_DETAILS.equals(cls)) {
            if (buttonLaunch != 0 && now - buttonLaunch < BUTTON_MS) {
                buttonLaunch = 0;
                open(BUTTON_APP);
            }
        } else if (AMAZON_HOME.equals(cls)) {
            if (now - lastRedirect < ESCAPE_MS) {
                lastRedirect = 0;
                Log.i(TAG, "Amazon home again within " + ESCAPE_MS + " ms: staying on it");
                return;
            }
            lastRedirect = now;
            open(LAUNCHER);
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
