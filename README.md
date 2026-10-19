<div align="center">

<img src="app/src/main/res/drawable/logo_bambunet.png" alt="Logo BambuNet" width="150" />

# 🐼 BambuNet — Pro Telecom & Wi-Fi Analyzer
### **An Advanced Native Android Spectrum, Network Diagnostic, & Drive Test Tool**

![Kotlin](https://img.shields.io/badge/Kotlin-100%25-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20(Material%203)-0F172A?style=for-the-badge&logo=jetpackcompose&logoColor=white)
![Map OSM](https://img.shields.io/badge/Maps-OpenStreetMap%20(Leaflet.js)-06B6D4?style=for-the-badge&logo=openstreetmap&logoColor=white)
![RF Engine](https://img.shields.io/badge/Physics-Link%20Budget%20%26%20Fresnel-0D9488?style=for-the-badge&logo=physics&logoColor=white)

**BambuNet** adalah aplikasi penganalisis jaringan, pemetaan spektrum nirkabel, dan pengumpulan telemetri seluler (*Drive Test*) native Android yang dirancang khusus untuk praktisi telekomunikasi, administrator jaringan, dan peneliti lapangan (*indoor/outdoor*). Aplikasi ini membawa identitas visual instrumen telekomunikasi modern dengan tema *Cyber Teal, Electric Cyan, dan Titanium Slate*.

</div>

---

## 📸 Tampilan Utama (Features Preview)

| Spektrum RF & AP Scanner | Grafik Overlapping | Traceroute & Port Scan | Live Drive Test Map | Radar Grid BTS (Offline) | Logger & RF Calc |
| :---: | :---: | :---: | :---: | :---: | :---: |
| *(Tarik SS 1)* | *(Tarik SS 2)* | *(Tarik SS 3)* | *(Tarik SS 4)* | *(Tarik SS 5)* | *(Tarik SS 6)* |

---

## ✨ Fitur-Fitur "Super Telecom Analyzer" (Core Features)

### 1. 📡 RF Spectrum & Wi-Fi Inspector
* **Pemindai Frekuensi Real-Time:** Menangkap data SSID, BSSID, kekuatan sinyal (dBm), lebar pita (bandwidth MHz), standar IEEE (Wi-Fi 4/5/6/7), keamanan (WPA2/WPA3), dan identitas vendor.
* **Kalibrasi Jarak Fisika:** Menggunakan rumus *Log-Distance Path Loss Model* dengan eksponen yang dapat diubah untuk melacak estimasi jarak ke router berdasarkan kondisi lingkungan lapangan (LOS, Lab, Dinding Beton).
* **Rekomendasi Kanal Pintar:** Algoritma otomatis yang menghitung skor interferensi kanal-kanal 2.4 GHz di sekitar dan merekomendasikan kanal terbaik yang paling aman dari tumpang tindih (*co-channel interference*).

### 2. 📈 Spektrum Kurva Parabola Channel
* Visualisasi grafik spektogram parabola untuk kanal 1-14 di frekuensi 2.4 GHz, 5 GHz, dan 6 GHz secara langsung, membantu mengidentifikasi tabrakan kanal (*overlapping*) dengan jelas.

### 3. 🚗 G-Net Drive Test & Cellular Telemetry (Online & Offline Map)
* **Drive Test Recording:** Merekam jejak rute perjalanan sambil memantau parameter jaringan seluler asli secara dinamis (seperti **RSRP, RSRQ, SINR, PCI, TAC, Operator, dan Tipe Jaringan**).
* **OpenStreetMap & CartoDB Maps:** Merender visualisasi titik dan rute perjalanan berwarna (menandakan status dBm) lengkap dengan posisi menara BTS sekitar (*Spider Line Tracker*).
* **Radar Grid BTS Offline:** Menggunakan visualisasi *Native Canvas* sebagai alternatif darurat apabila Anda berada di pelosok yang tidak memiliki akses internet (100% tanpa API Key, murni koordinat GPS dan radar math).
* **Test Drive Simulator:** Tombol uji coba simulasi gerak untuk mengetes fungsionalitas visualisasi rute tanpa harus mengemudi di dunia nyata.
* **Export .CSV & .KML:** Ekspor dataset perjalanan ke format CSV (untuk analisis Excel/Python Pandas) atau KML (untuk visualisasi 3D di Google Earth Pro / QGIS).

### 4. ⚡ NOC Network Diagnostic (Ping & Jitter Analyzer)
* **RTT & Jitter Analyzer:** Mengukur latency minimal, maksimal, rata-rata (ms), serta dispersi keterlambatan paket (*jitter*) dengan tampilan log konsol real-time.
* **TCP Service & IoT Port Scanner:** Melakukan pemindaian cepat terhadap port terbuka seperti SSH (22), HTTP (80), HTTPS (443), MQTT (1883/8883), MySQL (3306), dan API Web (8080) pada server tujuan.

### 5. 📍 Indoor Survey & RSSI Fingerprinting Logger
* Khusus riset sistem navigasi dalam ruangan! Rekam snapshot kekuatan sinyal semua AP di sekitar pada koordinat target `(X, Y)` dan unduh sebagai berkas dataset `.CSV` dalam hitungan detik.

### 6. 🧮 RF & Network Engineering Suite (Kalkulator Lapangan)
* **Link Budget Calculator:** Menghitung daya terima Rx ($P_{rx}$), FSPL, dan EIRP pemancar berdasarkan loss kabel, redaman, dan gain antena.
* **Fresnel Zone Radius:** Mengetahui batas ruang bebas hambatan (Zona Fresnel ke-1) untuk pemasangan antena *point-to-point* nirkabel jarak jauh.
* **Kalkulator Subnet IPv4:** Analisis CIDR prefix (/8 s/d /32) untuk menghitung subnet mask, wildcard mask, dan jumlah host yang tersedia.

---

## 🛠️ Stack Teknologi & Dependensi

* **Sistem & UI:** Kotlin, Jetpack Compose, Material Design 3 (Material Icon Extended).
* **Telephony & Jaringan:** `TelephonyManager`, `CellInfoLte`, `CellInfoNr`, dan `LocationManager` GPS.
* **Pemetaan & Web:** Android WebView, Leaflet.js, OpenStreetMap & CartoDB, HTML5/CSS3.
* **Engine Fisika & Matematika:** Custom trigonometric telemetry, FSPL, Fresnel formulas, IP masking.
* **Perpustakaan Ekspor:** `FileProvider` & cache sharing API.

---

## 👨‍💻 Kontributor

Dikembangkan oleh **Muhammad Saputra**  
🏢 **Founder of PandaKode** *(Pekonbaru, Riau, Indonesia)*
