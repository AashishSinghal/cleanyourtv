package com.gathin.tvhome;

import android.view.KeyEvent;
import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RemoteKeyPolicyTest {

    @Test public void navigationAndConfirmationAreProtected() {
        int[] keys = {KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_DPAD_LEFT,
                KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER,
                KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_HOME, KeyEvent.KEYCODE_MENU, KeyEvent.KEYCODE_ESCAPE};
        for (int key : keys) assertTrue("keycode " + key, RemoteKeyPolicy.isProtected(key));
    }

    @Test public void powerVolumeAndPlaybackAreProtected() {
        int[] keys = {KeyEvent.KEYCODE_POWER, KeyEvent.KEYCODE_SLEEP, KeyEvent.KEYCODE_WAKEUP,
                KeyEvent.KEYCODE_TV_POWER, KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_VOLUME_DOWN,
                KeyEvent.KEYCODE_VOLUME_MUTE, KeyEvent.KEYCODE_MEDIA_PLAY, KeyEvent.KEYCODE_MEDIA_PAUSE,
                KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, KeyEvent.KEYCODE_MEDIA_STOP, KeyEvent.KEYCODE_MEDIA_NEXT,
                KeyEvent.KEYCODE_MEDIA_PREVIOUS, KeyEvent.KEYCODE_MEDIA_REWIND, KeyEvent.KEYCODE_MEDIA_FAST_FORWARD};
        for (int key : keys) assertTrue("keycode " + key, RemoteKeyPolicy.isProtected(key));
    }

    @Test public void channelsInputsSearchAndVoiceAreProtected() {
        int[] keys = {KeyEvent.KEYCODE_CHANNEL_UP, KeyEvent.KEYCODE_CHANNEL_DOWN, KeyEvent.KEYCODE_TV,
                KeyEvent.KEYCODE_TV_INPUT, KeyEvent.KEYCODE_TV_INPUT_HDMI_1, KeyEvent.KEYCODE_TV_INPUT_HDMI_4,
                KeyEvent.KEYCODE_SETTINGS, KeyEvent.KEYCODE_SEARCH, KeyEvent.KEYCODE_ASSIST,
                KeyEvent.KEYCODE_VOICE_ASSIST, KeyEvent.KEYCODE_APP_SWITCH, KeyEvent.KEYCODE_ALL_APPS};
        for (int key : keys) assertTrue("keycode " + key, RemoteKeyPolicy.isProtected(key));
    }

    @Test public void typingKeysAreProtected() {
        assertTrue(RemoteKeyPolicy.isProtected(KeyEvent.KEYCODE_0));
        assertTrue(RemoteKeyPolicy.isProtected(KeyEvent.KEYCODE_9));
        assertTrue(RemoteKeyPolicy.isProtected(KeyEvent.KEYCODE_A));
        assertTrue(RemoteKeyPolicy.isProtected(KeyEvent.KEYCODE_Z));
        assertTrue(RemoteKeyPolicy.isProtected(KeyEvent.KEYCODE_NUMPAD_5));
        assertTrue(RemoteKeyPolicy.isProtected(KeyEvent.KEYCODE_DEL));
        assertTrue(RemoteKeyPolicy.isProtected(KeyEvent.KEYCODE_SPACE));
        assertTrue(RemoteKeyPolicy.isProtected(KeyEvent.KEYCODE_TAB));
    }

    @Test public void gamepadOkAndBackAreProtected() {
        assertTrue(RemoteKeyPolicy.isProtected(KeyEvent.KEYCODE_BUTTON_A));
        assertTrue(RemoteKeyPolicy.isProtected(KeyEvent.KEYCODE_BUTTON_B));
        assertTrue(RemoteKeyPolicy.isProtected(KeyEvent.KEYCODE_BUTTON_START));
    }

    @Test public void colouredAndAppButtonsStayAssignable() {
        int[] keys = {KeyEvent.KEYCODE_PROG_RED, KeyEvent.KEYCODE_PROG_GREEN, KeyEvent.KEYCODE_PROG_YELLOW,
                KeyEvent.KEYCODE_PROG_BLUE, KeyEvent.KEYCODE_GUIDE, KeyEvent.KEYCODE_INFO, KeyEvent.KEYCODE_CAPTIONS,
                KeyEvent.KEYCODE_BUTTON_3, KeyEvent.KEYCODE_BUTTON_5, KeyEvent.KEYCODE_BUTTON_8};
        for (int key : keys) assertFalse("keycode " + key, RemoteKeyPolicy.isProtected(key));
    }

    @Test public void aButtonNeedsAKeycodeOrAScancode() {
        assertFalse(RemoteKeyPolicy.canAssign(0, 0));
        assertFalse(RemoteKeyPolicy.canAssign(KeyEvent.KEYCODE_UNKNOWN, 0));
        assertTrue(RemoteKeyPolicy.canAssign(KeyEvent.KEYCODE_UNKNOWN, 398));
        assertTrue(RemoteKeyPolicy.canAssign(KeyEvent.KEYCODE_PROG_RED, 398));
    }

    @Test public void aProtectedKeycodeIsNeverAssignableWhateverItsScancode() {
        assertFalse(RemoteKeyPolicy.canAssign(KeyEvent.KEYCODE_BACK, 158));
        assertFalse(RemoteKeyPolicy.canAssign(KeyEvent.KEYCODE_DPAD_CENTER, 353));
        assertFalse(RemoteKeyPolicy.canAssign(KeyEvent.KEYCODE_VOLUME_UP, 115));
        assertFalse(RemoteKeyPolicy.canAssign(KeyEvent.KEYCODE_POWER, 116));
    }
}
