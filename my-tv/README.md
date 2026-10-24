# my-tv

What runs on our TCL Fire TV (AFTDEC012E, Fire OS 8.1.8.5) besides the launcher in `../launcher`.

- `theme/config.json`: the theme pushed to the launcher (dock, hidden apps, cinematic shelf, weather for
  an example place (London) in °C, the my-tv switches). The wallpapers are `../themes/scenes/wallpapers/`, in order every
  10 minutes. `theme-quiet/` (what the TV runs) also groups them with `wallpaper_moods`: morning, evening and
  night pictures by the time of day, Monsoon while it rains and the misty ones in fog. On this TV the shell cannot write `Android/data`, so files go through the launcher's
  ThemeProvider: push to `/data/local/tmp`, then
  `content write --uri content://com.gathin.tvhome.theme/<file> < /data/local/tmp/<file>`.
- `home-return/`: a small accessibility service (no permissions, no screen reading) that opens the launcher
  when Amazon's home appears (Home twice within 3 s stays on Amazon's, including after its sidebar or settings
  menu) and opens YouTube when a remote app
  button leads to an app that is not available (the Amazon Music button). Enable with
  `settings put secure enabled_accessibility_services local.homereturn/.HomeReturnService` and
  `settings put secure accessibility_enabled 1`.

Both are built with the toolchain in `~/firetv-build` (`source ~/firetv-build/env.sh`, then
`./gradlew --no-daemon assembleRelease`) and signed with its local debug key.
