package com.gathin.tvhome;

import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * "Press the button you want to assign": while the Remote buttons screen is listening, the key services (which see
 * every remote key before the system does) hand the next assignable button to it and swallow that press, so the
 * button does not do its usual job. Buttons RemoteKeyPolicy protects are never taken: they pass through and work
 * as always, which is also how Back cancels the listening. Nothing here stores anything; the screen decides.
 */
final class RemoteKeyLearn {

    interface Listener {
        void onButton(int keyCode, int scanCode);
    }

    private static volatile Listener listener;
    /** The key services that are bound now: without one, no button can be read. */
    private static final Set<Object> HELPERS = ConcurrentHashMap.newKeySet();

    private RemoteKeyLearn() { }

    static void start(Listener l) {
        listener = l;
    }

    static void stop() {
        listener = null;
    }

    static void helperConnected(Object service) {
        HELPERS.add(service);
    }

    static void helperGone(Object service) {
        HELPERS.remove(service);
    }

    static boolean helperRunning() {
        return !HELPERS.isEmpty();
    }

    /** The release of a press that was taken is dropped too, even though listening has stopped by then. */
    private static final long RELEASE_WINDOW_MS = 2000;
    private static volatile int pendingRelease = -1;
    private static volatile long pendingReleaseUntil;

    /** Called first by the key services for every key. True: the key was taken for the screen and must be dropped. */
    static boolean consume(KeyEvent event) {
        int keyCode = event.getKeyCode();
        int scanCode = event.getScanCode();
        if (event.getAction() == KeyEvent.ACTION_UP && keyCode == pendingRelease) {
            boolean fresh = event.getEventTime() <= pendingReleaseUntil;
            pendingRelease = -1;
            if (fresh) return true;
        }
        Listener l = listener;
        if (l == null) return false;
        if (!RemoteKeyPolicy.canAssign(keyCode, scanCode)) return false;
        if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
            pendingRelease = keyCode;
            pendingReleaseUntil = event.getEventTime() + RELEASE_WINDOW_MS;
            new Handler(Looper.getMainLooper()).post(() -> {
                Listener now = listener;
                if (now != null) now.onButton(keyCode, scanCode);
            });
        }
        return true;
    }
}
