#!/usr/bin/env python3
"""Keep the TV's photo screensaver in step with a folder on this Mac.

  tvphotos            sync now: resize, rename, send what changed, remove what is gone
  tvphotos --auto     quiet; does nothing unless the folder changed, and just waits if the TV is off
  tvphotos --status   what is where
  tvphotos --dry-run  show what a sync would do, without touching the TV
  tvphotos --rebuild  clear the TV's pictures and send everything again
  tvphotos --show     start the screensaver when done, to look at it
  tvphotos --install-agent / --remove-agent   run --auto by itself (folder change, and every 10 minutes)

How it avoids duplicates and clutter:
  * each picture gets a stable name made from its file name (Dad Birthday.HEIC -> dad-birthday.jpg), so the
    same picture is always the same file on the TV and a changed picture is overwritten, never added twice;
  * identical pictures under two names are sent once;
  * before sending, the TV is asked for the checksum of the file it already has, and only differing or
    missing files are sent;
  * names this tool put on the TV (the ledger) that are no longer in the folder are deleted from the TV;
  * pictures are shrunk to MAX_EDGE px and saved as JPEG (the screensaver draws at about 1080p, so more
    pixels cost storage and upload time and show nothing), and MAX_FILES / MAX_TOTAL_MB cap the folder;
  * temporary files on the TV are removed before and after every run.

Settings live in ~/.cleanyourtv/tvphotos/config.json. The TV side is the repo's photo-screensaver add-on.
"""
import argparse
import hashlib
import json
import os
import plistlib
import re
import socket
import subprocess
import sys
import time
from datetime import datetime
from pathlib import Path

HOME = Path.home()
BASE = HOME / ".cleanyourtv" / "tvphotos"
CACHE = BASE / "cache"
CONFIG = BASE / "config.json"
LEDGER = BASE / "ledger.json"
STATE = BASE / "state.json"
LOG = BASE / "tvphotos.log"
LOCK = BASE / "lock"

PKG = "com.gathin.photodream"
PICS = f"content://{PKG}.files/pictures"
SECONDS_URI = f"content://{PKG}.files/seconds.txt"
DIR = f"/sdcard/Android/data/{PKG}/files/pictures"
DREAM = f"{PKG}/.PhotoDream"
EXTS = {".jpg", ".jpeg", ".png", ".heic", ".heif", ".webp"}
EMPTY_MD5 = "d41d8cd98f00b204e9800998ecf8427e"
LABEL = "com.cleanyourtv.tvphotos"
PLIST = HOME / "Library" / "LaunchAgents" / f"{LABEL}.plist"
IMAGE_MAX = 10 * 1024 * 1024  # the add-on skips bigger pictures

# The repo this file sits in (my-tv/tvphotos/ -> two levels up), or empty when it was copied elsewhere.
_HERE = Path(__file__).resolve().parent
_REPO = next((p for p in (_HERE, *_HERE.parents) if (p / "tools" / "tv.py").exists()), None)

DEFAULTS = {
    "SRC": str(HOME / "Downloads" / "ScreenSaver"),
    "TV_HOST": "",  # the TV's IP address: set it in config.json
    "REPO": str(_REPO) if _REPO else "",  # where tools/tv.py and tools/venv live
    "MAX_EDGE": 2560,
    "QUALITY": 82,
    "MAX_FILES": 100,
    "MAX_TOTAL_MB": 200,
    "SECONDS_PER_PICTURE": 20,
    "IDLE_MINUTES": 1,
}

AUTO = False


def say(msg=""):
    print(msg, flush=True)
    if AUTO:
        with open(LOG, "a") as f:
            f.write(f"{datetime.now():%Y-%m-%d %H:%M:%S} {msg}\n")


def load_json(path, default):
    try:
        return json.loads(path.read_text())
    except (OSError, ValueError):
        return default


def save_json(path, data):
    tmp = path.with_name(path.name + ".tmp")
    tmp.write_text(json.dumps(data, indent=1, sort_keys=True))
    tmp.replace(path)


def md5_file(path):
    h = hashlib.md5()
    with open(path, "rb") as f:
        for block in iter(lambda: f.read(1 << 20), b""):
            h.update(block)
    return h.hexdigest()


# ---------------------------------------------------------------- the folder on this Mac

