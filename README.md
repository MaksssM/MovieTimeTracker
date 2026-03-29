# 🎬 MovieTime Tracker

<div align="center">

![Android](https://img.shields.io/badge/Platform-Android-green.svg)
![Kotlin](https://img.shields.io/badge/Language-Kotlin%202.0-blue.svg)
![API](https://img.shields.io/badge/Min%20API-24-orange.svg)
![Room](https://img.shields.io/badge/Room%20DB-v15-purple.svg)
![Build](https://img.shields.io/badge/Build-Passing-brightgreen.svg)

**A personal movie & TV show tracker for Android**

</div>

---

## About

MovieTime Tracker lets you keep track of everything you watch. Powered by the [TMDB API](https://www.themoviedb.org/), it stores your data locally via Room DB and optionally syncs social features through Firebase.

**Supported languages:** English · Ukrainian · Russian

---

## Features

- **Three watch lists** — Watched, Watching, Planned — with quick transfer between them
- **Personal ratings** (0.5–10) and rewatch history
- **TV show progress** — track seasons and episodes
- **Advanced search & filters** — genre, year, rating, runtime, language, streaming service
- **Trending & upcoming** — discover what is popular right now
- **Recommendations** — based on your watch history, sorted by popularity
- **Detailed statistics** — total watch time, genre breakdown, runtime records, Year in Review charts
- **Cinematic Universes** — MCU, DCEU, Star Wars, Wizarding World, Middle-earth with Universe → Saga → Entry hierarchy and per-entry watch progress
- **Actor / director pages** — filmography sorted by popularity
- **Custom collections** — personal lists with emoji cover art
- **Social features** _(requires Firebase)_ — friends feed, spoiler-protected reviews, friend requests

---

## Tech Stack

| Area       | Technology                |
| ---------- | ------------------------- |
| Language   | Kotlin 2.0                |
| Min SDK    | Android 7.0 (API 24)      |
| Target SDK | Android 14 (API 34)       |
| DI         | Hilt 2.48                 |
| Database   | Room 2.6 (15 tables)      |
| Network    | Retrofit 2 + OkHttp       |
| Images     | Coil 2.5                  |
| Charts     | MPAndroidChart 3.1        |
| Animations | Lottie + Shimmer          |
| Backend    | Firebase Auth + Firestore |
| UI         | Material Design 3         |

Architecture: **MVVM + Repository + Hilt DI**

---

## Setup

1. Clone the repo and open in Android Studio (Iguana or newer, JDK 17+).

2. Add your TMDB API key to `local.properties`:

   ```
   TMDB_API_KEY=your_key_here
   ```

3. _(Optional)_ For social features — add `google-services.json` to `app/` and uncomment `id("com.google.gms.google-services")` in `app/build.gradle.kts`.

4. Build:
   ```bash
   ./gradlew assembleDebug
   ```

## License

MIT — see [LICENSE](LICENSE).
