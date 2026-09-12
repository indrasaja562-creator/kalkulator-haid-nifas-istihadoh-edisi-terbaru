package com.example.fiqih

data class FiqihNiatItem(
    val title: String,
    val arabicText: String,
    val latinText: String,
    val translation: String,
    val rujukanKitab: String,
    val description: String
)

data class FiqihProhibitionItem(
    val number: Int,
    val title: String,
    val legalStatus: String,
    val explanation: String,
    val reference: String
)

data class KitabOtoritatifInfo(
    val title: String,
    val author: String,
    val era: String,
    val importance: String
)

data class FiqihNifasGuideItem(
    val categoryName: String,
    val kitabReference: String,
    val definition: String,
    val hukumNifasDanIstihadhah: String,
    val kewajibanShalatDanQadha: String
)

data class FiqihHaidGuideItem(
    val categoryName: String,
    val kitabReference: String,
    val definition: String,
    val hukumHaidDanIstihadhah: String,
    val kewajibanShalatDanQadha: String
)

object FiqihGuideData {

    // 5 Golongan Utama + 2 Tambahan dalam Catatan (Uyunul Masa-il Linnisa' & Tuhfatun Niswah)
    val nifasGuideList = listOf(
        FiqihNifasGuideItem(
            categoryName = "1. Mubtadi'ah Mumayyizah fin-Nifas",
            kitabReference = "Kitab Uyunul Masa'il Linnisa' hal. 91 & Tuhfatun Niswah hal. 36",
            definition = "Wanita yang baru pertama kali melahirkan, mengeluarkan darah melebihi batas maksimal 60 hari, serta memiliki variasi darah kuat (merah/kental) dan darah lemah (kuning/keruh) yang memenuhi syarat tamyiz.",
            hukumNifasDanIstihadhah = "Darah kuat dihukumi NIFAS, sedangkan darah lemah dihukumi ISTIHADHAH FIN-NIFAS.",
            kewajibanShalatDanQadha = "Shalat di masa darah kuat gugur. Di masa darah lemah wajib shalat. Jika shalat di masa darah lemah sempat ditinggalkan karena mengira nifas, wajib diqadha seluruhnya."
        ),
        FiqihNifasGuideItem(
            categoryName = "2. Mubtadi'ah Ghairu Mumayyizah fin-Nifas",
            kitabReference = "Kitab Uyunul Masa'il Linnisa' hal. 92 & Tuhfatun Niswah hal. 36-37",
            definition = "Wanita yang baru melahirkan, darah melebihi 60 hari, dan sifat darahnya seragam atau tidak tamyiz.\nTerbagi menjadi 2:\nPoin A: Jika ia belum pernah haid sama sekali (Mubtadi'ah fil-Haid).\nPoin B: Jika ia sudah pernah haid (Mu'tadah fil-Haid).",
            hukumNifasDanIstihadhah = "Nifasnya HANYA MAJJAH (1 tetesan/sekejap). Sisa darahnya:\n- Poin A: 29 hari istihadhah (suci), lalu 1 hari haid, berulang seterusnya.\n- Poin B: Istihadhah berlanjut sesuai jumlah hari suci adat sebelumnya, lalu haid sesuai adat sebelumnya.",
            kewajibanShalatDanQadha = "Peringatan hukum berat: Wajib mandi besar di akhir hari ke-60, lalu WAJIB MENGQADHA SHALAT selama masa istihadhah (59 hari) yang sempat ia tinggalkan!"
        ),
        FiqihNifasGuideItem(
            categoryName = "3. Mu'tadah Mumayyizah fin-Nifas",
            kitabReference = "Kitab Uyunul Masa'il Linnisa' hal. 93 & Tuhfatun Niswah hal. 37",
            definition = "Wanita yang pernah melahirkan sebelumnya, darah melebihi 60 hari, dan memiliki perbedaan darah kuat dan lemah yang memenuhi syarat tamyiz.",
            hukumNifasDanIstihadhah = "Tamyiz mengalahkan adat persalinan sebelumnya! Darah kuat dihukumi NIFAS, dan darah lemah dihukumi ISTIHADHAH FIN-NIFAS.",
            kewajibanShalatDanQadha = "Shalat di masa darah kuat gugur. Shalat di masa darah lemah yang ditinggalkan sebelum hari ke-60 wajib diqadha."
        ),
        FiqihNifasGuideItem(
            categoryName = "4. Mu'tadah Ghairu Mumayyizah Dzakirah fin-Nifas",
            kitabReference = "Kitab Uyunul Masa'il Linnisa' hal. 94 & Tuhfatun Niswah hal. 37-38",
            definition = "Wanita yang pernah melahirkan sebelumnya, darah melebihi 60 hari, darahnya seragam/tanpa tamyiz, dan INGAT kadar durasi hari adat nifas sebelumnya (misal 40 hari).",
            hukumNifasDanIstihadhah = "Nifasnya DIKEMBALIKAN KEPADA ADAT PERSALINAN SEBELUMNYA (misal 40 hari). Darah dari hari ke-41 sampai hari ke-60 dan seterusnya adalah ISTIHADHAH FIN-NIFAS.",
            kewajibanShalatDanQadha = "Shalat dari hari setelah masa adat (hari ke-41) sampai hari ke-60 yang sempat ditinggalkan WAJIB DIQADHA seluruhnya (20 hari shalat fardhu)."
        ),
        FiqihNifasGuideItem(
            categoryName = "5. Mu'tadah Ghairu Mumayyizah Nasiyah (Mutahayyirah Nifas)",
            kitabReference = "Kitab Uyunul Masa'il Linnisa' hal. 94 & Tuhfatun Niswah hal. 38",
            definition = "Wanita yang pernah melahirkan sebelumnya, darah melebihi 60 hari, darahnya seragam, dan LUPA SAMA SEKALI kadar durasi serta waktu adat nifas sebelumnya.",
            hukumNifasDanIstihadhah = "Nifasnya dihukumi seperti Mutahayyirah: mengambil 1 hari (lahzhah) sebagai nifas yang diyakini, selebihnya diperlakukan dengan kaidah IHTIYATH (berhati-hati).",
            kewajibanShalatDanQadha = "Wajib bersuci (mandi jinabat atau wudhu) setiap masuk waktu shalat fardhu, tetap wajib shalat dan puasa, serta mengqadha shalat yang diragukan."
        ),
        // 2 Catatan dalam Kitab Uyunul Masa-il Linnisa' hal. 94 & Tuhfatun Niswah hal. 38:
        FiqihNifasGuideItem(
            categoryName = "Catatan 1: Dzakirah lil-Waqti dunan Qadri fin-Nifas",
            kitabReference = "Kitab Uyunul Masa'il Linnisa' hal. 94 (Tanbih) & Tuhfatun Niswah hal. 38",
            definition = "Wanita yang pernah nifas, ingat kapan waktu/tanggal mulai keluar darah nifas, tetapi LUPA berapa hari kadar durasi adat nifasnya.",
            hukumNifasDanIstihadhah = "Awal waktunya dihukumi nifas yakin selama sekejap (lahzhah / 1 hari), selebihnya sampai hari ke-60 berstatus IHTIYATH.",
            kewajibanShalatDanQadha = "Hari ke-1 shalat gugur. Hari ke-2 sampai ke-60 wajib shalat dengan bersuci tiap waktu shalat atas dasar ihtiyath, dan shalat yang ditinggalkan diqadha."
        ),
        FiqihNifasGuideItem(
            categoryName = "Catatan 2: Dzakirah lil-Qadri dunan Waqti fin-Nifas",
            kitabReference = "Kitab Uyunul Masa'il Linnisa' hal. 94 (Tanbih) & Tuhfatun Niswah hal. 38",
            definition = "Wanita yang pernah nifas, ingat berapa hari kadar adat nifasnya (misal 40 hari), tetapi LUPA tanggal/waktu persis mulainya keluarnya darah setelah melahirkan.",
            hukumNifasDanIstihadhah = "Nifasnya dikembalikan kepada kadar hari adat yang diingat (40 hari) dihitung sejak persalinan, hari selebihnya istihadhah.",
            kewajibanShalatDanQadha = "Shalat selama 40 hari adat gugur. Hari ke-41 sampai hari ke-60 yang kemarin ditinggalkan wajib diqadha seluruhnya."
        ),
        FiqihNifasGuideItem(
            categoryName = "Catatan Tambahan A: Jeda Melahirkan ke Darah Pertama >= 15 Hari",
            kitabReference = "Kitab Uyunul Masa'il Linnisa' hal. 49 & Tuhfatun Niswah hal. 18",
            definition = "Wanita melahirkan namun darah baru keluar setelah berselang 15 hari atau lebih dari saat persalinan.",
            hukumNifasDanIstihadhah = "TIDAK ADA NIFAS (Laisa bi Nifas). Darah tersebut dihukumi darah haid baru jika memenuhi syarat haid, atau istihadhah.",
            kewajibanShalatDanQadha = "Wajib mandi wiladah (karena melahirkan) sejak persalinan. Darah yang keluar sekarang diperlakukan dengan hukum haid/istihadhah biasa."
        ),
        FiqihNifasGuideItem(
            categoryName = "Catatan Tambahan B: Jeda Suci di Tengah Nifas >= 15 Hari",
            kitabReference = "Kitab Uyunul Masa'il Linnisa' hal. 51 & Tuhfatun Niswah hal. 19",
            definition = "Wanita nifas mengalami jeda darah bersih di tengah masa nifas selama 15 hari atau lebih.",
            hukumNifasDanIstihadhah = "Jeda 15 hari MEMUTUS MASA NIFAS secara sah. Darah yang keluar setelah jeda 15 hari tersebut BUKAN NIFAS LAGI, melainkan DARAH HAID BARU.",
            kewajibanShalatDanQadha = "Wajib mandi nifas saat darah pertama berhenti masuk jeda suci. Darah kedua dihukumi siklus haid baru (shalat gugur selama 24 jam - 15 hari)."
        )
    )

