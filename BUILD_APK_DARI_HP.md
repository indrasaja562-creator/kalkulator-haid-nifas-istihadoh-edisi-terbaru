# Panduan Praktis: Build APK Sendiri Langsung dari HP (via GitHub Actions)

Panduan ini memungkinkan Anda untuk meng-compile file APK terbaru kapan saja secara otomatis **langsung dari HP Anda** tanpa memerlukan laptop atau komputer.

---

## 📱 Langkah-langkah Build APK dari HP:

### 1. Push / Upload Kode ke GitHub
1. Pastikan seluruh project ini sudah Anda hubungkan atau push ke repository GitHub Anda (bisa lewat tombol **Export to GitHub** di menu AI Studio atau git push).

### 2. Buka GitHub dari Browser HP
1. Buka browser di HP Anda (Chrome, Brave, atau browser favorit Anda).
2. Masuk ke halaman repository project Anda di [github.com](https://github.com).

### 3. Jalankan Workflow Build APK
1. Di halaman repository Anda, klik tab **Actions** (jika menggunakan tampilan mobile browser, Anda bisa mengaktifkan mode *"Situs Desktop"* jika tab Actions tidak terlihat).
2. Di kolom sebelah kiri, pilih workflow **Build Android APK**.
3. Klik tombol **Run workflow** -> pilih branch `main` atau `master` -> klik tombol hijau **Run workflow**.

### 4. Unduh File APK ke HP
1. Tunggu proses build berjalan selama sekitar 2–4 menit hingga muncul tanda centang hijau (Success).
2. Klik pada proses workflow yang baru selesai tersebut.
3. Gulir ke bagian paling bawah ke bagian **Artifacts**.
4. Klik pada nama artifact: **Kalkulator-Haid-Nifas-APK**.
5. File `.zip` berisi APK akan otomatis terunduh ke HP Anda.
6. Ekstrak zip tersebut di HP Anda, lalu instal file `app-debug.apk` seperti biasa.

---

## ⚙️ Fitur Workflow Otomatis:
- Setiap kali Anda melakukan push atau commit perubahan ke branch utama (`main`/`master`), GitHub Actions akan **otomatis mem-build APK baru**.
- Anda juga bisa memicu build secara manual kapan pun diinginkan lewat tombol **Run workflow**.
- Keystore debug ditangani secara otomatis sehingga APK siap langsung diinstal dan ditimpa ke instalasi sebelumnya.
