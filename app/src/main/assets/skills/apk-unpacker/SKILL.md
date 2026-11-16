---
name: APK Unpacker
description: Inspect and unpack an APK the user owns or is authorised to analyse — decode resources/manifest with apktool, read DEX with jadx/apkanalyzer, inspect native libs and assets, and repackage after edits. Load this whenever the user asks to unpack, decompile, disassemble, inspect, or re-sign an APK, read an app manifest, or extract resources from an APK.
---

# APK Unpacker

Use only on APKs the user owns, built themselves, or has written permission to analyse. For third-party apps, prefer black-box observation (`aapt`/`apkanalyzer` metadata) over decompiling proprietary code, and never redistribute extracted assets.

## Quick metadata (no unpacking)

```sh
aapt dump badging app.apk          # package, versionName, permissions, launchable activity
apkanalyzer apk summary app.apk
apkanalyzer manifest print app.apk
unzip -l app.apk                   # entries, sizes, native libs, assets
```

## Full decode (manifest + resources)

```sh
apktool d -f app.apk -o decoded/            # smali + res + AndroidManifest.xml
apktool b decoded/ -o rebuilt.apk           # rebuild after edits
```

`decoded/AndroidManifest.xml` is readable XML, `decoded/res/` holds decoded resources, `decoded/smali*/` holds Dalvik bytecode.

## Java-level reading

```sh
jadx -d out/ app.apk                         # decompile DEX to Java
jadx --show-bad-code -d out/ app.apk         # include hard-to-decompile methods
```

`jadx` output is a reading aid, not compilable source.

## Native libraries

```sh
unzip -o app.apk 'lib/*' -d libs/
file libs/arm64-v8a/*.so
readelf -d libs/arm64-v8a/libfoo.so   # NEEDED libs, JNI exports
nm -D --defined-only libs/arm64-v8a/libfoo.so | head -50
```

Java entry points are `Java_<package>_<Class>_<method>` symbols; use `jadx` to find the matching `native` declarations.

## Repackaging

Rebuilding breaks the signature, so the APK must be re-signed and the original removed first:

```sh
apksigner sign --ks release.jks --out signed.apk rebuilt.apk
apksigner verify --print-certs signed.apk
```

An app that checks its own certificate will refuse a re-signed build — that is expected behaviour, not a bug to work around.

## Integrity checks

If a decoded manifest or DEX looks encrypted/packed, record the finding (packer name, entry class, size ratio) and report it. Do not attempt to defeat protections on software you do not own.

## Report format

Return: package/version, permissions of interest, entry activities, notable libraries, extracted artifacts with paths, and open questions.
