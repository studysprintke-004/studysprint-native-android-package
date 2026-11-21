# StudySprint Android

Native Kotlin Android starter for StudySprint.

## Included in this first build
- Live document listing from the six Google Apps Script / Google Drive sources used by the existing StudySprint website.
- University selection, category filtering and document search.
- Downloads saved in the app's private Downloads folder.
- Offline downloads list with open and delete actions.
- GitHub Actions workflow that builds a debug APK in the cloud.

## Build the APK on GitHub
1. Upload the complete contents of this project to the root of your GitHub repository.
2. Open the repository's **Actions** tab.
3. Select **Build StudySprint Android APK**.
4. Press **Run workflow** (or wait for the automatic build after pushing to `main`).
5. Open the completed workflow run and download the `StudySprint-debug-APK` artifact.
6. Extract the artifact ZIP on your phone and install `app-debug.apk`. You may need to allow installation from your browser or file manager.

## Drive access note
The app uses the same Apps Script listing endpoints as the existing website. Downloading a file uses the standard Google Drive `uc?export=download&id=...` URL. The Drive file must be accessible to the downloader. If a file is private or Google displays a confirmation page instead of the PDF, we will update the download method after testing with one of your files.

## Next development stage
Firebase Authentication, profile sync, and the rest of the StudySprint website features are intentionally not included in this first APK build. We will add them after confirming that this base APK builds and that one real PDF can be downloaded and opened offline.
