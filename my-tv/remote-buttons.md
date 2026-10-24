# Remote buttons

Launcher settings > **Remote buttons** assigns a remote button to an app from the TV itself: press the button, pick the
app. It is for buttons that do nothing on Fire OS (the coloured ones, for example) or that you want to point somewhere
else.

## Use

1. Hold OK on the Settings button, choose **Launcher settings**, then **Remote buttons**.
2. **Add a button: press it on the remote**, then press the button. Pick the app it should open.
3. The list shows every assigned button. Open a row to change its app or **Remove** it. Rows marked *(theme)* come from the
   theme's `app_keys`; picking an app for one makes an on-TV choice that wins over the theme, and Remove brings the theme's app back.

It needs the remote key helper (an accessibility service) switched on, because only that sees a remote key before the
system does:

```
adb shell settings put secure enabled_accessibility_services "com.gathin.tvhome/.InputKeyService"
adb shell settings put secure accessibility_enabled 1
```

(With Home Return in use the list holds both services, separated by a colon: `local.homereturn/.HomeReturnService:com.gathin.tvhome/.InputKeyService`.)
A force stop of the launcher switches the helper off; run the two lines again. After updating the launcher app, switch
accessibility off and on once.

## Buttons that cannot be assigned

While the screen is listening, these keep doing their normal job and are never offered: the D-pad, OK, Back, Home and
Menu; power and sleep; volume and mute; playback (play, pause, stop, next, previous, rewind, fast-forward, record);
channel up and down, TV and every input key; settings, search, assistant and voice; the number and letter keys, Space,
Delete, Tab and Page Up/Down; a game controller's A, B, Start and Select. The list is `RemoteKeyPolicy`, checked on the
Android keycode, so a protected button stays protected whatever its scancode is. A stored choice for a protected keycode
is ignored on load. Entries a theme lists in `app_keys` are the owner's own file and are not filtered.

## How it works

- `RemoteKeyLearn`: the key helpers (`InputKeyService`, `HomeKeyService`) offer every key to it first; while the screen is
  listening it takes the next assignable button and drops that press, including its release.
- `RemoteKeyChoices`: the choices, in their own preferences file (`remote_keys`), so pushing a theme does not wipe them.
  A button is stored by its scancode when it has one (`scan:398`), otherwise its keycode. The helpers add the choices
  after the theme's `app_keys` and reload them as soon as one is saved, so a new assignment works at once.
- `RemoteKeysActivity`: the screen. It uses the Launcher settings layout and styles, and the app picker has its own
  dialog theme (`RemoteKeysDialog`) in the same colours.

## Limits

- A button can only open an app; there is no "do nothing" choice yet.
- Scancodes belong to one remote: a button assigned with the Bluetooth remote does not carry over to an IR remote.
- The strings are English only (`translatable="false"`); an upstream pull request would need them in every `values-*` folder.

## Tested on

Xiaomi FX 55" Fire TV (AFTNE4E43F), Fire OS 8.1.8.5 / Android 11, with its Bluetooth remote: assigning, changing and
removing a coloured button works. The screen was also checked on an
Android 12 TV emulator for layout and focus. The protected-button list is covered by `RemoteKeyPolicyTest`; the storage and
merge rules by `RemoteKeyChoicesTest`.
