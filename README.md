# Stop2End

An Android app that dismisses political spam text notifications from **Google Messages** and **Google Voice**.

It watches incoming message notifications and dismisses any that contain "stop to end", "end to end" or a
variation (`stop2end`, `STOP 2 END`, `STOP=END`, `END2END`, `stop to quit`, `end to quit`, `stop to opt out`,
`stop to unsubscribe`, including fullwidth, bold-Unicode, lookalike-letter and zero-width-character tricks).

Messages are **not** dismissed when they come from:

- a saved contact
- a number or name on the in-app allowlist
- a 5- or 6-digit short code (banks, pharmacies, login codes)
- a group conversation

Only the notification is dismissed. The message itself stays in your Messages / Voice inbox, because
Android only lets the default SMS app block or delete texts.

## Install

1. Open the latest successful run under the repository's **Actions** tab and download the
   `stop2end-debug-apk` artifact. Unzip it to get `app-debug.apk`.
2. Copy it to your phone and open it, allowing your browser or file manager to install unknown apps,
   or run `adb install app-debug.apk`.
3. Open Stop2End and tap **Grant access**, then turn on notification access for Stop2End.
   If the switch is greyed out, go back, tap **App info** › ⋮ › **Allow restricted settings**, and try again.
4. Tap **Allow contacts access** so messages from contacts are always kept. Without it, any sender shown
   by name (not by number) is treated as a contact.

The **Filtering active** switch pauses and resumes filtering instantly.

### Installing updates

GitHub Actions makes a new debug signing key for every build, so Android refuses to install one build over
another ("App not installed"). Either uninstall first (this resets the allowlist), or give CI a fixed key:

```sh
# Uses Android Studio's debug key if you have one; otherwise create one:
keytool -genkeypair -v -keystore debug.keystore -storepass android -alias androiddebugkey \
  -keypass android -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Android Debug,O=Android,C=US"
base64 -w0 debug.keystore   # macOS: base64 -i debug.keystore
```

Save the output as a repository secret named `DEBUG_KEYSTORE_BASE64`
(Settings › Secrets and variables › Actions). Uninstall once, then later builds install as updates.

## Develop

Open the project in Android Studio, or build from the command line with JDK 17+ and the Android SDK:

```sh
./gradlew testDebugUnitTest assembleDebug
```

To see what the filter decided for each notification:

```sh
adb logcat -s Stop2End
```

| File | Purpose |
|---|---|
| `filter/SpamMatcher.kt` | Text normalization and the "stop to end" pattern |
| `filter/MessageFilter.kt` | Skip rules: group, short code, allowlist, contacts |
| `NotificationParser.kt` | Extracts sender and text from a notification |
| `ContactsChecker.kt` | Looks up senders in your contacts |
| `SpamListenerService.kt` | The notification listener that dismisses spam |
| `MainActivity.kt` | Settings screen |
