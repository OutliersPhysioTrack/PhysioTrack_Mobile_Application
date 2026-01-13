# PhysioTrack — Mobile App  


PhysioTrack adalah aplikasi pasien untuk rehab jarak jauh yang menggabungkan **program latihan**, **tracking progres**, **monitoring sensor device (IoT)**, dan **kamera + pose estimation (MediaPipe)** yang terhubung ke **backend PhysioTrack (REST API)**.


---


## Fitur Utama


### 1) Auth & Onboarding
- **Register** (`/auth/register`) dan **Login** (`/auth/login`)
- Menyimpan `patient_id` secara lokal (persisten) via `AuthSession` + `KeyValueStore`


### 2) Home (Monitoring)
- Menampilkan:
  - status koneksi device (online/offline) + “needs calibration” (heuristik)
  - latest sensor readings (polling berkala)
  - AI prediction snapshot (polling berkala)
  - ringkasan “Today’s Plan” dari assignment backend


### 3) Program (Assignments)
- Menampilkan daftar program/assignment latihan dari backend (`/assignments?patient_id=...`)
- Menjadi “source of truth” untuk jadwal latihan pasien (server-driven)


### 4) Train (Latihan + Library)
- **Exercise Library lokal** (GIF di `androidMain/assets/exercise_videos/...`) untuk tutorial visual
- **Training Session Android (CameraX + MediaPipe Pose)**:
  - kamera live + overlay
  - counter repetisi (tergantung jenis exercise)
  - audio cue / guidance (file mp3 di `androidMain/res/raw`)
  - sinkronisasi session ke backend (create/patch)
  - auto-record clip highlight dan upload ke backend (jika endpoint tersedia)

> Catatan: di Desktop/JVM, Training Session masih placeholder (“Android first”).


### 5) Grip Strength Test (Loadcell via Backend)
- Mengambil data **latest sensor reading** khusus `LOADCELL_KG` dari backend:
  - endpoint: `/devices/{device_id}/sensor-readings/latest?metrics=LOADCELL_KG`
- Flow:
  - 3 attempt, masing-masing ~3 detik
  - menunggu “fresh reading” (timestamp/value berubah) supaya tidak memakai nilai lama


### 6) Progress (Analitik ringan)
- Mengambil `sessions` dan `notes` dari backend:
  - `/sessions?patient_id=...`
  - `/notes?patient_id=...&only_new=true`
- Menghitung:
  - total sessions
  - streak (current / longest)
  - adherence window 7 hari (berdasarkan ada/tidaknya sesi per hari)
  - trend grip dari `session.grip_avg_kg` (jika backend menyimpan nilai ini di session)


### 7) Profile
- Toggle theme, Help Center, Logout
- **API Settings** untuk mengubah `baseUrl`, `deviceId`, `patientId` tanpa rebuild (penting karena tunnel URL bisa berubah)



---


## Tech Stack

- **Kotlin Multiplatform (KMP)**: target **Android** + **Desktop (JVM)**
- **Compose Multiplatform** (UI)
- **Ktor Client** (HTTP)
- **CameraX** (Android)
- **Google MediaPipe Tasks Vision** (`com.google.mediapipe:tasks-vision:0.10.29`)
- Storage sederhana:
  - Android: `SharedPreferences` (via `KeyValueStore.android`)
  - JVM: file-based (via `KeyValueStore.jvm`)
- Assets:
  - Pose model: `composeApp/src/androidMain/assets/pose_landmarker_lite.task`
  - Tutorial GIF: `composeApp/src/androidMain/assets/exercise_videos/*.gif`
  - Logo: `composeApp/src/androidMain/assets/pt_logo.jpeg`


---


## Struktur Project (yang penting)

- `composeApp/src/commonMain/`  
  UI + model + networking yang dipakai lintas platform.
- `composeApp/src/androidMain/`  
  Implementasi Android: CameraX, MediaPipe helper, audio cue, asset loader, dsb.
- `composeApp/src/jvmMain/`  
  Implementasi Desktop/JVM (sebagian masih placeholder).


---


## Menjalankan Aplikasi

### Android (disarankan)
1. Buka project di **Android Studio**.
2. Pilih run configuration Android.
3. Jalankan ke emulator atau device.

Build lewat terminal:
- macOS/Linux
  ```bash
  ./gradlew :composeApp:assembleDebug