def scan(src):
    return sorted((p for p in src.iterdir()
                   if p.is_file() and not p.name.startswith(".") and p.suffix.lower() in EXTS),
                  key=lambda p: p.name.lower())


def signature(files, cfg):
    h = hashlib.md5()
    for p in files:
        st = p.stat()
        h.update(f"{p.name}|{st.st_size}|{st.st_mtime_ns}\n".encode())
    for k in ("MAX_EDGE", "QUALITY", "MAX_FILES", "MAX_TOTAL_MB", "SECONDS_PER_PICTURE", "IDLE_MINUTES", "TV_HOST"):
        h.update(f"{k}={cfg[k]}\n".encode())
    return h.hexdigest()


def slug(stem):
    s = re.sub(r"[^a-z0-9]+", "-", stem.lower()).strip("-")[:60].strip("-")
    return s or "photo"


def dims(path):
    out = subprocess.run(["sips", "-g", "pixelWidth", "-g", "pixelHeight", str(path)],
                         capture_output=True, text=True).stdout
    w = re.search(r"pixelWidth:\s*(\d+)", out)
    h = re.search(r"pixelHeight:\s*(\d+)", out)
    return (int(w.group(1)), int(h.group(1))) if w and h else (0, 0)


def convert(src, dest, edge, quality):
    """JPEG copy of src at no more than edge px on the long side. False if sips could not do it."""
    w, h = dims(src)
    if not w:
        return False
    part = dest.with_name(dest.name + ".part.jpg")
    cmd = ["sips", "-s", "format", "jpeg", "-s", "formatOptions", str(quality)]
    if max(w, h) > edge:
        cmd += ["-Z", str(edge)]
    r = subprocess.run(cmd + [str(src), "--out", str(part)], capture_output=True, text=True)
    if r.returncode != 0 or not part.exists() or part.stat().st_size == 0:
        part.unlink(missing_ok=True)
        return False
    part.replace(dest)
    return True


def build_desired(files, cfg):
    """[(name, cached path, md5, size)] in the order the TV will show them, plus warnings."""
    CACHE.mkdir(parents=True, exist_ok=True)
    warnings, desired, used, seen_md5 = [], [], set(), {}
    total, cap = 0, cfg["MAX_TOTAL_MB"] * 1024 * 1024
    for src in files:
        name = slug(src.stem) + ".jpg"
        if name in used:
            name = f"{slug(src.stem)}-{hashlib.md5(src.name.encode()).hexdigest()[:6]}.jpg"
        meta = CACHE / (name + ".meta")
        st = src.stat()
        key = f"{src.name}|{st.st_size}|{st.st_mtime_ns}|{cfg['MAX_EDGE']}|{cfg['QUALITY']}"
        dest = CACHE / name
        if not (dest.exists() and meta.exists() and meta.read_text() == key):
            if not convert(src, dest, cfg["MAX_EDGE"], cfg["QUALITY"]):
                warnings.append(f"skipped {src.name}: could not convert it")
                continue
            if dest.stat().st_size > IMAGE_MAX * 0.95:  # the add-on ignores pictures over 10 MB
                convert(src, dest, cfg["MAX_EDGE"], 60)
            if dest.stat().st_size > IMAGE_MAX:
                dest.unlink()
                warnings.append(f"skipped {src.name}: still over 10 MB after shrinking")
                continue
            meta.write_text(key)
        digest, size = md5_file(dest), dest.stat().st_size
        if digest in seen_md5:
            warnings.append(f"skipped {src.name}: the same picture as {seen_md5[digest]}")
            continue
        if len(desired) >= cfg["MAX_FILES"] or total + size > cap:
            warnings.append(f"skipped {src.name}: over the limit of {cfg['MAX_FILES']} files / {cfg['MAX_TOTAL_MB']} MB")
            continue
        used.add(name)
        seen_md5[digest] = src.name
        total += size
        desired.append((name, dest, digest, size))
    keep = {d[0] for d in desired}
    for f in CACHE.iterdir():  # shrunk copies of pictures that left the folder, or failed halfway
        if f.name.endswith(".meta"):
            base = f.name[:-5]
        elif f.name.endswith(".part.jpg"):
            f.unlink()
            continue
        else:
            base = f.name
        if base not in keep:
            f.unlink(missing_ok=True)
    return desired, warnings


