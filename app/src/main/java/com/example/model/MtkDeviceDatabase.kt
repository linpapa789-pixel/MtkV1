package com.example.model

data class MtkDeviceModel(
    val modelName: String,
    val chipset: String,
    val chipCode: String,
    val bromInstruction: String
)

data class MtkBrand(
    val brandName: String,
    val iconName: String,
    val models: List<MtkDeviceModel>
)

object MtkDeviceDatabase {

    val brands: List<MtkBrand> = listOf(
        MtkBrand(
            brandName = "Auto Detect (Generic)",
            iconName = "generic",
            models = listOf(
                MtkDeviceModel("Auto Detect (Universal)", "Auto (Helio/Dimensity)", "MT6765", "Hold Vol+ & Vol- -> Insert USB"),
                MtkDeviceModel("Helio A22 (MT6761)", "MT6761 (Helio A22)", "MT6761", "Hold Vol+ & Vol- -> Insert USB"),
                MtkDeviceModel("Helio P22 (MT6762)", "MT6762 (Helio P22)", "MT6762", "Hold Vol+ & Vol- -> Insert USB"),
                MtkDeviceModel("Helio P35 / G25 / G35 (MT6765)", "MT6765 (P35/G35)", "MT6765", "Hold Vol+ & Vol- -> Insert USB"),
                MtkDeviceModel("Helio G70 / G80 / G85 (MT6768)", "MT6768 (G80/G85)", "MT6768", "Hold Vol+ & Vol- -> Insert USB"),
                MtkDeviceModel("Helio P60 / P70 (MT6771)", "MT6771 (P60/P70)", "MT6771", "Hold Vol+ & Vol- / TestPoint"),
                MtkDeviceModel("Helio P90 / P95 (MT6779)", "MT6779 (P90/P95)", "MT6779", "Hold Vol+ & Vol- / TestPoint"),
                MtkDeviceModel("Helio G96 (MT6781)", "MT6781 (Helio G96)", "MT6781", "Hold Vol+ & Vol- -> Insert USB"),
                MtkDeviceModel("Helio G90T / G95 (MT6785)", "MT6785 (G90T/G95)", "MT6785", "Hold Vol+ & Vol- -> Insert USB"),
                MtkDeviceModel("Helio G99 / G99-Ultra (MT6789)", "MT6789 (Helio G99)", "MT6789", "Hold Vol+ & Vol- -> Insert USB"),
                MtkDeviceModel("Dimensity 700 / 6020 (MT6833)", "MT6833 (Dimensity 700)", "MT6833", "Hold Vol+ & Vol- -> Insert USB"),
                MtkDeviceModel("Dimensity 900 / 1080 / 7050 (MT6877)", "MT6877 (Dimensity 900/1080)", "MT6877", "Hold Vol+ & Vol- -> Insert USB"),
                MtkDeviceModel("Dimensity 1000 / 1200 / 1300 (MT6893)", "MT6893 (Dimensity 1200)", "MT6893", "Hold Vol+ & Vol- -> Insert USB"),
                MtkDeviceModel("Dimensity 8100 / 8200 / 8300 (MT6895)", "MT6895 (Dimensity 8100/8200)", "MT6895", "Hold Vol+ & Vol- -> Insert USB"),
                MtkDeviceModel("Dimensity 9000 / 9200 / 9300 (MT6983)", "MT6983 (Dimensity 9000/9200)", "MT6983", "Hold Vol+ & Vol- -> Insert USB"),
                MtkDeviceModel("MT6580 / MT6572 / MT6582", "MT6580 (Legacy 32-bit)", "MT6580", "Hold Vol Down -> Insert USB"),
                MtkDeviceModel("MT6735 / MT6737 / MT6739", "MT6739 (Legacy 64-bit)", "MT6739", "Hold Vol+ & Vol- -> Insert USB")
            )
        ),
        MtkBrand(
            brandName = "Samsung",
            iconName = "samsung",
            models = listOf(
                MtkDeviceModel("Galaxy A01 Core (SM-A013F/G)", "MT6739 (Quad-Core)", "MT6739", "Hold Vol+ & Vol- -> Insert USB"),
                MtkDeviceModel("Galaxy A02 (SM-A022F/M)", "MT6739W (Quad-Core)", "MT6739", "Hold Vol+ & Vol- -> Insert USB"),
                MtkDeviceModel("Galaxy A03s (SM-A037F/G/M)", "MT6765 (Helio P35)", "MT6765", "Hold Vol+ & Vol- -> Insert USB (or TestPoint)"),
                MtkDeviceModel("Galaxy A04 (SM-A045F/M)", "MT6765 (Helio P35)", "MT6765", "Hold Vol+ & Vol- -> Insert USB"),
                MtkDeviceModel("Galaxy A04e (SM-A042F/M)", "MT6765 (Helio P35)", "MT6765", "Hold Vol+ & Vol- -> Insert USB"),
                MtkDeviceModel("Galaxy A13 5G (SM-A136U/W/B)", "MT6833 (Dimensity 700)", "MT6833", "Hold Vol+ & Vol- -> Insert USB"),
                MtkDeviceModel("Galaxy A14 5G (SM-A146P/U)", "MT6833 (Dimensity 700)", "MT6833", "Hold Vol+ & Vol- -> Insert USB"),
                MtkDeviceModel("Galaxy A22 5G (SM-A226B)", "MT6833 (Dimensity 700)", "MT6833", "Hold Vol+ & Vol- -> Insert USB"),
                MtkDeviceModel("Galaxy A24 4G (SM-A245F/M)", "MT6789 (Helio G99)", "MT6789", "Hold Vol+ & Vol- -> Insert USB"),
                MtkDeviceModel("Galaxy A32 4G (SM-A325F/M)", "MT6769 (Helio G80)", "MT6768", "Hold Vol+ & Vol- -> Insert USB"),
                MtkDeviceModel("Galaxy M01s (SM-M017F)", "MT6762 (Helio P22)", "MT6762", "Hold Vol+ & Vol- -> Insert USB"),
                MtkDeviceModel("Galaxy M02 (SM-M022F/M)", "MT6739W", "MT6739", "Hold Vol+ & Vol- -> Insert USB"),
                MtkDeviceModel("Galaxy M32 (SM-M325F)", "MT6769 (Helio G80)", "MT6768", "Hold Vol+ & Vol- -> Insert USB"),
                MtkDeviceModel("Galaxy Tab A7 Lite (SM-T220/T225)", "MT6765 (Helio P22T)", "MT6765", "Hold Vol+ & Vol- -> Insert USB")
            )
        ),
        MtkBrand(
            brandName = "Xiaomi / Redmi / Poco",
            iconName = "xiaomi",
            models = listOf(
                MtkDeviceModel("Redmi 6A (cactus)", "MT6761 (Helio A22)", "MT6761", "Hold Vol+ & Vol- -> Insert USB"),
                MtkDeviceModel("Redmi 6 (cereus)", "MT6762 (Helio P22)", "MT6762", "Hold Vol+ & Vol- -> Insert USB"),
                MtkDeviceModel("Redmi 9A / 9i (dandelion)", "MT6765 (Helio G25)", "MT6765", "Hold Vol+ & Vol- -> Insert USB"),
                MtkDeviceModel("Redmi 9C / 9C NFC (angelica)", "MT6765 (Helio G35)", "MT6765", "Hold Vol+ & Vol- -> Insert USB"),
                MtkDeviceModel("Redmi 10A (sunstone)", "MT6765 (Helio G25)", "MT6765", "Hold Vol+ & Vol- -> Insert USB"),
                MtkDeviceModel("Poco C3 / C31 (angelicain)", "MT6765 (Helio G35)", "MT6765", "Hold Vol+ & Vol- -> Insert USB"),
                MtkDeviceModel("Poco C51 / C50", "MT6765 (Helio G36)", "MT6765", "Hold Vol+ & Vol- -> Insert USB"),
                MtkDeviceModel("Redmi 9 / 9 Prime (lancelot)", "MT6768 (Helio G80)", "MT6768", "Hold Vol+ & Vol- -> Insert USB"),
                MtkDeviceModel("Redmi Note 9 (merlin)", "MT6768 (Helio G85)", "MT6768", "Hold Vol+ & Vol- -> Insert USB"),
                MtkDeviceModel("Redmi 12C / Poco C55 (earth)", "MT6768 (Helio G85)", "MT6768", "Hold Vol+ & Vol- -> Insert USB"),
                MtkDeviceModel("Redmi 13C / Poco C65 (gale)", "MT6768 (Helio G85)", "MT6768", "Hold Vol+ & Vol- -> Insert USB"),
                MtkDeviceModel("Poco M2 (shiva)", "MT6768 (Helio G80)", "MT6768", "Hold Vol+ & Vol- -> Insert USB"),
                MtkDeviceModel("Redmi Note 8 Pro (begonia)", "MT6785 (Helio G90T)", "MT6785", "Hold Vol+ & Vol- -> Insert USB"),
                MtkDeviceModel("Redmi Note 10S (rosemary)", "MT6785 (Helio G95)", "MT6785", "Hold Vol+ & Vol- -> Insert USB"),
                MtkDeviceModel("Redmi Note 11S / Poco M4 Pro (fleur)", "MT6781 (Helio G96)", "MT6781", "Hold Vol+ & Vol- -> Insert USB"),
                MtkDeviceModel("Redmi Note 11 Pro 4G (viva)", "MT6781 (Helio G96)", "MT6781", "Hold Vol+ & Vol- -> Insert USB"),
                MtkDeviceModel("Redmi Note 12S (sea)", "MT6781 (Helio G96)", "MT6781", "Hold Vol+ & Vol- -> Insert USB"),
                MtkDeviceModel("Redmi Note 13 Pro 4G (emerald)", "MT6789 (Helio G99-Ultra)", "MT6789", "Hold Vol+ & Vol- -> Insert USB"),
                MtkDeviceModel("Poco M5 (rock)", "MT6789 (Helio G99)", "MT6789", "Hold Vol+ & Vol- -> Insert USB"),
                MtkDeviceModel("Redmi Note 10 5G (camellian)", "MT6833 (Dimensity 700)", "MT6833", "Hold Vol+ & Vol- -> Insert USB"),
                MtkDeviceModel("Poco M3 Pro 5G (camellian)", "MT6833 (Dimensity 700)", "MT6833", "Hold Vol+ & Vol- -> Insert USB"),
                MtkDeviceModel("Redmi Note 11T 5G (evergo)", "MT6833P (Dimensity 810)", "MT6833", "Hold Vol+ & Vol- -> Insert USB"),
                MtkDeviceModel("Redmi Note 12 Pro 5G (ruby)", "MT6877 (Dimensity 1080)", "MT6877", "Hold Vol+ & Vol- -> Insert USB"),
                MtkDeviceModel("Redmi Note 13 Pro+ 5G (zircon)", "MT6897 (Dimensity 7200-Ultra)", "MT6895", "Hold Vol+ & Vol- -> Insert USB"),
                MtkDeviceModel("Xiaomi 11T (agate)", "MT6893 (Dimensity 1200)", "MT6893", "Hold Vol+ & Vol- -> Insert USB"),
                MtkDeviceModel("Poco F3 GT (ares)", "MT6893 (Dimensity 1200)", "MT6893", "Hold Vol+ & Vol- -> Insert USB"),
                MtkDeviceModel("Xiaomi 12T (plato)", "MT6895 (Dimensity 8100-Ultra)", "MT6895", "Hold Vol+ & Vol- -> Insert USB"),
                MtkDeviceModel("Xiaomi 13T (aristotle)", "MT6897 (Dimensity 8200-Ultra)", "MT6895", "Hold Vol+ & Vol- -> Insert USB")
            )
        ),
        MtkBrand(
            brandName = "Realme",
            iconName = "realme",
            models = listOf(
                MtkDeviceModel("Realme C2 (RMX1941)", "MT6762 (Helio P22)", "MT6762", "Hold Vol+ & Vol- -> Insert USB"),
                MtkDeviceModel("Realme C11 (RMX2185)", "MT6765 (Helio G35)", "MT6765", "Hold Vol+ & Vol- -> Insert USB"),
                MtkDeviceModel("Realme C12 (RMX2189)", "MT6765 (Helio G35)", "MT6765", "Hold Vol+ & Vol- -> Insert USB"),
                MtkDeviceModel("Realme C15 (RMX2180)", "MT6765 (Helio G35)", "MT6765", "Hold Vol+ & Vol- -> Insert USB"),
                MtkDeviceModel("Realme C20 / C21 (RMX3063)", "MT6765 (Helio G35)", "MT6765", "Hold Vol+ & Vol- -> Insert USB"),
                MtkDeviceModel("Realme C25 / C25s (RMX3193)", "MT6769 (Helio G70/G85)", "MT6768", "Hold Vol+ & Vol- -> Insert USB"),
                MtkDeviceModel("Realme C55 / Narzo N55 (RMX3710)", "MT6769 (Helio G88)", "MT6768", "Hold Vol+ & Vol- -> Insert USB"),
                MtkDeviceModel("Realme 6 / 6s (RMX2001)", "MT6785 (Helio G90T)", "MT6785", "Hold Vol+ & Vol- -> Insert USB"),
                MtkDeviceModel("Realme 7 / Narzo 20 Pro (RMX2151)", "MT6785 (Helio G95)", "MT6785", "Hold Vol+ & Vol- -> Insert USB"),
                MtkDeviceModel("Realme 8 4G (RMX3085)", "MT6785 (Helio G95)", "MT6785", "Hold Vol+ & Vol- -> Insert USB"),
                MtkDeviceModel("Realme 8 5G / Narzo 30 5G (RMX3241)", "MT6833 (Dimensity 700)", "MT6833", "Hold Vol+ & Vol- -> Insert USB"),
                MtkDeviceModel("Realme 10 4G (RMX3630)", "MT6789 (Helio G99)", "MT6789", "Hold Vol+ & Vol- -> Insert USB"),
                MtkDeviceModel("Realme 11 5G / 11x 5G (RMX3780)", "MT6835 (Dimensity 6100+)", "MT6833", "Hold Vol+ & Vol- -> Insert USB"),
                MtkDeviceModel("Realme GT Neo / Neo 2T (RMX3031)", "MT6893 (Dimensity 1200)", "MT6893", "Hold Vol+ & Vol- -> Insert USB")
            )
        ),
        MtkBrand(
            brandName = "Oppo",
            iconName = "oppo",
            models = listOf(
                MtkDeviceModel("Oppo A1k / A11k (CPH1923)", "MT6762 (Helio P22)", "MT6762", "Hold Vol+ & Vol- or TestPoint"),
                MtkDeviceModel("Oppo A5s / A12 (CPH1909)", "MT6765 (Helio P35)", "MT6765", "Hold Vol+ & Vol- or TestPoint"),
                MtkDeviceModel("Oppo A15 / A31 (CPH2185)", "MT6765 (Helio P35)", "MT6765", "Hold Vol+ & Vol- or TestPoint"),
                MtkDeviceModel("Oppo A16 / A16k (CPH2269)", "MT6765 (Helio G35)", "MT6765", "TestPoint (CMD to GND)"),
                MtkDeviceModel("Oppo A17 / A17k (CPH2477)", "MT6765 (Helio G35)", "MT6765", "TestPoint (CMD to GND)"),
                MtkDeviceModel("Oppo A58 / A78 4G (CPH2577)", "MT6769 (Helio G85)", "MT6768", "Hold Vol+ & Vol- -> Insert USB"),
                MtkDeviceModel("Oppo F9 / F11 / F11 Pro", "MT6771 (Helio P60/P70)", "MT6771", "Hold Vol+ & Vol- or TestPoint"),
                MtkDeviceModel("Oppo A9 / A9x (CPH1938)", "MT6771 (Helio P70)", "MT6771", "Hold Vol+ & Vol- or TestPoint"),
                MtkDeviceModel("Oppo Reno 2F (CPH1989)", "MT6771 (Helio P70)", "MT6771", "Hold Vol+ & Vol- or TestPoint"),
                MtkDeviceModel("Oppo Reno 2Z / Reno 3 (CPH1979)", "MT6779 (Helio P90)", "MT6779", "Hold Vol+ & Vol- or TestPoint"),
                MtkDeviceModel("Oppo F17 Pro / A93 (CPH2119)", "MT6779 (Helio P95)", "MT6779", "Hold Vol+ & Vol- or TestPoint"),
                MtkDeviceModel("Oppo Reno 6 5G / 7 5G (CPH2251)", "MT6877 (Dimensity 900)", "MT6877", "Hold Vol+ & Vol- or TestPoint"),
                MtkDeviceModel("Oppo Reno 8 5G (CPH2359)", "MT6893 (Dimensity 1300)", "MT6893", "Hold Vol+ & Vol- or TestPoint")
            )
        ),
        MtkBrand(
            brandName = "Vivo",
            iconName = "vivo",
            models = listOf(
                MtkDeviceModel("Vivo Y81 / Y81i / Y83", "MT6762 (Helio P22)", "MT6762", "TestPoint (CMD to GND) or Vol Keys"),
                MtkDeviceModel("Vivo Y91 / Y91c / Y93", "MT6762 (Helio P22)", "MT6762", "TestPoint (CMD to GND)"),
                MtkDeviceModel("Vivo Y12 / Y15 / Y17", "MT6765 (Helio P35)", "MT6765", "TestPoint (CMD to GND)"),
                MtkDeviceModel("Vivo Y21 / Y30 / Y3s", "MT6765 (Helio P35)", "MT6765", "TestPoint (CMD to GND)"),
                MtkDeviceModel("Vivo Y01 / Y02 / Y02t", "MT6765 (Helio P35)", "MT6765", "TestPoint (CMD to GND)"),
                MtkDeviceModel("Vivo Y19 / S1", "MT6768 (Helio P65)", "MT6768", "TestPoint (CMD to GND)"),
                MtkDeviceModel("Vivo Y22 / Y27 4G", "MT6769 (Helio G85)", "MT6768", "TestPoint (CMD to GND)"),
                MtkDeviceModel("Vivo Y36 4G", "MT6789 (Helio G99)", "MT6789", "TestPoint (CMD to GND)"),
                MtkDeviceModel("Vivo V11i / V15", "MT6771 (Helio P60/P70)", "MT6771", "TestPoint (CMD to GND)"),
                MtkDeviceModel("Vivo V21 5G / V21e 5G", "MT6853 (Dimensity 800U)", "MT6853", "TestPoint (CMD to GND)"),
                MtkDeviceModel("Vivo Y72 5G / Y75 5G", "MT6833 (Dimensity 700)", "MT6833", "TestPoint (CMD to GND)"),
                MtkDeviceModel("Vivo V23 Pro 5G / V25 5G", "MT6893 (Dimensity 1200/900)", "MT6893", "TestPoint (CMD to GND)")
            )
        ),
        MtkBrand(
            brandName = "Tecno",
            iconName = "tecno",
            models = listOf(
                MtkDeviceModel("Tecno Spark 6 Go / Spark 7", "MT6761 (Helio A22/A25)", "MT6761", "Hold Vol+ & Vol- -> Insert USB"),
                MtkDeviceModel("Tecno Spark 8 / Spark 8C", "MT6765 (Helio P22/G35)", "MT6765", "Hold Vol+ & Vol- -> Insert USB"),
                MtkDeviceModel("Tecno Spark 9 / Spark 9 Pro", "MT6765 (Helio G37/G85)", "MT6765", "Hold Vol+ & Vol- -> Insert USB"),
                MtkDeviceModel("Tecno Spark 10 / Spark 10 Pro", "MT6768 (Helio G88)", "MT6768", "Hold Vol+ & Vol- -> Insert USB"),
                MtkDeviceModel("Tecno Spark 20 / Spark 20 Pro", "MT6789 (Helio G99)", "MT6789", "Hold Vol+ & Vol- -> Insert USB"),
                MtkDeviceModel("Tecno Pova / Pova 2", "MT6768 (Helio G80/G85)", "MT6768", "Hold Vol+ & Vol- -> Insert USB"),
                MtkDeviceModel("Tecno Pova 3 / Pova 4 / Pova 5", "MT6789 (Helio G99)", "MT6789", "Hold Vol+ & Vol- -> Insert USB"),
                MtkDeviceModel("Tecno Camon 17 / Camon 18", "MT6768 (Helio G85/G88)", "MT6768", "Hold Vol+ & Vol- -> Insert USB"),
                MtkDeviceModel("Tecno Camon 19 / Camon 20", "MT6768 (Helio G85/G99)", "MT6768", "Hold Vol+ & Vol- -> Insert USB"),
                MtkDeviceModel("Tecno Camon 20 Pro 5G", "MT6895 (Dimensity 8050)", "MT6895", "Hold Vol+ & Vol- -> Insert USB")
            )
        ),
        MtkBrand(
            brandName = "Infinix",
            iconName = "infinix",
            models = listOf(
                MtkDeviceModel("Infinix Smart 4 / Smart 5 / Smart 6", "MT6761 (Helio A22)", "MT6761", "Hold Vol+ & Vol- -> Insert USB"),
                MtkDeviceModel("Infinix Hot 8 / Hot 9 / Hot 9 Play", "MT6761 (Helio A22/A25)", "MT6761", "Hold Vol+ & Vol- -> Insert USB"),
                MtkDeviceModel("Infinix Hot 10 Play / 11 Play / 12i", "MT6765 (Helio G25/G35)", "MT6765", "Hold Vol+ & Vol- -> Insert USB"),
                MtkDeviceModel("Infinix Hot 10 / Hot 11 / Hot 12", "MT6768 (Helio G70/G85)", "MT6768", "Hold Vol+ & Vol- -> Insert USB"),
                MtkDeviceModel("Infinix Hot 30 / Hot 40 / Hot 40 Pro", "MT6789 (Helio G88/G99)", "MT6789", "Hold Vol+ & Vol- -> Insert USB"),
                MtkDeviceModel("Infinix Note 7 / Note 8 / Note 10", "MT6768 (Helio G70/G85)", "MT6768", "Hold Vol+ & Vol- -> Insert USB"),
                MtkDeviceModel("Infinix Note 11 / Note 11 Pro", "MT6781 (Helio G96)", "MT6781", "Hold Vol+ & Vol- -> Insert USB"),
                MtkDeviceModel("Infinix Note 12 / Note 12 G96", "MT6781 (Helio G96)", "MT6781", "Hold Vol+ & Vol- -> Insert USB"),
                MtkDeviceModel("Infinix Note 30 / Note 30 Pro", "MT6789 (Helio G99)", "MT6789", "Hold Vol+ & Vol- -> Insert USB"),
                MtkDeviceModel("Infinix Note 40 / Note 40 Pro", "MT6789 (Helio G99 Ultimate)", "MT6789", "Hold Vol+ & Vol- -> Insert USB"),
                MtkDeviceModel("Infinix Zero 8 / Zero 8i", "MT6785 (Helio G90T)", "MT6785", "Hold Vol+ & Vol- -> Insert USB"),
                MtkDeviceModel("Infinix Zero 5G / Zero 20", "MT6877 (Dimensity 900/G99)", "MT6877", "Hold Vol+ & Vol- -> Insert USB")
            )
        ),
        MtkBrand(
            brandName = "Itel",
            iconName = "itel",
            models = listOf(
                MtkDeviceModel("Itel A16 / A17 / A18", "MT6580 (Legacy 32-bit)", "MT6580", "Hold Vol Down -> Insert USB"),
                MtkDeviceModel("Itel S15 / P33 Plus / A56", "MT6580 (Legacy 32-bit)", "MT6580", "Hold Vol Down -> Insert USB"),
                MtkDeviceModel("Itel Vision 1 / Vision 1 Pro", "MT6761 (Helio A22)", "MT6761", "Hold Vol+ & Vol- -> Insert USB"),
                MtkDeviceModel("Itel A48 / A25 Pro", "MT6761 (Helio A22)", "MT6761", "Hold Vol+ & Vol- -> Insert USB"),
                MtkDeviceModel("Itel Vision 2s / Vision 3", "MT6762 (Helio P22)", "MT6762", "Hold Vol+ & Vol- -> Insert USB"),
                MtkDeviceModel("Itel S23+ (Curved AMOLED)", "MT6768 (Helio G85)", "MT6768", "Hold Vol+ & Vol- -> Insert USB")
            )
        ),
        MtkBrand(
            brandName = "Huawei / Honor",
            iconName = "huawei",
            models = listOf(
                MtkDeviceModel("Honor X8a (CRT-LX1/LX2/LX3)", "MT6768 (Helio G88)", "MT6768", "Hold Vol+ & Vol- -> Insert USB"),
                MtkDeviceModel("Honor X5 / X5 Plus", "MT6765 (Helio G36)", "MT6765", "Hold Vol+ & Vol- -> Insert USB"),
                MtkDeviceModel("Honor X6 / X6a (WDY-LX1)", "MT6765 (Helio G25/G36)", "MT6765", "Hold Vol+ & Vol- -> Insert USB"),
                MtkDeviceModel("Honor Play 9A / 9C", "MT6765 (Helio P35)", "MT6765", "Hold Vol+ & Vol- or TestPoint"),
                MtkDeviceModel("Huawei Y5p / Y6p (MED-LX9)", "MT6762 (Helio P22)", "MT6762", "TestPoint (CMD to GND) / Vol Keys"),
                MtkDeviceModel("Honor 70 Lite / 90 Lite (CRT-N53)", "MT6833 (Dimensity 6020)", "MT6833", "Hold Vol+ & Vol- -> Insert USB")
            )
        ),
        MtkBrand(
            brandName = "Motorola / Lenovo",
            iconName = "motorola",
            models = listOf(
                MtkDeviceModel("Moto E7 / E7 Power (XT2097)", "MT6762 (Helio P22/G25)", "MT6762", "Hold Vol+ & Vol- -> Insert USB"),
                MtkDeviceModel("Moto E20 (XT2155)", "MT6765 (Unisoc/MTK)", "MT6765", "Hold Vol+ & Vol- -> Insert USB"),
                MtkDeviceModel("Moto G22 (XT2231)", "MT6765 (Helio G37)", "MT6765", "Hold Vol+ & Vol- -> Insert USB"),
                MtkDeviceModel("Moto G24 / G24 Power", "MT6768 (Helio G85)", "MT6768", "Hold Vol+ & Vol- -> Insert USB"),
                MtkDeviceModel("Lenovo Tab M8 / M10 HD (TB-X306)", "MT6762 (Helio P22T)", "MT6762", "Hold Vol+ & Vol- -> Insert USB"),
                MtkDeviceModel("Lenovo Tab M10 Plus (TB-X606)", "MT6765 (Helio P22T)", "MT6765", "Hold Vol+ & Vol- -> Insert USB")
            )
        ),
        MtkBrand(
            brandName = "OnePlus",
            iconName = "oneplus",
            models = listOf(
                MtkDeviceModel("OnePlus Nord 2 5G (DN2101/DN2103)", "MT6893 (Dimensity 1200-AI)", "MT6893", "Hold Vol+ & Vol- -> Insert USB"),
                MtkDeviceModel("OnePlus Nord 2T 5G (CPH2399)", "MT6893 (Dimensity 1300)", "MT6893", "Hold Vol+ & Vol- -> Insert USB"),
                MtkDeviceModel("OnePlus Nord 3 5G (CPH2491/CPH2493)", "MT6895 (Dimensity 9000)", "MT6895", "Hold Vol+ & Vol- -> Insert USB"),
                MtkDeviceModel("OnePlus Pad / Pad Go (OPD2203)", "MT6895 (Dimensity 9000/G99)", "MT6895", "Hold Vol+ & Vol- -> Insert USB")
            )
        )
    )

    fun getDefaultBrand(): MtkBrand = brands.first()
    fun getDefaultModel(): MtkDeviceModel = brands.first().models.first()
}
