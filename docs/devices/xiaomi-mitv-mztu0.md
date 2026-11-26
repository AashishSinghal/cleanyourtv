# Xiaomi MiTV-MZTU0, Google TV

Android 14, armeabi-v7a, Google TV with `com.google.android.apps.tv.launcherx`. Done 2026-11-25.

- List: a conservative one built from the TV's own package list (telemetry, PatchWall, `com.mitv.tvhome.*`,
  `com.xm.webcontent`, `com.xiaomi.floatingframe`, partner auto-installs). Voice, Chromecast, updates, tuner and
  HDMI inputs kept. Applied by hand with `pm disable-user`, so it is not a tested `examples/` list yet.
- Launcher: `launcher/build-and-install.sh` equivalent, run by hand. Disabling the stock launcher
  (`com.google.android.apps.tv.launcherx`) made `com.google.android.tungsten.setupwraith` (RecoveryActivity)
  grab Home; disabling `setupwraith` too fixed it. Both go in the restore script. The scripts' check that
  setupwraith does not own the saved Wi-Fi network (`dumpsys wifi | grep -o 'cname=[^ ]*'`) was not run, because
  the steps were done by hand; the TV stayed on Wi-Fi and ADB afterwards.
- Remote's R button (scancode 166, `KEY_STOPCD`) opens SmartTube with `app_keys` = `scan:166:org.smarttube.stable`
  and `HomeKeyService` enabled. Per-TV setting, not committed to `config.xml`.
- The Mac and TV were on different subnets: TCP to the TV was blocked, so the repo's scripts could not connect.
  Plain `adb` over paired Wireless debugging still worked. `tools/` has no hook for a Wireless-debugging port.
- Wireless debugging ports change on every toggle; use the serial `adb devices` shows for the mDNS entry.
