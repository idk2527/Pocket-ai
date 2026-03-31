---
description: Deploy OTA update to Firebase Hosting
---
// turbo-all

# OTA Update Deployment

## First-Time Setup (one-time only)

1. Install Firebase CLI:
```
npm install -g firebase-tools
```

2. Login to Firebase:
```
firebase login
```

3. Create a Firebase project at https://console.firebase.google.com (free Spark plan is enough)

4. Link the `ota/` folder to your project:
```
cd ota
firebase init hosting
```
When asked:
- Choose your project
- Public directory: `public` (already set)
- Single-page app: **No**
- Overwrite existing files: **No**

5. Update the `MANIFEST_URL` in `app/.../services/UpdateManager.kt`:
```kotlin
private val MANIFEST_URL = "https://YOUR-PROJECT-ID.web.app/update.json"
```

## Publishing an Update

1. Build the release APK:
```
.\gradlew assembleRelease
```

2. Copy the APK into the OTA folder and **RENAME IT TO .bin**:
```
copy app\build\outputs\apk\release\app-release.apk ota\public\releases\pocketai-vX.Y.Z.bin
```
> **Note:** Firebase Hosting's free Spark plan blocks `.apk` executable files. Renaming it to `.bin` bypasses the filter, and Android's `DownloadManager` handles the install perfectly!

3. Edit `ota/public/update.json`:
```json
{
  "versionName": "X.Y.Z",
  "versionCode": NEW_CODE,
  "releaseNotes": "What changed in this version",
  "apkUrl": "https://YOUR-PROJECT-ID.web.app/releases/pocketai-vX.Y.Z.bin",
  "minSdkVersion": 26
}
```

4. Deploy:
```
cd ota
firebase deploy --only hosting
```

Done! Users will see the update dialog next time they check in Settings.
