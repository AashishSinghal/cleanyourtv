# CleanYourTV on a Fire TV smart TV

This branch (`my-tv`) is a fork of [Gathin23/cleanyourtv](https://github.com/Gathin23/cleanyourtv) for one TV: a TCL
Fire TV smart TV, model AFTDEC012E (`ginza`), Fire OS 8.1.8.5 / Android 11. It keeps upstream's launcher and adds what
that TV needed, plus a few looks. Everything new is off until a theme turns it on.

![Home on the TV](my-tv/screenshot.jpg)

## What is different

| Part | What it does |
|---|---|
| **Home Return** (`my-tv/home-return/`) | Fire OS keeps Home for Amazon's launcher, so this small app opens the launcher whenever Amazon's home screen appears. Press Home twice within 3 s to stay on Amazon's home; opening and closing its sidebar or settings menu then leaves you there until the next Home press. It also turns the Amazon Music remote button into YouTube when the Music app is disabled. It is a separate app, so it works with any build of the launcher, upstream's included. |
| **Weather on the shelf** (`weather`, `weather_place`, `weather_latitude`, `weather_longitude`, `weather_fahrenheit`) | Current conditions and the next hours from [Open-Meteo](https://open-meteo.com), ported from [manas-04/cleanyourtv](https://github.com/manas-04/cleanyourtv/tree/feat/wallpapers-weather) (ca9ad37). |
| **Wallpaper moods with weather** (`wallpaper_moods`) | Upstream's `wallpaper_moods` rotates the `morning`, `evening` or `night` group by the sun. Here, with weather on, `rain` and `fog` groups win while the forecast's current hour has them, and the forecast's sunrise and sunset come first. |
| **Quiet buttons and a calm layout** (`quiet_buttons`, `home_layout` `"calm"`) | Small round top-bar buttons that slide their label out only while focused, with a green dot while online; `calm` also moves the time and weather into a fixed header. |
| **Settings button** | `settings_key_panel` also opens the quick settings panel from the Settings button. |
| **Emulator kit** (`my-tv/sim/sim.sh`) | Drives the Android TV emulator like the remote, to try changes before the TV. |

The display switches and the time-of-day half of wallpaper moods started here and are now upstream, without network:
`wifi_status_color`, `cinematic_dots`, `cinematic_scrim`, `cinematic_art`, `cinematic_white_focus` and `wallpaper_moods`
([#1038](https://github.com/Gathin23/cleanyourtv/pull/1038) to [#1042](https://github.com/Gathin23/cleanyourtv/pull/1042)).
The themes in `my-tv/` use them.

## Network, permissions and privacy

- **The launcher** gains the `INTERNET` permission for the weather only. While `weather` is on it sends the configured
  coordinates and the unit to Open-Meteo about once an hour; with `weather` off it makes no requests. No analytics, no
  accounts, nothing else leaves the TV.
- **Home Return** has no permission besides binding as an accessibility service. It listens only for window changes
  from `com.amazon.tv.launcher` and `com.amazon.venezia` (the Appstore), never reads screen content
  (`canRetrieveWindowContent="false"`) and never filters keys.

## Build and install

With JDK 17, Android SDK platform 34 and build-tools 34.0.0, and the TV's ADB address in `TV`:

```sh
cd launcher && ./gradlew --no-daemon assembleRelease
adb -s $TV install -r app/build/outputs/apk/release/app-release.apk
cd ../my-tv/home-return && ./gradlew --no-daemon assembleRelease
adb -s $TV install -r app/build/outputs/apk/release/app-release.apk
adb -s $TV shell settings put secure enabled_accessibility_services local.homereturn/.HomeReturnService
adb -s $TV shell settings put secure accessibility_enabled 1
```

Themes are in `my-tv/theme*/`; `theme-quiet/` is the one the TV runs (the weather place here is an example, London:
set your own coordinates). On this TV the shell cannot write `Android/data`, so a theme goes in through the launcher's
ThemeProvider, as upstream's `tools/theme.sh` does:

```sh
adb -s $TV push my-tv/theme-quiet/config.json /data/local/tmp/config.json
adb -s $TV shell 'content write --uri content://com.gathin.tvhome.theme/config.json < /data/local/tmp/config.json'
```

To undo: `settings delete secure enabled_accessibility_services`, `settings put secure accessibility_enabled 0`, then
uninstall `local.homereturn` and `com.gathin.tvhome`. Amazon's launcher is never disabled, so its home simply stays.

## Credits and licence

Upstream launcher and tools by [Gathin23](https://github.com/Gathin23/cleanyourtv); the weather by
[manas-04](https://github.com/manas-04/cleanyourtv). MIT, as upstream ([LICENSE](LICENSE)).
