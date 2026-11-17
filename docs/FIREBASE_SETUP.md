# Turn on Google sign-in and cloud sync

The app code is ready. It switches on when `app/google-services.json` exists.

1. Open https://console.firebase.google.com and create a project (or pick one).
2. **Add an Android app** with package name `com.unibud.app`.
   Add these fingerprints for the key that signs the build (`app/debug.keystore`, committed on purpose so every build has the same fingerprint):
   - SHA1: 65:4A:FA:75:A1:E0:18:53:9D:75:55:43:33:F8:34:47:EE:69:F6:C8
   - SHA256: 1B:88:81:CD:1F:26:EB:85:E9:91:E0:AB:D9:A8:28:94:39:9C:FB:A8:30:8E:DE:96:91:FE:36:12:15:76:ED:4E
3. **Authentication > Sign-in method:** enable **Google**.
4. **Firestore Database:** create a database, then open the Rules tab and paste the contents of `firestore.rules`.
5. Download `google-services.json` and put it in the `app/` folder, next to `build.gradle.kts`.
6. Commit and push. GitHub Actions builds the APK. Install it and sign in.

If sign-in says "no credentials" or shows a developer error, the SHA-1 above is missing from the Firebase Android app, or `google-services.json` was downloaded before the SHA-1 was added. Add it, download a fresh file and push again.

The debug key is public in this repo. That is fine for your own phone. Create a private keystore before publishing anywhere.