    // 7 Golongan Mustahadhah fil-Haid (Uyunul Masa-il Linnisa' Bab IV & Tuhfatun Niswah)
    val haidGuideList = listOf(
        FiqihHaidGuideItem(
            categoryName = "1. Mubtadi'ah Mumayyizah",
            kitabReference = "Kitab Uyunul Masa'il Linnisa' hal. 66 & Tuhfatun Niswah hal. 27",
            definition = "Wanita baru pertama kali haid, darah melampaui 15 hari, dan darahnya memiliki variasi kuat dan lemah yang memenuhi 4 syarat tamyiz.",
            hukumHaidDanIstihadhah = "Darah kuat adalah HAID, darah lemah adalah ISTIHADHAH.",
            kewajibanShalatDanQadha = "Shalat selama masa darah kuat gugur. Masa darah lemah wajib shalat dan berwudhu istibahah tiap waktu fardhu."
        ),
        FiqihHaidGuideItem(
            categoryName = "2. Mubtadi'ah Ghairu Mumayyizah",
            kitabReference = "Kitab Uyunul Masa'il Linnisa' hal. 70 & Tuhfatun Niswah hal. 29",
            definition = "Wanita baru pertama kali haid, darah melampaui 15 hari, dan darahnya seragam satu warna atau tidak memenuhi syarat tamyiz.",
            hukumHaidDanIstihadhah = "Haidnya HANYA 1 HARI 1 MALAM (24 jam), dan sucinya 29 hari.",
            kewajibanShalatDanQadha = "Wajib mandi jinabat di hari ke-15, lalu WAJIB MENGQADHA SHALAT 14 HARI yang sempat ia tinggalkan!"
        ),
        FiqihHaidGuideItem(
            categoryName = "3. Mu'tadah Mumayyizah",
            kitabReference = "Kitab Uyunul Masa'il Linnisa' hal. 72 & Tuhfatun Niswah hal. 30",
            definition = "Wanita pernah haid sebelumnya, darah melampaui 15 hari, dan darahnya memenuhi 4 syarat tamyiz.",
            hukumHaidDanIstihadhah = "Tamyiz mengalahkan adat! Darah kuat adalah HAID, darah lemah adalah ISTIHADHAH.",
            kewajibanShalatDanQadha = "Shalat di masa darah kuat gugur. Shalat di masa darah lemah wajib dikerjakan/diqadha jika sempat ditinggalkan."
        ),
        FiqihHaidGuideItem(
            categoryName = "4. Mu'tadah Ghairu Mumayyizah (Ingat Lengkap)",
            kitabReference = "Kitab Uyunul Masa'il Linnisa' hal. 73 & Tuhfatun Niswah hal. 30",
            definition = "Wanita pernah haid sebelumnya, darah satu warna, dan ingat kadar durasi hari serta waktu mulainya siklus haid bulan lalu.",
            hukumHaidDanIstihadhah = "Haidnya DIKEMBALIKAN KE ADAT BULAN LALU. Darah setelah hari adat sampai hari ke-15 adalah ISTIHADHAH.",
            kewajibanShalatDanQadha = "Shalat dari hari setelah masa adat sampai hari ke-15 yang sempat ditinggalkan WAJIB DIQADHA."
        ),
        FiqihHaidGuideItem(
            categoryName = "5. Mu'tadah Nasiyah (Mutahayyirah Mahdhah)",
            kitabReference = "Kitab Uyunul Masa'il Linnisa' hal. 84 & Tuhfatun Niswah hal. 33",
            definition = "Wanita pernah haid, darah melampaui 15 hari, satu warna, dan LUPA SAMA SEKALI kadar durasi serta waktu mulainya haid.",
            hukumHaidDanIstihadhah = "Berstatus Mutahayyirah Mahdhah (kebingungan murni). Wajib IHTIYATH (berhati-hati).",
            kewajibanShalatDanQadha = "Wajib mandi atau wudhu setiap masuk waktu shalat fardhu, tetap wajib shalat dan puasa, dilarang jima'."
        ),
        FiqihHaidGuideItem(
            categoryName = "6. Mu'tadah Dzakirah lil-Waqti dunan Qadr",
            kitabReference = "Kitab Uyunul Masa'il Linnisa' hal. 80 & Tuhfatun Niswah hal. 32",
            definition = "Wanita pernah haid, ingat waktu/tanggal mulainya haid, tetapi lupa berapa hari durasi adatnya.",
            hukumHaidDanIstihadhah = "24 jam pertama dari waktu mulai adalah haid yakin. Hari ke-2 s/d ke-15 berstatus ihtiyath.",
            kewajibanShalatDanQadha = "Hari ke-1 shalat gugur. Hari ke-2 s/d ke-15 wajib shalat atas dasar ihtiyath dan mandi pada waktu yang dimungkinkan putusnya darah."
        ),
        FiqihHaidGuideItem(
            categoryName = "7. Mu'tadah Dzakirah lil-Qadri dunan Waqt",
            kitabReference = "Kitab Uyunul Masa'il Linnisa' hal. 82 & Tuhfatun Niswah hal. 32",
            definition = "Wanita pernah haid, ingat berapa hari durasi haidnya (misal 7 hari), tetapi lupa tanggal mulainya haid.",
            hukumHaidDanIstihadhah = "Kadar 7 hari diyakini sebagai ukuran haid, namun harinya diragukan sehingga berlaku hukum ihtiyath.",
            kewajibanShalatDanQadha = "Wajib berhati-hati sepanjang rentang 15 hari pertama dengan tetap menunaikan shalat."
        ),
        FiqihHaidGuideItem(
            categoryName = "Istihadhah Takmilatan lit-Tuhri",
            kitabReference = "Kitab Tuhfatun Niswah",
            definition = "Kondisi di mana darah kembali keluar SEBELUM masa suci mencapai batas minimal (15 hari 15 malam).",
            hukumHaidDanIstihadhah = "Darah tersebut berstatus ISTIHADHAH (penyempurna masa suci) sampai batas genap 15 hari dari selesainya haid sebelumnya.",
            kewajibanShalatDanQadha = "Shalat WAJIB dikerjakan (dengan tata cara mustahadhah: wudhu tiap masuk waktu shalat fardhu). Jika ditinggalkan wajib diqadha."
        )
    )

