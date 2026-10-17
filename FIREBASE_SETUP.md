# Set up Google Sign-In and encrypted backups

Bloodflow 1.3 adds **optional** Google login and passphrase-encrypted, manual backup/restore. Until you configure Firebase, the app still works entirely as a guest; Google login displays a configuration notice rather than failing unexpectedly.

1. Open [Firebase Console](https://console.firebase.google.com/) and create a project (Spark/free plan if eligible).
2. Add an Android app with the **exact** package `com.bloodflow.app`. Turn on Google in **Authentication → Sign-in method**.
3. In Firebase Project Settings → Android app, register the **SHA-1** fingerprint of the certificate signing the APK. Find it in the output of the GitHub Actions step “Show Android signing SHA-1 for Firebase configuration” on a successful build. Preserve the signing key for app upgrades. If you later use Play App Signing, add the Play signing certificate too.
4. Under Google Cloud credentials (or Firebase project's Google OAuth client settings), identify the **Web application** OAuth client ID associated with the project, ending in `.apps.googleusercontent.com`. This is the server client ID used for `GetGoogleIdOption`, not the Android OAuth client ID.
5. In your GitHub repository, open **Settings → Secrets and variables → Actions → New repository secret**. Add the four values:

| Secret | Value |
|---|---|
| `BLOODFLOW_FIREBASE_API_KEY` | Firebase project's web API key |
| `BLOODFLOW_FIREBASE_APP_ID` | Firebase Android app ID, e.g. `1:...:android:...` |
| `BLOODFLOW_FIREBASE_PROJECT_ID` | Firebase project ID |
| `BLOODFLOW_FIREBASE_WEB_CLIENT_ID` | Google OAuth Web client ID |

Never paste these into screenshots or chats. No `google-services.json` is needed because Bloodflow initializes Firebase from generated BuildConfig values. Configuration is injected during CI compilation.

6. Create Firestore in production mode. Paste these **Firestore Security Rules**:

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /bloodflow_backups/{userId} {
      allow read, write: if request.auth != null
        && request.auth.uid == userId
        && request.resource.data.keys().hasOnly(['encrypted','format','updatedAt'])
        && request.resource.data.encrypted is string
        && request.resource.data.encrypted.size() < 900000
        && request.resource.data.format == 1;
    }
  }
}
```

**Important**: The Firestore rule above validates writes only. Because `request.resource` is absent on reads, separate read and write rules are required! Use these actual rules instead:

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /bloodflow_backups/{userId} {
      allow get: if request.auth != null && request.auth.uid == userId;
      allow list: if false;
      allow create, update: if request.auth != null
        && request.auth.uid == userId
        && request.resource.data.keys().hasOnly(['encrypted','format','updatedAt'])
        && request.resource.data.encrypted is string
        && request.resource.data.encrypted.size() < 900000
        && request.resource.data.format == 1;
      allow delete: if request.auth != null && request.auth.uid == userId;
    }
  }
}
```

7. Run GitHub Actions → **Build Bloodflow APK** on the `main` branch again. Download the **new run's artifact**, not any older ZIP. Install over the existing app, without clearing data.

## Privacy and security
- Sign-in is optional; it **never** triggers an automatic upload.
- “Create / replace encrypted backup” uploads only AES-256-GCM ciphertext, encrypted locally using a 210,000-iteration PBKDF2-HMAC-SHA256-derived key from a user-chosen passphrase.
- The passphrase is **not** uploaded or saved. If forgotten, the cloud backup cannot be decrypted.
- Restoring requires explicit confirmation and overwrites the local history. Sign-out does not erase local data.
- This is a single manual backup snapshot, not continuous synchronization. Additional security review and independent testing are advised before distributing a health-data app widely.
- Firestore reads occur from the server for explicit restore. Configured Firebase services require internet; ordinary cycle tracking still works offline.
- Do not publish API configuration as a secret substitute for Firestore rules; access control depends on Firebase Auth and strong rules.
- Disclose Firebase use and health-data processing in a real privacy policy before public distribution.
