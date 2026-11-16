---
name: APK Packer
description: Harden and protect the release build of an Android app you own — R8 shrinking/obfuscation, resource shrinking, signing, secret hygiene, tamper and repackaging detection. Load this whenever the user asks to "pack" an APK, protect an app, obfuscate code, prevent reverse engineering of their own app, or ship a safer release build.
---

# APK Packer (release hardening)

Goal: make the shipped APK small, fast and resistant to casual inspection **without** breaking it. Only apply this to apps the user owns or is authorised to build.

## 1. Baseline hardening (do this first)

In the app module `build.gradle.kts`:

```kotlin
buildTypes {
    release {
        isMinifyEnabled = true          // R8: shrink + obfuscate
        isShrinkResources = true        // drop unused resources
        proguardFiles(
            getDefaultProguardFile("proguard-android-optimize.txt"),
            "proguard-rules.pro"
        )
    }
}
```

R8 renames classes/fields and removes dead code. Expect to add `-keep` rules for reflection, serialization models, JNI entry points, and anything instantiated by name. Run the release build and check `mapping.txt` plus the R8 report in `app/build/outputs/mapping/release/`.

## 2. Secrets

Obfuscation never hides a string constant that is actually in the APK. Keys, tokens and endpoints that must stay secret belong on a server, never in `BuildConfig`, `strings.xml`, assets, or native libs. Grep the release APK for `api_key`, `token`, `secret`, `Bearer` before publishing.

## 3. Signing and integrity

- Generate a dedicated upload keystore, keep it out of the repo, and sign in CI from repository secrets.
- Prefer Play App Signing when distributing through Google Play.
- Add a runtime signature check only if it degrades gracefully; a hard crash on mismatch mostly annoys legitimate users with modified builds.
- For stronger server-trust guarantees use Play Integrity (Play-distributed apps) and validate the verdict server-side.

## 4. Tamper / repackaging signals

Detectable signals: resigning (certificate hash change), installer package not the store, debugger attached, emulator fingerprint, unexpected `X-Android-*` headers. Treat each as one input to a risk score, and make the server the decision point. Never hardcode the pass/fail logic client-side only — it is a two-minute patch.

## 5. Verify the result

1. `./gradlew assembleRelease`
2. `apkanalyzer apk summary`, `apkanalyzer dex packages` to confirm renaming happened.
3. Install the release build on a device and exercise the main flows (R8 breakage is usually reflection or serialization).
4. Keep `mapping.txt` per release for crash deobfuscation.

## Report format

Return: what changed, expected size delta, keep-rules added, what still leaks (and why it must move server-side), and the exact commands used.