    val niatList = listOf(
        FiqihNiatItem(
            title = "Niat Mandi Wajib Haid",
            arabicText = "نَوَيْتُ الْغُسْلَ لِرَفْعِ الْحَدَثِ الْأَكْبَرِ مِنَ الْحَيْضِ فَرْضًا لِلَّهِ تَعَالَى",
            latinText = "Nawaitul ghusla liraf'il hadatsil akbari minal haidhi fardhan lillahi ta'ala.",
            translation = "Aku berniat mandi untuk menghilangkan hadats besar dari haid, fardhu karena Allah Ta'ala.",
            rujukanKitab = "Fathul Qarib & Kasyifatus Saja hal. 115",
            description = "Dibaca dalam hati saat awal mula menyiramkan air ke anggota tubuh pertama (bersamaan basuhan pertama)."
        ),
        FiqihNiatItem(
            title = "Niat Mandi Wajib Nifas",
            arabicText = "نَوَيْتُ الْغُسْلَ لِرَفْعِ الْحَدَثِ الْأَكْبَرِ مِنَ النِّفَاسِ فَرْضًا لِلَّهِ تَعَالَى",
            latinText = "Nawaitul ghusla liraf'il hadatsil akbari minan nifasi fardhan lillahi ta'ala.",
            translation = "Aku berniat mandi untuk menghilangkan hadats besar dari nifas, fardhu karena Allah Ta'ala.",
            rujukanKitab = "Al-Bajuri Juz 1 hal. 112 & Tuhfatun Niswah hal. 20",
            description = "Dilakukan setelah darah nifas benar-benar bersih dan tuntas (maksimal 60 hari atau sesuai adat)."
        ),
        FiqihNiatItem(
            title = "Niat Mandi Wiladah (Melahirkan)",
            arabicText = "نَوَيْتُ الْغُسْلَ لِرَفْعِ حَدَثِ الْوِلَادَةِ فَرْضًا لِلَّهِ تَعَالَى",
            latinText = "Nawaitul ghusla liraf'i hadatsil wiladati fardhan lillahi ta'ala.",
            translation = "Aku berniat mandi untuk menghilangkan hadats melahirkan (wiladah), fardhu karena Allah Ta'ala.",
            rujukanKitab = "Al-Umm & Al-Majmu' Syarah al-Muhadzdzab",
            description = "Wajib bagi wanita yang melahirkan tanpa keluar darah nifas. Jika keluar nifas, cukup mandi nifas setelah nifas selesai."
        ),
        FiqihNiatItem(
            title = "Niat Wudhu Istibahah Mustahadhah",
            arabicText = "نَوَيْتُ الْوُضُوْءَ لِاسْتِبَاحَةِ الصَّلَاةِ فَرْضًا لِلَّهِ تَعَالَى",
            latinText = "Nawaitul wudhu'a li-istibaahatish sholaati fardhan lillahi ta'ala.",
            translation = "Aku berniat wudhu untuk diperbolehkan melaksanakan shalat, fardhu karena Allah Ta'ala.",
            rujukanKitab = "Bujairami 'alal Minhaj & Uyunul Masa'il hal. 99",
            description = "PENTING: Mustahadhah berstatus da'imul hadats, sehingga niat wudhunya BUKAN menghilangkan hadats, melainkan 'li-istibaahatish shalah' (agar diperbolehkan shalat), dan wajib berwudhu SETELAH MASUK WAKTU shalat."
        ),
        FiqihNiatItem(
            title = "Niat Puasa Qadha Ramadhan",
            arabicText = "نَوَيْتُ صَوْمَ غَدٍ عَنْ قَضَاءِ فَرْضِ شَهْرِ رَمَضَانَ لِلَّهِ تَعَالَى",
            latinText = "Nawaitu shauma ghadin 'an qadha'i fardhi syahri Ramadhana lillahi ta'ala.",
            translation = "Aku berniat puasa esok hari untuk mengqadha fardhu bulan Ramadhan karena Allah Ta'ala.",
            rujukanKitab = "Fathul Mu'in hal. 58",
            description = "Wajib dibaca di malam hari (tabyit an-niyyah) sebelum terbit fajar."
        )
    )

