# 🔴 Nokia Bounce Classic - Native Android Remaster

Porting Native Android mandiri untuk game legendaris ponsel Nokia **Bounce Classic** (v1.24) dengan runtime J2ME kustom berkinerja tinggi, tampilan retro Nokia Series 40, kontrol keypad Nokia lengkap, serta panel cheat rahasia bawaan.

[![Download APK](https://img.shields.io/badge/Download-Bounce.apk-red?style=for-the-badge&logo=android)](https://github.com/GUPRON98/Bounce-Android/raw/main/Bounce.apk)

---

## 📥 Unduh Game (Download APK)

Anda dapat langsung mengunduh file APK yang sudah jadi dan siap dimainkan:
- **Direct Download**: [Bounce.apk](https://github.com/GUPRON98/Bounce-Android/raw/main/Bounce.apk)
- **Halaman Releases**: [GitHub Releases v1.0.0](https://github.com/GUPRON98/Bounce-Android/releases/tag/v1.0.0)

---

## ✨ Fitur Utama

- **🚀 100% Native Android Performance**: Dijalankan langsung pada runtime ART/Dalvik Android native (ukuran APK super ringan ~70 KB).
- **📺 Retro 128x128 Nokia Series 40 Display**: Tampilan pixel-perfect tajam bernuansa ponsel Nokia klasik dengan bingkai casing retro.
- **🎮 Keypad Kontrol Lengkap (Full Nokia Numpad & D-Pad)**:
  - **`2` / UP**: Lompat ke atas.
  - **`4` / LEFT**: Gerak ke kiri.
  - **`6` / RIGHT**: Gerak ke kanan.
  - **`8` / DOWN**: Merunduk.
  - **`5` / Action**: Lompat / Terbang (*Fly Mode*).
  - **`#`**: Kecepatan super (*Super Speed Warp*).
  - **`1` & `3`**: Pintasan ganti level (*Level 1 s/d Level 11*).
  - **`SELECT` (-6)** & **`BACK` (-7)**: Tombol SoftKey menu.
- **🔄 Dual Controller Mode**: Berganti instan antara **Mode Full Numpad 3x4** dan **Mode D-Pad + Red Ball Action**.
- **⚡ Panel Cheat Bawaan (Nokia Cheat Codes)**:
  - 🛡️ **God Mode (`787898`)**: Bola kebal dari duri, lilin, dan musuh.
  - 🚀 **Fly & Float Mode (`787899`)**: Bola melayang bebas di udara saat menekan tombol lompat.
  - ⚡ **Super Speed Warp (`#`)**: Melesat secepat kilat.
  - ⏩ **Level Selector (`1` & `3`)**: Bebas memilih stage level 1 sampai 11.

---

## 🔨 Cara Kompilasi (Build Mandiri di Termux / Linux)

Cukup jalankan script:
```bash
bash build_apk.sh
```

Hasil kompilasi APK yang siap dipasang akan tersimpan di:
- `/sdcard/ai/Bounce.apk`
- `/sdcard/Bounce.apk`
