---
name: APK Signing
description: Create a keystore, sign release APKs with apksigner or Gradle, verify signatures, and set up CI signing with GitHub Actions secrets. Load this whenever a build fails at signing, the user asks how to sign an APK, or an APK must be re-signed after being rebuilt.
---

# APK Signing

## Create a keystore (once)

```sh
keytool -genkeypair -v -keystore release.jks -alias key0 \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -storepass <storepass> -keypass <keypass> \
  -dname "CN=App, OU=Dev, O=App, C=FR"
```

Never commit the keystore or its passwords. Losing the keystore means you cannot update the app under the same identity.

## Gradle signing config

```kotlin
android {
    signingConfigs {
        create("release") {
            storeFile = file(System.getenv("KEYSTORE_PATH") ?: "release.jks")
            storePassword = System.getenv("STORE_PASSWORD")
            keyAlias = System.getenv("KEY_ALIAS")
            keyPassword = System.getenv("KEY_PASSWORD")
        }
    }
    buildTypes {
        release { signingConfig = signingConfigs.getByName("release") }
    }
}
```

## CI signing (GitHub Actions)

Store the keystore as base64 in a repository secret and decode it in the job:

```yaml
- name: Setup signing
  env:
    KEYSTORE_BASE64: ${{ secrets.SIGNING_KEYSTORE_BASE64 }}
  run: |
    mkdir -p ~/.android_packing
    printf '%s' "$KEYSTORE_BASE64" | base64 -d > ~/.android_packing/release.jks
```

`base64 -w0 release.jks` produces the secret value. CI-generated throwaway keystores are fine for artifacts the user installs manually, but not for store updates.

## Verify

```sh
apksigner verify --verbose --print-certs app-release.apk
```

Check the certificate digest matches the expected signing identity before publishing.
