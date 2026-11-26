# Amazon Fire TV Stick Lite (2020), AFTSS / sheldon

Fire OS 7.7.1.7 (build PS7717.5746N), Android 9 (SDK 28), armeabi-v7a, about 2.6 GB free storage.
Done 2026-11-26 from a Mac on the same subnet as the Stick, plain `adb` over port 5555.

## Debloat

```bash
export TV_HOST=<stick ip>
tools/disable.sh examples/amazon-fire-tv-stick-lite-2020/debloat.txt
tools/restore.sh examples/amazon-fire-tv-stick-lite-2020/debloat.txt com.amazon.tv.launcher   # undo
```

Result: 52 packages disabled (1 was before). Still enabled on purpose: `com.amazon.tv.launcher`,
`com.amazon.tv.settings.v2`, `com.amazon.venezia`, `com.amazon.vizzini` (voice), `com.amazon.firebat`
(Prime Video), Netflix, `com.amazon.tv.oobe`. Home is still `com.amazon.tv.launcher/.ui.HomeActivity_vNext`.

Take a copy of `pm list packages`, `pm list packages -d`, `getprop` and `settings list secure|system|global`
before changing anything (kept outside the repo, `backup/` is gitignored).

## The clean launcher on Fire OS: not worth it on this Stick

Tried 2026-11-26 and removed again. On this Stick (Fire OS 7, Android 9) `home-return` works but is too slow: every
Home press shows Amazon's home for about 4.5 s before the clean launcher appears (measured 4.38 to 4.66 s over four
presses). The log shows why, right after the service's launch request:
`W/ActivityManager: Activity start request from <uid> stopped`. Android 9 holds activity starts from background
apps for about 5 s after a Home press, and the service has no way to lift that. The redirect itself fires within
0.5 s; the screen change is what waits. The setup is removed (`local.homereturn` and `com.gathin.tvhome`
uninstalled, accessibility settings back to `null`/`0`) and the Stick runs Amazon's launcher with the debloat only.

Untried ideas: send the launch from an `AlarmManager` `PendingIntent` so system_server is the caller, or cover
Amazon's home with an accessibility overlay for the 5 s. Neither is tested. The FX 55" (Android 11) did not show
the delay in its notes, but that was not re-measured.

What follows is the setup as it was done (proven on the Xiaomi FX 55", AFTNE4E43F):

Fire OS does not hand the Home role to another launcher (`com.amazon.tv.launcher` is protected), so
`launcher/build-and-install.sh` is the wrong tool here. What works (proven on the Xiaomi FX 55", AFTNE4E43F):

1. Install the launcher APK (`com.gathin.tvhome`) like any app. Do not disable the Amazon launcher.
2. Install `my-tv/home-return` (`local.homereturn`). It is an accessibility service that opens the clean
   launcher when Amazon's home appears. Two Home presses within 3 s stay on Amazon's home.
3. Enable it, appending to whatever is already enabled:

```bash
s=$(adb shell settings get secure enabled_accessibility_services)   # "null" when empty
adb shell settings put secure enabled_accessibility_services "local.homereturn/.HomeReturnService"   # or "$s:local.homereturn/.HomeReturnService"
adb shell settings put secure accessibility_enabled 1
```

## Result on this Stick (2026-11-26)

- Final state: the debloat above only, Amazon's launcher as Home. About 2.6 GB storage and 224 MB RAM free afterwards.
- When the clean launcher was installed, the dock needed Fire OS package names: Prime Video is `com.amazon.firebat` and
  YouTube is `com.amazon.firetv.youtube` (the `config.xml` defaults `com.amazon.amazonvideo.livingroom` and
  `com.google.android.youtube.tv` are not on a Stick). Netflix `com.netflix.ninja`, JioHotstar `in.startv.hotstar`.
- Measure a redirect from the log, not by eye: the service's launch request is logged within 0.5 s, so "START"
  lines make it look instant. Compare it with `am_set_resumed_activity` for `com.gathin.tvhome/.MainActivity`.
- Use `screencap -p /sdcard/s.png` then `adb pull`, not `exec-out screencap`, for screenshots.

## Gotchas (each one cost time before)

- **Check the APK before installing it.** A `local.homereturn` build once had no service class:
  `ClassNotFoundException: Didn't find class local.homereturn.HomeReturnService` in logcat, the service
  listed as enabled but doing nothing. Verify with
  `$ANDROID_HOME/build-tools/34.0.0/dexdump -l plain app-release.apk | grep HomeReturnService`
  (versionCode 2 / 1.1 built 2026-11-26 has it).
- **Turn the service off before reinstalling the APK.** Installing over an enabled accessibility service can
  leave Android's binding stuck. Clear `enabled_accessibility_services`, set `accessibility_enabled 0`,
  install, wait a second, write the list back, set `accessibility_enabled 1`.
- **A force-stop drops accessibility services** from the enabled list (cleaner apps, `am force-stop`, theme
  pushes). Re-enable them with the commands above, then check `dumpsys accessibility | grep -i 'Enabled services'`.
- **`adb shell input keyevent` does not reach the key services.** Test remote buttons with the real remote.
  Find a button's scancode with `adb shell getevent -lt` (the Amazon remote's colours are 398 red, 399 green,
  400 yellow, 401 blue) and map it in a theme with `app_keys: ["scan:398:<package>"]`.
- **Shell cannot write `Android/data` on Fire OS 8.** Push theme files through the launcher's provider:
  `content write --uri content://com.gathin.tvhome.theme/<file> < /data/local/tmp/<file>`. Android 8/9 have no
  `content write`; `tools/theme.sh` falls back to `cp`.
- **Protected packages** refuse `pm disable-user` ("Cannot disable a protected package"); the example list
  has them commented out. Leave `com.amazon.tv.oobe` alone, a factory reset needs it.
- Re-enabled Vewd broadcaster apps (`com.vewd.core.tvapp`) show up as launcher tiles and cause a duplicate
  "Live TV" prompt on input switching.
- The wireless link: `adb connect <ip>:5555` shows `unauthorized` until the "Allow USB debugging?" prompt is
  accepted on the TV. Switch ADB debugging off when done.

## Not done

Privacy toggles (Settings > Preferences > Privacy Settings, Featured Content autoplay) cannot be set from
the shell; do them with the remote.
