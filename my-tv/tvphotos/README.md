# tvphotos

Keeps the pictures of the [photo screensaver](../../addons/photo-screensaver/README.md) on the TV in step with a
folder on a Mac. Drop photos into the folder, run one command, and the TV has exactly those photos.

## Why not the add-on's `sync.sh`

On Fire OS 8 the adb shell cannot read `Android/data` (`Permission denied`). `sync.sh` lists the TV's folder
from the shell to decide what to send and what to remove, so there it sees an empty folder: it reports
"0 file(s) on the TV", sends every file again on every run and never removes a photo you deleted. `tvphotos`
asks the app's own provider instead: it reads each file back (`content read ... | md5sum`) and compares the
checksum, and keeps a small ledger of the names it put on the TV so deleted photos can be removed.

## Set up

1. Install the add-on: `TV_HOST=<tv ip> addons/photo-screensaver/build-and-install.sh` (JDK 17, Android SDK 34).
2. `python3 -m venv tools/venv && tools/venv/bin/pip install -r tools/requirements.txt`
3. Run `my-tv/tvphotos/tvphotos --dry-run` once. It creates `~/.cleanyourtv/tvphotos/config.json`; put the TV's
   IP address in `TV_HOST` (and the folder in `SRC`), then run it without `--dry-run`.

Needs macOS (it shrinks pictures with `sips`), network ADB debugging on the TV, and the add-on installed.

## Use

| Command | What it does |
|---|---|
| `tvphotos` | Sync now. Prints what it sends and removes. |
| `tvphotos --dry-run` | Shows the plan without touching the TV. |
| `tvphotos --status` | Folder, TV, last sync and whether anything is pending. |
| `tvphotos --show` | Syncs, then starts the screensaver to look at it. |
| `tvphotos --rebuild` | Clears the TV's pictures and sends everything again. |
| `tvphotos --auto` | Quiet: does nothing unless the folder changed, and waits if the TV is off. |
| `tvphotos --install-agent` / `--remove-agent` | Runs `--auto` from a macOS LaunchAgent when the folder changes and every 10 minutes. Opt-in; it needs network ADB to stay on. |

## What it does to the pictures

- Each picture gets a stable name from its file name (`Dad Birthday.HEIC` becomes `dad-birthday.jpg`), is
  shrunk to `MAX_EDGE` px on the long side and saved as JPEG at `QUALITY`. The screensaver draws at about 1080p,
  so more pixels only cost storage and upload time. JPG and PNG are tested; HEIC and WebP go through `sips` the same
  way but have not been tried. The add-on's 10 MB limit per picture is checked after shrinking.
- The same picture under two names is sent once. A changed picture overwrites its file, so nothing is added twice.
- `MAX_FILES` (100, the add-on's limit) and `MAX_TOTAL_MB` (200) cap the set; pictures past the cap are skipped
  with a message. Temporary files on the TV are removed before and after each run.
- The first sync also makes Photos the TV's screensaver, sets the idle time (`IDLE_MINUTES`, via
  `screen_off_timeout`) and the seconds per picture (`SECONDS_PER_PICTURE`, 5 to 3600). The screensaver the TV had
  before is kept for `addons/photo-screensaver/sync.sh --off`.

Settings (`~/.cleanyourtv/tvphotos/config.json`): `SRC`, `TV_HOST`, `REPO`, `MAX_EDGE` (2560), `QUALITY` (82),
`MAX_FILES`, `MAX_TOTAL_MB`, `SECONDS_PER_PICTURE` (20), `IDLE_MINUTES` (1). State (ledger, shrunk copies, log) is
in the same folder, outside the repo.

## When the screensaver starts

The system starts it after the idle time, unless something keeps the screen awake. On the Xiaomi FX 55" Spotify in
the foreground does (its window holds a screen-on lock), so the screensaver stays off while it plays. Audio that
keeps playing after you go back to the launcher has not been tested.

## Tested on

Xiaomi FX 55" Fire TV (AFTNE4E43F), Fire OS 8.1.8.5 / Android 11, macOS. Sync, duplicate and removal handling were
checked on the TV; `--install-agent` has not been run.