# ---------------------------------------------------------------- the TV

class TV:
    def __init__(self, cfg):
        self.host = cfg["TV_HOST"]
        if not self.host or not cfg["REPO"]:
            raise RuntimeError(f"set TV_HOST (the TV's IP address) and REPO (this repo's folder) in {CONFIG}")
        self.tools = Path(cfg["REPO"]).expanduser() / "tools"
        self.py = str(self.tools / "venv" / "bin" / "python")
        self.env = dict(os.environ, TV_HOST=self.host)

    def reachable(self):
        try:
            with socket.create_connection((self.host, 5555), timeout=3):
                return True
        except OSError:
            return False

    def run(self, script, *args, timeout=120):
        return subprocess.run([self.py, "-I", script, *args], cwd=self.tools, env=self.env,
                              capture_output=True, text=True, timeout=timeout)

    def sh(self, cmd, timeout=120):
        r = self.run("tv.py", cmd, timeout=timeout)
        if r.returncode != 0 and not r.stdout.strip():
            raise RuntimeError((r.stderr.strip().splitlines() or ["tv.py failed"])[-1])
        return r.stdout.replace("\r", "")

    def push(self, local, remote):
        r = self.run("b64push.py", str(local), remote, timeout=900)
        if r.returncode != 0 or not re.search(r"(?<!MIS)MATCH", r.stdout):
            raise RuntimeError("upload failed or the checksum did not match")

    def md5s(self, names):
        """{name: md5 of the file the TV has, or None}, one connection for the lot."""
        result = {}
        for i in range(0, len(names), 20):
            chunk = names[i:i + 20]
            script = "; ".join(f'echo "{n} $(content read --uri {PICS}/{n} 2>/dev/null | md5sum)"' for n in chunk)
            for line in self.sh(script).splitlines():
                m = re.match(r"^(\S+) ([0-9a-f]{32})\b", line.strip())
                if m:
                    result[m.group(1)] = None if m.group(2) == EMPTY_MD5 else m.group(2)
        return {n: result.get(n) for n in names}

    def send(self, name, local):
        tmp = f"/data/local/tmp/tvp-{name}"
        self.push(local, tmp)
        # Written through the add-on's own provider so the app owns the file; a plain copy is the fallback.
        self.sh(f"rm -f {DIR}/{name} 2>/dev/null; "
                f"content write --uri {PICS}/{name} < {tmp} 2>/dev/null || {{ mkdir -p {DIR}; cp {tmp} {DIR}/{name}; }}; "
                f"rm -f {tmp}")

    def delete(self, name):
        out = self.sh(f"content delete --uri {PICS}/{name} >/dev/null 2>&1; rm -f {DIR}/{name} 2>/dev/null; "
                      f"content read --uri {PICS}/{name} 2>/dev/null | wc -c")
        return out.strip().split()[-1] == "0"


def apply_settings(tv, cfg, state, force_seconds):
    out = tv.sh("settings get secure screensaver_components; settings get secure screensaver_enabled; "
                "settings get system screen_off_timeout").split("\n")
    comp, enabled, timeout = (out + ["", "", ""])[:3]
    cmds, changed = [], []
    if comp.strip() != DREAM:
        # The add-on's sync.sh keeps the screensaver the TV had before, for `sync.sh --off`; do the same once.
        cmds.append("[ -f /data/local/tmp/cyt-screensaver-before ] || "
                    "{ settings get secure screensaver_components; settings get secure screensaver_enabled; } "
                    "> /data/local/tmp/cyt-screensaver-before")
        cmds.append(f"settings put secure screensaver_components {DREAM}")
        changed.append("screensaver")
    if enabled.strip() != "1":
        cmds.append("settings put secure screensaver_enabled 1")
    want = cfg["IDLE_MINUTES"] * 60000
    if cfg["IDLE_MINUTES"] > 0 and timeout.strip() != str(want):
        cmds.append(f"settings put system screen_off_timeout {want}")
        changed.append(f"starts after {cfg['IDLE_MINUTES']} min idle")
    if force_seconds or state.get("seconds") != cfg["SECONDS_PER_PICTURE"]:
        tmp = "/data/local/tmp/tvp-seconds.txt"
        cmds.append(f"printf '%s' {cfg['SECONDS_PER_PICTURE']} > {tmp}; "
                    f"content write --uri {SECONDS_URI} < {tmp}; rm -f {tmp}")
        changed.append(f"{cfg['SECONDS_PER_PICTURE']} s per picture")
    if cmds:
        tv.sh("; ".join(cmds))
    return changed


