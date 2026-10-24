package com.gathin.tvhome;

import android.view.KeyEvent;

/**
 * Remote buttons that the on-TV "Remote buttons" screen never hands to an app: moving around and confirming,
 * Back and Home, power, volume, playback, channels and inputs, search and voice, and the typing keys. Taking one
 * of these over could leave the TV without a way to use it. The coloured buttons, the app buttons (Netflix, Prime
 * Video and the like) and any button that has no job on the TV stay assignable.
 *
 * The check is on the Android keycode, so a button is protected whatever its scancode is. Entries a theme lists
 * in app_keys are the owner's own file and are not filtered here.
 */
final class RemoteKeyPolicy {

    private RemoteKeyPolicy() { }

    static boolean isProtected(int keyCode) {
        if (keyCode >= KeyEvent.KEYCODE_0 && keyCode <= KeyEvent.KEYCODE_9) return true;
        if (keyCode >= KeyEvent.KEYCODE_A && keyCode <= KeyEvent.KEYCODE_Z) return true;
        if (keyCode >= KeyEvent.KEYCODE_NUMPAD_0 && keyCode <= KeyEvent.KEYCODE_NUMPAD_9) return true;
        switch (keyCode) {
            // Moving around, confirming, going back
            case KeyEvent.KEYCODE_DPAD_UP:
            case KeyEvent.KEYCODE_DPAD_DOWN:
            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_DPAD_RIGHT:
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_DPAD_UP_LEFT:
            case KeyEvent.KEYCODE_DPAD_DOWN_LEFT:
            case KeyEvent.KEYCODE_DPAD_UP_RIGHT:
            case KeyEvent.KEYCODE_DPAD_DOWN_RIGHT:
            case KeyEvent.KEYCODE_SYSTEM_NAVIGATION_UP:
            case KeyEvent.KEYCODE_SYSTEM_NAVIGATION_DOWN:
            case KeyEvent.KEYCODE_SYSTEM_NAVIGATION_LEFT:
            case KeyEvent.KEYCODE_SYSTEM_NAVIGATION_RIGHT:
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_NUMPAD_ENTER:
            case KeyEvent.KEYCODE_ESCAPE:
            case KeyEvent.KEYCODE_BACK:
            case KeyEvent.KEYCODE_HOME:
            case KeyEvent.KEYCODE_MENU:
            // Typing and scrolling
            case KeyEvent.KEYCODE_TAB:
            case KeyEvent.KEYCODE_SPACE:
            case KeyEvent.KEYCODE_DEL:
            case KeyEvent.KEYCODE_FORWARD_DEL:
            case KeyEvent.KEYCODE_MOVE_HOME:
            case KeyEvent.KEYCODE_MOVE_END:
            case KeyEvent.KEYCODE_PAGE_UP:
            case KeyEvent.KEYCODE_PAGE_DOWN:
            // A game controller's A, B and menu buttons are OK and Back in the TV's screens
            case KeyEvent.KEYCODE_BUTTON_A:
            case KeyEvent.KEYCODE_BUTTON_B:
            case KeyEvent.KEYCODE_BUTTON_START:
            case KeyEvent.KEYCODE_BUTTON_SELECT:
            case KeyEvent.KEYCODE_BUTTON_MODE:
            // Power and sleep
            case KeyEvent.KEYCODE_POWER:
            case KeyEvent.KEYCODE_SLEEP:
            case KeyEvent.KEYCODE_WAKEUP:
            case KeyEvent.KEYCODE_SOFT_SLEEP:
            case KeyEvent.KEYCODE_TV_POWER:
            case KeyEvent.KEYCODE_STB_POWER:
            case KeyEvent.KEYCODE_AVR_POWER:
            // Volume
            case KeyEvent.KEYCODE_VOLUME_UP:
            case KeyEvent.KEYCODE_VOLUME_DOWN:
            case KeyEvent.KEYCODE_VOLUME_MUTE:
            case KeyEvent.KEYCODE_MUTE:
            // Playback
            case KeyEvent.KEYCODE_MEDIA_PLAY:
            case KeyEvent.KEYCODE_MEDIA_PAUSE:
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
            case KeyEvent.KEYCODE_MEDIA_STOP:
            case KeyEvent.KEYCODE_MEDIA_NEXT:
            case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
            case KeyEvent.KEYCODE_MEDIA_REWIND:
            case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
            case KeyEvent.KEYCODE_MEDIA_RECORD:
            case KeyEvent.KEYCODE_MEDIA_CLOSE:
            case KeyEvent.KEYCODE_MEDIA_EJECT:
            case KeyEvent.KEYCODE_MEDIA_SKIP_FORWARD:
            case KeyEvent.KEYCODE_MEDIA_SKIP_BACKWARD:
            case KeyEvent.KEYCODE_MEDIA_STEP_FORWARD:
            case KeyEvent.KEYCODE_MEDIA_STEP_BACKWARD:
            // Channels and inputs
            case KeyEvent.KEYCODE_CHANNEL_UP:
            case KeyEvent.KEYCODE_CHANNEL_DOWN:
            case KeyEvent.KEYCODE_LAST_CHANNEL:
            case KeyEvent.KEYCODE_TV:
            case KeyEvent.KEYCODE_TV_INPUT:
            case KeyEvent.KEYCODE_TV_INPUT_HDMI_1:
            case KeyEvent.KEYCODE_TV_INPUT_HDMI_2:
            case KeyEvent.KEYCODE_TV_INPUT_HDMI_3:
            case KeyEvent.KEYCODE_TV_INPUT_HDMI_4:
            case KeyEvent.KEYCODE_TV_INPUT_COMPOSITE_1:
            case KeyEvent.KEYCODE_TV_INPUT_COMPOSITE_2:
            case KeyEvent.KEYCODE_TV_INPUT_COMPONENT_1:
            case KeyEvent.KEYCODE_TV_INPUT_COMPONENT_2:
            case KeyEvent.KEYCODE_TV_INPUT_VGA_1:
            case KeyEvent.KEYCODE_STB_INPUT:
            case KeyEvent.KEYCODE_AVR_INPUT:
            // Settings, search, voice, the system's own screens
            case KeyEvent.KEYCODE_SETTINGS:
            case KeyEvent.KEYCODE_SEARCH:
            case KeyEvent.KEYCODE_ASSIST:
            case KeyEvent.KEYCODE_VOICE_ASSIST:
            case KeyEvent.KEYCODE_APP_SWITCH:
            case KeyEvent.KEYCODE_ALL_APPS:
            case KeyEvent.KEYCODE_NOTIFICATION:
                return true;
            default:
                return false;
        }
    }

    /**
     * A pressed button can be assigned when it can be told apart (it has a scancode or a keycode) and its keycode
     * is not protected.
     */
    static boolean canAssign(int keyCode, int scanCode) {
        return (keyCode > 0 || scanCode > 0) && !isProtected(keyCode);
    }
}
