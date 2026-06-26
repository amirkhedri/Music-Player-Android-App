
<div align="center">

# 🎵 Music Player

![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/kotlin-%237F52FF.svg?style=for-the-badge&logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-4285F4?style=for-the-badge&logo=android&logoColor=white)
![Material Design](https://img.shields.io/badge/Material%20Design%203-E52592?style=for-the-badge&logo=materialdesign&logoColor=white)
![Architecture](https://img.shields.io/badge/Architecture-MVVM-orange?style=for-the-badge)

A modern, feature-rich Android Music Player built entirely with **Kotlin** and **Jetpack Compose**, following the **MVVM Architecture**. The application provides an elegant UI, playlist management, artist browsing, favorites, local music playback, and a beautiful user experience with Material Design 3.

</div>

---

## 📱 Screenshots

<div align="center">

### 🔑 Authentication & Settings

<table>
  <tr>
    <td align="center">
      <img src="ScreenShots/Login.png" width="220"/>
      <br><b>Login Screen</b>
    </td>
    <td align="center">
      <img src="ScreenShots/Create Account.png" width="220"/>
      <br><b>Create Account</b>
    </td>
    <td align="center">
      <img src="ScreenShots/Equlizer.png" width="220"/>
      <br><b>Equalizer</b>
    </td>
  </tr>
</table>

### 🎶 Library & Discovery

<table>
  <tr>
    <td align="center">
      <img src="ScreenShots/Light Library.png" width="220"/>
      <br><b>Light Library</b>
    </td>
    <td align="center">
      <img src="ScreenShots/Dark Library.png" width="220"/>
      <br><b>Dark Library</b>
    </td>
    <td align="center">
      <img src="ScreenShots/Artists.png" width="220"/>
      <br><b>Artists View</b>
    </td>
  </tr>
</table>

### ▶️ Playback Experience

<table>
  <tr>
    <td align="center">
      <img src="ScreenShots/Light Player.png" width="220"/>
      <br><b>Light Player</b>
    </td>
    <td align="center">
      <img src="ScreenShots/Dark Player.png" width="220"/>
      <br><b>Dark Player</b>
    </td>
    <td align="center">
      <img src="ScreenShots/playscreen glassy (2).png" width="220"/>
      <br><b>Glassy Theme</b>
    </td>
  </tr>
</table>

### ❤️ Favorites & Immersive UI

<table>
  <tr>
    <td align="center">
      <img src="ScreenShots/Dark favourite.png" width="220"/>
      <br><b>Favorites</b>
    </td>
    <td align="center">
      <img src="ScreenShots/playscreen1.png" width="220"/>
      <br><b>Playback Visualization</b>
    </td>
    <td align="center">
      <img src="ScreenShots/playscreen2.png" width="220"/>
      <br><b>Immersive Controls</b>
    </td>
  </tr>
</table>

</div>

---

## ✨ Features

### 🎼 Music Library
* Scan and display all local audio files
* Beautiful song cards with album artwork
* Sort songs by Name, Artist, and Date Added
* Instant search
* Multi-selection mode
* Rename, delete, and share songs

### ❤️ Favorites
* Add/remove favorite songs
* Dedicated Favorites screen
* Search favorites instantly

### 👨‍🎤 Artists
* Automatically groups songs by artist
* Artist artwork support
* Artist detail page
* Search artists and search songs inside each artist

### 🎶 Playlists
* Create unlimited playlists
* Rename and delete playlists
* Add/remove songs from playlists
* Playlist search
* Beautiful playlist artwork

### ▶️ Music Playback
* Play/Pause, Skip Next, Skip Previous
* Custom Built-in Equalizer
* Queue Playback
* Mini Player
* Currently Playing Indicator
* Animated Playing Visualizer
* Album Artwork Support

### 🌙 UI & UX
* Material Design 3
* Dynamic Light/Dark Theme & Premium Glassy Theme
* Smooth Animations
* Responsive Layout
* Beautiful Card Design
* Modern Bottom Navigation
* Search Animations

### 🔐 Authentication
* Login
* Create Account / Register
* Logout
* Session Management

---

## 🏗 Architecture

The project follows the **MVVM (Model-View-ViewModel)** architecture.

```text
Presentation
│
├── UI (Jetpack Compose)
├── ViewModels
│
Domain
│
├── Repository
├── Business Logic
│
Data
│
├── Room Database
├── MediaStore
├── Preferences
└── Local Storage

```

---

## 🛠 Built With

* Kotlin
* Jetpack Compose
* Material Design 3
* MVVM Architecture
* Hilt (Dependency Injection)
* Kotlin Coroutines
* StateFlow
* Navigation Compose
* Room Database
* Coil
* Android MediaStore
* Android Storage Access Framework
* ExoPlayer / Media3
* DataStore
* AndroidX

---

## 📂 Project Structure

```text
app/
│
├── data/
│   ├── model/
│   ├── repository/
│   └── local/
│
├── ui/
│   ├── screens/
│   ├── components/
│   └── theme/
│
├── viewmodel/
│
├── navigation/
│
├── service/
│
└── MainActivity.kt

```

---

## 🚀 Getting Started

### Prerequisites

* Android Studio Meerkat (or newer)
* Android SDK 35+
* Kotlin 2.x
* Gradle 8+

### Installation

1. Clone the repository:

```bash
git clone [https://github.com/amirkhedri/Music-Player-Android-App.git](https://github.com/amirkhedri/Music-Player-Android-App.git)

```

2. Open in Android Studio (`File → Open`)
3. Sync Gradle
4. Run the application on an Android Emulator or a Physical Device (Android 8.0+)

---

## 📋 Permissions

The application requires the following permissions. Depending on the Android version, runtime permissions are requested automatically.

```xml
<uses-permission android:name="android.permission.READ_MEDIA_AUDIO" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

```

---

## 📦 Libraries Used

| Library | Purpose |
| --- | --- |
| **Jetpack Compose** | UI |
| **Navigation Compose** | Navigation |
| **Hilt** | Dependency Injection |
| **Room** | Local Database |
| **Coil** | Image Loading |
| **Media3 / ExoPlayer** | Audio Playback |
| **DataStore** | Preferences |
| **Coroutines** | Async Programming |
| **StateFlow** | UI State |

---

## 🎯 App Workflow

```text
Login / Register
   │
   ▼
Library
   │
   ├── Favorites
   ├── Artists
   │      └── Songs
   ├── Playlists
   │      └── Playlist Songs
   └── Player
          └── Equalizer

```

---

## 📸 Main Screens

* Login / Create Account
* Library
* Favorites
* Artists
* Artist Details
* Playlists
* Playlist Details
* Full Music Player
* Equalizer

---

## 🎨 Design Principles

* Material Design 3
* Responsive Layout
* Modern Animations
* Smooth User Experience
* Minimalistic Interface
* Accessibility Friendly

---

## 🔒 Data Management

The application stores the following locally on the device:

* Favorite songs
* Custom playlists
* Theme preferences
* User session
* Custom equalizer profiles

---

## ⚡ Performance Optimizations

* LazyColumn & LazyVerticalGrid
* remember {}
* StateFlow
* Coroutines
* Image caching with Coil
* Efficient recomposition
* MediaStore querying

---

## 🔮 Future Improvements

* Lyrics Support
* Sleep Timer
* Recently Played & Most Played
* Album Screen
* Folder Browser
* Shuffle All & Repeat Modes
* Cloud Backup
* Online Streaming
* Wear OS & Android Auto Support
* Chromecast

---

## 🤝 Contributing

Contributions are welcome!

1. Fork the repository
2. Create a feature branch:

```bash
git checkout -b feature/NewFeature

```

3. Commit changes:

```bash
git commit -m "Add New Feature"

```

4. Push:

```bash
git push origin feature/NewFeature

```

5. Open a Pull Request

---

## 👨‍💻 Author

**Amir Khedri**

* Computer Engineering Student
* University of Isfahan

---

## 📄 License

This project is licensed under the MIT License.

```text
MIT License

Copyright (c) 2026 Amir Khedri

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.

```

---

## ⭐ Support

If you like this project, please give it a ⭐ on GitHub! It helps support the project and motivates future development.

## 📧 Contact

* **GitHub:** [https://github.com/amirkhedri](https://github.com/amirkhedri)
* **Email:** [khedria95@gmail.com](mailto:khedria95@gmail.com)

```

```