    val prohibitions = listOf(
        FiqihProhibitionItem(1, "Shalat Fardhu & Sunnah", "Haram Mutlak", "Tidak sah dan haram dikerjakan. Shalat selama masa haid/nifas sah gugur dan tidak diqadha.", "HR. Bukhari no. 314 & Fathul Qarib"),
        FiqihProhibitionItem(2, "Puasa Wajib & Sunnah", "Haram Mutlak", "Haram berpuasa, namun hari puasa fardhu (Ramadhan) yang tertinggal WAJIB diqadha.", "HR. Muslim no. 335 & Kasyifatus Saja"),
        FiqihProhibitionItem(3, "Thawaf di Baitullah", "Haram Mutlak", "Thawaf wadha', ifadhah, maupun sunnah tidak sah bagi wanita berhadats besar.", "HR. Bukhari no. 294"),
        FiqihProhibitionItem(4, "Menyentuh & Membawa Mushaf", "Haram", "Haram menyentuh kertas, tulisan, maupun sampul mushaf Al-Qur'an.", "QS. Al-Waqi'ah: 79 & I'anatuth Thalibin"),
        FiqihProhibitionItem(5, "Membaca Al-Qur'an", "Haram (Kecuali Zikir)", "Haram melafadzkan ayat dengan niat qiro'ah. Boleh jika diniatkan doa, zikir, atau muraja'ah bagi hafidzah.", "Hasyiyah al-Bajuri Juz 1"),
        FiqihProhibitionItem(6, "Berdiam Diri di Masjid", "Haram", "Tidak boleh i'tikaf atau berdiam diri di dalam masjid.", "HR. Abu Daud no. 232"),
        FiqihProhibitionItem(7, "Melintas Masjid (Khawatir Najis)", "Haram / Makruh", "Haram bila khawatir darah menetes mengotori masjid; makruh jika aman.", "Nihayatul Muhtaj Juz 1"),
        FiqihProhibitionItem(8, "Bersetubuh (Jima')", "Dosa Besar", "Haram mutlak bersenggama sampai darah berhenti dan mandi wajib selesai.", "QS. Al-Baqarah: 222 & Al-Muhadzdzab"),
        FiqihProhibitionItem(9, "Istirja' Antara Pusar & Lutut", "Haram Tanpa Penghalang", "Menikmati bagian tubuh antara pusar dan lutut tanpa tabir.", "Bulughul Maram no. 31"),
        FiqihProhibitionItem(10, "Mentalak Istri saat Haid", "Haram (Talak Bid'i)", "Haram bagi suami menjatuhkan talak saat istri haid, namun talaknya tetap jatuh.", "Fathul Mu'in"),
        FiqihProhibitionItem(11, "Sujud Tilawah & Sujud Syukur", "Haram", "Karena syarat sahnya sama dengan syarat shalat yaitu suci dari hadats.", "Uyunul Masa-il Linnisa' hal. 56")
    )

