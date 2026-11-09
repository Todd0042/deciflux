# Contributing to Deciflux

Thank you for your interest in contributing to **Deciflux**! We welcome bug reports, workflow suggestions, documentation improvements, and pull requests.

---

## 🛠️ Development Setup

Deciflux is built with modern Android development standards:
* **Language**: Kotlin 2.0+ with Kotlin Serialization
* **UI**: Jetpack Compose & Material 3
* **Audio DSP**: Embedded Native FFmpeg (via `youtubedl-android` + NDK shared libraries)
* **Architecture**: MVI / MVVM with StateFlow & Foreground Services
* **JDK**: Version 17

### Building from Source

```bash
# Clone the repository
git clone https://github.com/Todd0042/deciflux.git
cd deciflux

# Run the unit test suite
./gradlew testDebugUnitTest

# Assemble the debug APK
./gradlew assembleDebug
```

The compiled APK will be located at:
`app/build/outputs/apk/debug/app-debug.apk`

---

## 🧪 Testing Guidelines

1. **Unit Tests**:
   - Run `./gradlew testDebugUnitTest` locally before opening a pull request.
   - Maintain unit test coverage for view models, normalization presets, state repositories, and URI utilities.
2. **Device Smoke Testing**:
   - Deciflux operates with native FFmpeg binaries and Android scoped storage. Always smoke-test video imports, EBU R128 analysis, and remuxing on a real physical device or test device.

---

## 📜 Pull Request Process

1. Fork the repository and create your feature branch:
   ```bash
   git checkout -b feat/your-feature-name
   ```
2. Commit your changes with clear, descriptive commit messages adhering to Conventional Commits (e.g. `feat: ...`, `fix: ...`, `docs: ...`).
3. Ensure the test suite passes:
   ```bash
   ./gradlew testDebugUnitTest assembleDebug
   ```
4. Push to your fork and submit a Pull Request to `main`.

---

## 📄 Licensing

Deciflux is distributed under the **PolyForm Noncommercial License 1.0.0**. By contributing to Deciflux, you agree that your contributions will be licensed under the same terms.
