# Color Picker for Android

An interactive color picker for Android – allows developers, designers, and artists to capture and organize the colors of everyday scenes using their camera. 🎨

## 📱 Features

- **Real-Time Color Detection**: Instantly identify colors from your camera feed.
- **Color Palette Management**: Save, label, and organize your favorite colors.

## 🎨 Reference

For an overview of the app's design and architecture:

- [System Architecture](https://www.figma.com/board/LAGL4SfbvMXu1iIJbLBXf6/Architecture)
- [User Interface](https://www.figma.com/design/kk8f2Lwy6ks7yIL5vlG9d4/App)

## 🚀 Getting Started

### ✅ Pre-Requirements

- Android Studio (2025.1.2+)
- Android SDK (API level 31+)
- An Android device or emulator using Android 12 (API level 31+) or later

### 🛠️ Installing and Building

1. 📋 **Clone the Repository**:

   ```bash
   git clone https://github.com/castanie/AP-Projekt.git
   ```

2. 🤖 **Open in Android Studio**:

    Launch Android Studio and open the cloned project directory.

3. 🐘 **Sync Gradle**:

    Allow Android Studio to sync the project and download necessary dependencies.

4. 🔧 **Configure the Device**:

    Make sure your Android version is 12 or greater (API level 31+).

    Because this project uses ARCore, you **must** install _Google Play Services for AR_ to launch the app.
    - If you are using a **real device**, install the services via the [Google Play Store](https://play.google.com/store/apps/details?id=com.google.ar.core).
    - If you are using an **emulator**, install the services using the **emulator-specific APK** from the [ARCore GitHub Repository](https://github.com/google-ar/arcore-android-sdk/releases). You can simply drag-and-drop it onto the running emulator.

5. ✨ **[_OPTIONAL_]** **Configure the Project:**

    The project is already configured to work on emulators.

    Depending on which [ARCore features](https://developers.google.com/ar/devices) the device supports, it is possible to adjust following configuration entries in the main activity:
    ```kt
    session = Session(applicationContext).apply {
        configure(Config(this).apply {
            // ...
            depthMode = Config.DepthMode.DISABLED
            // ...
        })
    }
    ```

6. ▶️ **Run the Application**:

   Connect an Android device or start an emulator, then click on the _Run_ button in Android Studio.


## 📢 Disclosure

This README was generated with the help of AI (_ChatGPT – GPT-5_) and refined by hand.


## 📄 License

All rights reserved.