# ---------------------------------------------------------------- commands

def sync(cfg, args):
    src = Path(cfg["SRC"]).expanduser()
    if not src.is_dir():
        say(f"The folder {src} does not exist.")
        return 1
    files = scan(src)
    sig = signature(files, cfg)
    state = load_json(STATE, {})
    if args.auto and not args.rebuild and state.get("sig") == sig and state.get("ok"):
        return 0  # nothing changed since the last good sync: do not even talk to the TV
    newest = max((p.stat().st_mtime for p in files), default=0)
    if time.time() - newest < 5:  # still being copied or downloaded
        time.sleep(6)
        files = scan(src)
        sig = signature(files, cfg)

    desired, warnings = build_desired(files, cfg)
    names = [d[0] for d in desired]
    ledger = load_json(LEDGER, [])
    gone = [n for n in ledger if n not in names]
    total_mb = sum(d[3] for d in desired) / 1048576

    if args.dry_run:
        say(f"{len(files)} picture(s) in {src}; {len(desired)} would be on the TV ({total_mb:.1f} MB):")
        for n, _, _, size in desired:
            say(f"  {n}  {size // 1024} KB")
        for n in gone:
            say(f"  would delete {n}")
        for w in warnings:
            say("  " + w)
        return 0

    tv = TV(cfg)
    if not tv.reachable():
        if args.auto:
            if state.get("waiting") != sig:
                say("Photos changed but the TV is not reachable; will retry.")
                save_json(STATE, dict(state, waiting=sig))
            return 0
        say(f"Cannot reach the TV at {cfg['TV_HOST']}:5555. Is it on, and is ADB debugging enabled in Developer options?")
        return 1
    if "package:" not in tv.sh(f"pm path {PKG}; true"):
        say("The photo screensaver app is not on the TV (build-and-install.sh in addons/photo-screensaver).")
        return 1

    tv.sh("rm -f /data/local/tmp/tvp-* 2>/dev/null; true")
    ok = True
    removed = sent = 0
    if args.rebuild:
        tv.sh(f"content delete --uri {PICS} >/dev/null 2>&1; true")
        ledger = []
        gone = []
        say("Cleared the TV's pictures.")
    for name in gone:
        if tv.delete(name):
            ledger.remove(name)
            removed += 1
            save_json(LEDGER, ledger)
            say(f"removed {name}")
        else:
            ok = False
            say(f"could not remove {name}")
    have = tv.md5s(names)
    same = 0
    for name, path, digest, size in desired:
        if have.get(name) == digest:
            same += 1
            if name not in ledger:
                ledger.append(name)
                save_json(LEDGER, ledger)
            continue
        say(f"sending {name} ({size // 1024} KB)")
        try:
            tv.send(name, path)
            if tv.md5s([name])[name] != digest:
                raise RuntimeError("the TV's copy does not match")
        except (RuntimeError, subprocess.TimeoutExpired) as e:
            ok = False
            say(f"failed {name}: {e}")
            continue
        sent += 1
        if name not in ledger:
            ledger.append(name)
            save_json(LEDGER, ledger)
    try:
        changed = apply_settings(tv, cfg, state, args.rebuild)
        state["seconds"] = cfg["SECONDS_PER_PICTURE"]
    except (RuntimeError, subprocess.TimeoutExpired) as e:
        ok = False
        changed = []
        say(f"could not set the screensaver options: {e}")
    tv.sh("rm -f /data/local/tmp/tvp-* 2>/dev/null; true")
    for w in warnings:
        say(w)
    state.update(sig=sig, ok=ok, time=datetime.now().isoformat(timespec="seconds"))
    state.pop("waiting", None)
    save_json(STATE, state)
    if not desired:
        say("No pictures in the folder: the screensaver will show a black screen.")
    say(f"TV has {len(desired)} picture(s), {total_mb:.1f} MB. Sent {sent}, removed {removed}, already there {same}."
        + (f" Set: {', '.join(changed)}." if changed else ""))
    if args.show and ok:
        say(tv.sh("am start -n com.android.systemui/.Somnambulator >/dev/null; sleep 3; "
                  "dumpsys dreams | grep mCurrentDreamName").strip())
    return 0 if ok else 1


