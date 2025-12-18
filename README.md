# 🎬 MovieTime Tracker

<div align="center">

![Android](https://img.shields.io/badge/Platform-Android-green.svg)
![Kotlin](https://img.shields.io/badge/Language-Kotlin-blue.svg)
![API](https://img.shields.io/badge/Min%20API-24-orange.svg)
![License](https://img.shields.io/badge/License-MIT-yellow.svg)

**Track your movies and TV shows with ease**

An elegant Android app to manage what you watch, plan to watch, and have watched

[Features](#-features) • [Tech Stack](#-tech-stack) • [Architecture](#️-architecture) • [Installation](#-installation) • [Screenshots](#-screenshots)

</div>

---

## 📱 About

**MovieTime Tracker** is a modern Android application for tracking movies and TV series. Built with clean architecture principles and powered by **TMDB API**, it offers an intuitive interface for managing your entertainment library.

## ✨ Features

- **Track Your Content** — Organize movies/series into watched, watching, and planned lists
- **Detailed Statistics** — View total watch time, content count, and average ratings
- **Smart Search** — Find any movie or TV show with advanced filtering
- **Personal Ratings** — Rate content and see TMDB ratings
- **Trending Content** — Discover popular movies and series
- **Upcoming Releases** — Stay informed about new premieres
- **Dark Theme** — Comfortable viewing at any time
- **Multi-language** — Support for Ukrainian, Russian, and English
- **Modern Design** — Material Design 3 with smooth animations

---

## 🛠 Tech Stack

### Core Technologies

- **Language:** Kotlin 1.9.0
- **Min SDK:** 24 (Android 7.0)
- **Target SDK:** 34 (Android 14)
- **Build System:** Gradle 8.2.2

### Architecture & Components

- **MVVM Pattern** — Clean separation of concerns
- **Android Jetpack:**
  - ViewModel & LiveData — State management
  - Navigation Component — Fragment navigation
  - Room Database — Local data persistence
  - ViewBinding — Type-safe view access
- **Hilt (Dagger)** — Dependency injection
- **Kotlin Coroutines** — Asynchronous programming
- **Retrofit + OkHttp** — Network communication
- **TMDB API** — Movie and TV show data

### UI Libraries

- **Material Design 3** — Modern UI components
- **Coil** — Image loading and caching
- **Lottie** — Vector animations
- **RecyclerView** — Efficient list rendering

---

## 🏗️ Architecture

This app follows the **MVVM (Model-View-ViewModel)** architecture pattern with clean separation between layers:

```
┌─────────────────────────────────────────────┐
│  UI Layer (Activities, Fragments)          │
├─────────────────────────────────────────────┤
│  ViewModel Layer (LiveData, State)         │
├─────────────────────────────────────────────┤
│  Repository Layer (Data coordination)      │
├─────────────────────────────────────────────┤
│  Data Sources (Remote API + Local DB)      │
└─────────────────────────────────────────────┘
```

### Key Components

**UI Layer:**

- Activities: Main, Search, Details
- Fragments: Watched, Planned, Statistics
- Adapters: RecyclerView with DiffUtil

**ViewModel Layer:**

- State management with LiveData
- Coroutine scopes for async operations
- Business logic coordination

**Repository Layer:**

- Single source of truth
- Network and database coordination
- Error handling

**Data Layer:**

- **Remote:** TMDB API via Retrofit
- **Local:** Room Database for offline access
- **Caching:** In-memory and disk caching

---

## 📦 Installation

### Prerequisites

- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17+
- Android SDK 34
- TMDB API Key ([Get one free](https://www.themoviedb.org/signup))

### Setup

1. **Clone the repository**

```bash
git clone https://github.com/yourusername/MovieTimeTracker.git
cd MovieTimeTracker
```

2. **Add your TMDB API Key**

Create a `local.properties` file in the root directory:

```properties
TMDB_API_KEY=your_api_key_here
```

3. **Build and run**

```bash
./gradlew assembleDebug
```

Or open in Android Studio and click Run ▶️

---

## 🗄️ Database Schema

### Room Tables

**watched_items**

- Stores watched movies and TV shows
- Fields: id, title, type, rating, runtime, watchedDate

**planned_items**

- Content planned to watch
- Fields: id, title, type, posterPath, addedDate

**watching_items**

- Currently watching series
- Fields: id, title, currentEpisode, totalEpisodes, lastWatchedDate

---

## 🌐 API Integration

This app uses [TMDB API](https://www.themoviedb.org/documentation/api) for fetching movie and TV show data.

**Main endpoints:**

- `/search/multi` — Universal search
- `/movie/{id}` — Movie details
- `/tv/{id}` — TV show details
- `/trending/{type}/{window}` — Trending content
- `/movie/upcoming` — Upcoming releases

---

## 📸 Screenshots

<div align="center">

| Home Screen                   | Search                            | Details                             | Statistics                      |
| ----------------------------- | --------------------------------- | ----------------------------------- | ------------------------------- |
| ![Home](screenshots/home.png) | ![Search](screenshots/search.png) | ![Details](screenshots/details.png) | ![Stats](screenshots/stats.png) |

</div>

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## 🙏 Acknowledgments

- [TMDB](https://www.themoviedb.org/) for providing the API
- [Material Design](https://material.io/) for design guidelines
- [Android Jetpack](https://developer.android.com/jetpack) for architecture components

---

<div align="center">

**Made with ❤️ for movie and TV show enthusiasts**

[⬆ Back to top](#-movietime-tracker)

</div>
