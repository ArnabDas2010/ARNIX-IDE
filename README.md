# ARNIX IDE 🚀
> Clean, simple, lightweight and native Android IDE with coding tools, file explorer, syntax highlighting, terminal emulator, and GitHub integration.

---

## 📱 Features
- **Multi-Tab Code Editor**: Fast tab-switching, line numbers gutter, undo/redo history, and real-time syntax highlighting for Kotlin, Java, Python, JavaScript, JSON, and Markdown.
- **Mobile Quick Symbol Bar**: Dedicated coding keyboard row for quick access to `{ } ( ) [ ] ; = " : < > / \ $ _ + - * & | ! ? -> TAB`.
- **Hierarchical File Explorer**: Manage workspace projects, open files in editor, create new files/directories, and delete items.
- **Interactive Terminal & Build Console**: Run built-in shell commands (`help`, `run`, `build`, `ls`, `cat`, `git status`, `git branch`, `clear`, `echo`).
- **Git & GitHub Integration**: Switch and create branches, stage/unstage changes, write commit messages, and configure GitHub repository connection (URL, user, PAT).
- **IDE Settings & Customization**: Adjustable font size slider, line numbers toggle, word wrap toggle, and auto-save options.
- **Debug-Signed Release APKs**: Pre-configured with ABI splits to run on real Android devices without certificate issues.

---

## 🤖 GitHub Actions Automated Releases
This repository includes a GitHub Actions workflow (`.github/workflows/release.yml`) that automatically builds and publishes 3 APK variants on every Git tag (e.g. `v1.0.0`) or manual workflow dispatch:

| APK File | Architecture | Target Devices |
| :--- | :--- | :--- |
| **`ARNIX-IDE-universal.apk`** | **Universal** | Recommended if unsure. Installs on all Android phones (ARM32, ARM64, x86). |
| **`ARNIX-IDE-arm64-v8a.apk`** | **arm64-v8a (v8)** | Optimized for modern 64-bit Android smartphones. Smallest size & highest performance. |
| **`ARNIX-IDE-armeabi-v7a.apk`** | **armeabi-v7a (v7)** | Optimized for older 32-bit Android devices. |

### How to trigger a release:
1. **Push a Git tag**:
   ```bash
   git tag v1.0.0
   git push origin v1.0.0
   ```
2. **Or run manually from GitHub**:
   - Go to your repository's **Actions** tab on GitHub.
   - Select **Build & Release Android APKs**.
   - Click **Run workflow** (choose tag name e.g. `v1.0.0`).

All 3 APKs and `sha256_checksums.txt` will automatically attach to the GitHub Release, ready for direct download to your phone!

---

## 📦 Building Locally
```bash
# Build Universal, v8, and v7 release APKs
./gradlew :app:assembleRelease -PbuildSplits=true

# Output location
ls -la app/build/outputs/apk/release/
```
