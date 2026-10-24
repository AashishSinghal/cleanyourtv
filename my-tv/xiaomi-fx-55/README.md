# Xiaomi FX 55" Fire TV (AFTNE4E43F, Fire OS 8.1.8.5)

Xiaomi FX series 55" 4K Fire TV (L55MB-FIN), Amazon model `AFTNE4E43F`, Fire OS 8.1.8.5 (RS8185/3885) on Android 11
(SDK 30), 2 GB RAM, sold in India. It has the ginza overlays (`ginzaeunm`) of the TCL AFTDEC012E. Done over Wi-Fi ADB.

| File | What it is |
|---|---|
| `debloat.txt` | The 49 packages that were disabled, grouped and commented |

## What worked

- **Debloat:** 49 packages disabled with `pm disable-user`, none refused as protected (all show as disabled), and the
  first 33 survived a reboot. Alexa, casting, Live TV, HDMI and OTA updates were left enabled; Alexa, casting and
  HDMI sources were not specifically retested afterwards.
- **Clean launcher:** Amazon's home stays the system home, so the launcher comes up over it through
  `my-tv/home-return/`. On this firmware Amazon's home reports its window twice per Home press; Home Return ignores the
  repeat and stays on Amazon's home after you open its sidebar or settings from there.
- **Photo screensaver:** the `addons/photo-screensaver` add-on installs and runs as the system screensaver; Fire OS
  accepts it through `screensaver_components`. `my-tv/tvphotos/` keeps its pictures in sync from a Mac.
- **YouTube without ads:** SmartTube (stable build) is installed next to Amazon's YouTube.

## What to know

- **Vewd apps:** the Vewd broadcaster portals (Canal Digitaal, Freeview, HD+, Skylink, ...) appear as apps in the clean
  launcher. They come from `com.vewd.core.opapp`, `fvpplayer` and `tvapp`, all in `debloat.txt`. Re-enabling `tvapp` made
  input switching ask "Live TV or Freeview Live TV".
- **Inputs:** with nothing plugged in, Amazon's input picker showed "No signal" for each HDMI port, while the clean
  launcher's Inputs button opened Live TV settings (Fire OS reserves input tuning to Amazon-signed apps). Not tried
  with a real source yet.
- **Screensaver and music:** with the idle time at 1 minute, the screensaver stays off while Spotify plays in the
  foreground (its window holds a screen-on lock).
- **Settings to turn off by hand:** Settings > Preferences > Privacy Settings (device and app usage data, interest-based
  ads) and Featured Content autoplay.
- **Updating Home Return:** turn the accessibility service off before `adb install`, or Android can leave it stuck
  binding. Check that the APK really holds the service (`ClassNotFoundException` in logcat means an empty build).