def status(cfg):
    src = Path(cfg["SRC"]).expanduser()
    files = scan(src) if src.is_dir() else []
    state = load_json(STATE, {})
    ledger = load_json(LEDGER, [])
    cache_mb = sum(f.stat().st_size for f in CACHE.glob("*.jpg")) / 1048576 if CACHE.is_dir() else 0
    say(f"Folder:   {src} ({len(files)} picture(s))")
    say(f"On TV:    {len(ledger)} picture(s) sent by this tool; {cache_mb:.1f} MB of shrunk copies kept here")
    say(f"Last sync: {state.get('time', 'never')} ({'ok' if state.get('ok') else 'not clean'})")
    say(f"Pending:  {'no' if state.get('sig') == signature(files, cfg) and state.get('ok') else 'yes'}")
    tv = TV(cfg)
    if tv.reachable():
        out = tv.sh("settings get secure screensaver_components; settings get system screen_off_timeout").split("\n")
        say(f"TV:       reachable; screensaver {out[0].strip()}, starts after {int(out[1] or 0) // 60000} min idle")
    else:
        say("TV:       not reachable")
    say(f"Agent:    {'installed' if PLIST.exists() else 'not installed'}")


def agent(install, cfg):
    uid = os.getuid()
    subprocess.run(["launchctl", "bootout", f"gui/{uid}/{LABEL}"], capture_output=True)
    if not install:
        PLIST.unlink(missing_ok=True)
        say("Automatic sync removed.")
        return 0
    plist = {
        "Label": LABEL,
        "ProgramArguments": [str(Path(cfg["REPO"]).expanduser() / "tools" / "venv" / "bin" / "python"), "-I",
                             str(Path(__file__).resolve()), "--auto"],
        "RunAtLoad": True,
        "StartInterval": 600,
        "WatchPaths": [str(Path(cfg["SRC"]).expanduser())],
        "ThrottleInterval": 30,
        "StandardOutPath": str(BASE / "launchd.out"),
        "StandardErrorPath": str(BASE / "launchd.err"),
        "EnvironmentVariables": {"PATH": "/usr/bin:/bin:/usr/sbin:/sbin:/opt/homebrew/bin"},
    }
    PLIST.parent.mkdir(parents=True, exist_ok=True)
    PLIST.write_bytes(plistlib.dumps(plist))
    r = subprocess.run(["launchctl", "bootstrap", f"gui/{uid}", str(PLIST)], capture_output=True, text=True)
    if r.returncode != 0:
        say(f"launchctl said: {r.stderr.strip()}")
        return 1
    say("Automatic sync on: runs when the folder changes and every 10 minutes (it only contacts the TV when "
        "something changed). Log: " + str(LOG))
    return 0


def main():
    global AUTO
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    for flag in ("auto", "status", "dry-run", "rebuild", "show", "install-agent", "remove-agent"):
        ap.add_argument("--" + flag, action="store_true")
    args = ap.parse_args()
    AUTO = args.auto
    BASE.mkdir(parents=True, exist_ok=True)
    if not CONFIG.exists():
        save_json(CONFIG, DEFAULTS)
    cfg = dict(DEFAULTS, **load_json(CONFIG, {}))
    if LOG.exists() and LOG.stat().st_size > 200_000:
        LOG.write_text("".join(LOG.read_text().splitlines(True)[-300:]))
    if args.status:
        try:
            return status(cfg)
        except RuntimeError as e:
            say(str(e))
            return 1
    if args.install_agent or args.remove_agent:
        return agent(args.install_agent, cfg)
    try:
        os.mkdir(LOCK)
    except FileExistsError:
        if time.time() - LOCK.stat().st_mtime < 1800:
            if not args.auto:
                say("Another sync is running.")
            return 0
        os.utime(LOCK)
    try:
        return sync(cfg, args)
    except (RuntimeError, subprocess.TimeoutExpired, OSError) as e:
        say(f"Sync stopped: {e}")
        return 1
    finally:
        try:
            os.rmdir(LOCK)
        except OSError:
            pass


if __name__ == "__main__":
    sys.exit(main())