    val kitabReferences = listOf(
        KitabOtoritatifInfo("Uyunul Masa-il Linnisa'", "LBM-PPL Pondok Pesantren Lirboyo Kediri", "Klasik Kontemporer", "Rujukan primer pesantren Jawa Timur untuk studi komprehensif darah wanita"),
        KitabOtoritatifInfo("Tuhfatun Niswah", "Agus Sholah (Ibnussama)", "2023", "Panduan praktis berdalil fiqih Syafi'i dengan ibarat kitab kuning"),
        KitabOtoritatifInfo("Tuhfatul Muhtaj bi Syarh al-Minhaj", "Imam Ibnu Hajar al-Haitami (w. 974 H)", "Klasik Mazhab Syafi'i", "Kitab fatwa mu'tamad tertinggi di wilayah Hijaz dan Nusantara"),
        KitabOtoritatifInfo("Nihayatul Muhtaj ila Syarh al-Minhaj", "Imam Syamsuddin ar-Ramli (w. 1004 H)", "Klasik Mazhab Syafi'i", "Kitab rujukan mu'tamad ulama Mesir dan Nusantara"),
        KitabOtoritatifInfo("Al-Majmu' Syarah al-Muhadzdzab", "Imam Abu Zakariya an-Nawawi (w. 676 H)", "Ensiklopedi Fiqih Akbar", "Kitab masterpiece komparasi dalil hadits dan ketetapan mazhab Syafi'i"),
        KitabOtoritatifInfo("Hasyiyah al-Bajuri 'ala Ibn Qasim", "Syaikh Ibrahim al-Bajuri (w. 1276 H)", "Syarah Klasik", "Rujukan standar rukun dan syarat bersuci di madrasah dan pesantren"),
        KitabOtoritatifInfo("Bughyatul Mustarsyidin", "Sayyid Abdurrahman bin Muhammad Ba'alawi", "Klasik Hadramaut", "Kumpulan fatwa mufti Hadramaut mengenai kaidah sahbi, qadha, dan suci"),
        KitabOtoritatifInfo("Fathul Mu'in & I'anatuth Thalibin", "Syaikh Zainuddin al-Malibari & Sayyid Bakri Syatha", "Madrasah Klasik", "Pegangan kajian hukum shalat, bersuci, dan qadha shalat wanita haid")
    )
}
