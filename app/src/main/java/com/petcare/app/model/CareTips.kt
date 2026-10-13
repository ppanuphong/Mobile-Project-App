package com.petcare.app.model

import java.time.LocalDate
import java.time.Month

/** ฤดูกาลของไทย (กรมอุตุนิยมวิทยา) */
enum class ThaiSeason(val label: String, val emoji: String) {
    HOT("ฤดูร้อน", "☀️"),
    RAINY("ฤดูฝน", "🌧️"),
    COOL("ฤดูหนาว", "❄️");

    companion object {
        fun of(date: LocalDate): ThaiSeason = when (date.month) {
            Month.MARCH, Month.APRIL, Month.MAY -> HOT
            Month.JUNE, Month.JULY, Month.AUGUST, Month.SEPTEMBER, Month.OCTOBER -> RAINY
            else -> COOL
        }
    }
}

/** ลักษณะเด่นของสายพันธุ์ที่ต้องดูแลเป็นพิเศษ */
enum class BreedTrait {
    THICK_COAT, // ขนหนา ทนร้อนไม่ได้
    FLAT_FACE, // หน้าสั้น หายใจลำบาก
    LONG_BACK, // หลังยาว เสี่ยงหมอนรองกระดูก
    TOY, // ตัวเล็ก ปัญหาฟัน/กระดูกเปราะ
    FLOPPY_EARS, // หูตก หูอักเสบง่าย
    SKIN_FOLDS, // ผิวหนังพับ อับชื้น
    LARGE, // ตัวใหญ่ ข้อสะโพก
    LONG_HAIR_CAT, // แมวขนยาว ก้อนขน
    FOLD_EARS, // สก็อตติช โฟลด์ ข้อต่อ
    HAIRLESS, // ไม่มีขน หนาว/แดดเผา
}

/** เคล็ดลับ 1 ใบ: tag = ป้ายบอกที่มา เช่น "ฤดูร้อน · ไซบีเรียน ฮัสกี้" */
data class CareTip(
    val id: String,
    val emoji: String,
    val title: String,
    val message: String,
    val tag: String,
    val petIds: List<String> = emptyList(),
)

/**
 * สร้างเคล็ดลับดูแลสัตว์เลี้ยงจากสายพันธุ์/ชนิดที่ผู้ใช้กรอก + ฤดูกาลปัจจุบัน
 * ทำงานในเครื่องทั้งหมด ไม่ต้องต่อ API
 */
object CareTipEngine {

    // คำค้นในชื่อสายพันธุ์ (ไทยหรืออังกฤษ ตัวพิมพ์เล็ก) → ลักษณะเด่น
    private val TRAIT_KEYWORDS: Map<BreedTrait, List<String>> = mapOf(
        BreedTrait.THICK_COAT to listOf(
            "husky", "ฮัสกี้", "malamute", "มาลามิวท์", "samoyed", "ซามอยด์", "chow", "เชาเชา",
            "akita", "อากิตะ", "shiba", "ชิบะ", "pomeranian", "ปอม", "golden", "โกลเด้น",
            "keeshond", "newfoundland", "bernese", "เบอร์นีส", "persian", "เปอร์เซีย", "maine coon", "เมนคูน",
            "norwegian", "นอร์วีเจียน", "himalayan", "หิมาลายัน", "ragdoll", "แร็กดอลล์", "angora", "แองโกร่า",
        ),
        BreedTrait.FLAT_FACE to listOf(
            "pug", "ปั๊ก", "bulldog", "บูลด็อก", "shih tzu", "shihtzu", "ชิสุ", "pekinese", "pekingese", "ปักกิ่ง",
            "boston", "บอสตัน", "boxer", "บ็อกเซอร์", "persian", "เปอร์เซีย", "exotic", "เอ็กซอติก", "himalayan", "หิมาลายัน",
        ),
        BreedTrait.LONG_BACK to listOf("dachshund", "ดัชชุนด์", "ไส้กรอก", "corgi", "คอร์กี้", "basset", "บาสเซ็ต"),
        BreedTrait.TOY to listOf(
            "chihuahua", "ชิวาวา", "toy", "ทอย", "yorkshire", "ยอร์คเชียร์", "maltese", "มอลทีส",
            "pomeranian", "ปอม", "papillon", "miniature pinscher", "มินพิน",
        ),
        BreedTrait.FLOPPY_EARS to listOf(
            "cocker", "ค็อกเกอร์", "beagle", "บีเกิ้ล", "basset", "บาสเซ็ต", "spaniel", "สแปเนียล",
            "golden", "โกลเด้น", "labrador", "ลาบราดอร์", "lop", "ลอป",
        ),
        BreedTrait.SKIN_FOLDS to listOf("shar pei", "sharpei", "ชาเป่ย", "pug", "ปั๊ก", "bulldog", "บูลด็อก"),
        BreedTrait.LARGE to listOf(
            "german shepherd", "เยอรมันเชพเพิร์ด", "great dane", "เกรทเดน", "rottweiler", "ร็อตไวเลอร์",
            "labrador", "ลาบราดอร์", "golden", "โกลเด้น", "mastiff", "มาสทิฟ", "st. bernard", "เซนต์เบอร์นาร์ด",
            "bernese", "เบอร์นีส", "newfoundland", "akita", "อากิตะ",
        ),
        BreedTrait.LONG_HAIR_CAT to listOf(
            "persian", "เปอร์เซีย", "maine coon", "เมนคูน", "ragdoll", "แร็กดอลล์", "himalayan", "หิมาลายัน",
            "norwegian", "นอร์วีเจียน", "angora", "แองโกร่า",
        ),
        BreedTrait.FOLD_EARS to listOf("scottish fold", "สก็อตติช โฟลด์", "โฟลด์"),
        BreedTrait.HAIRLESS to listOf("sphynx", "สฟิงซ์", "hairless", "ไม่มีขน"),
    )

    fun traitsOf(breed: String): Set<BreedTrait> {
        val text = breed.lowercase()
        if (text.isBlank()) return emptySet()
        return TRAIT_KEYWORDS.filterValues { words -> words.any { text.contains(it) } }.keys
    }

    /** ชื่อสายพันธุ์สำหรับแสดง: "ไซบีเรียน ฮัสกี้ (Siberian Husky)" → "ไซบีเรียน ฮัสกี้" */
    fun shortBreed(breed: String): String = breed.substringBefore(" (").trim()

    /** "โมจิ" / "โมจิและเต้าหู้" / "โมจิ เต้าหู้ และอีก 2 ตัว" */
    fun joinNames(names: List<String>): String = when (names.size) {
        0 -> "น้อง ๆ"
        1 -> names[0]
        2 -> "${names[0]}และ${names[1]}"
        else -> "${names[0]} ${names[1]} และอีก ${names.size - 2} ตัว"
    }

    private enum class Kind { DOG, CAT, RABBIT, BIRD, FISH, RODENT, REPTILE, OTHER }

    private fun kindOf(pet: Pet): Kind {
        val s = pet.species
        return when {
            s.contains("สุนัข") || s.contains("หมา") -> Kind.DOG
            s.contains("แมว") -> Kind.CAT
            s.contains("กระต่าย") -> Kind.RABBIT
            s.contains("นก") -> Kind.BIRD
            s.contains("ปลา") -> Kind.FISH
            s.contains("หนู") || s.contains("แฮมสเตอร์") || s.contains("ชินชิล่า") || s.contains("เดกู") -> Kind.RODENT
            s.contains("เต่า") || s.contains("งู") || s.contains("กิ้งก่า") || s.contains("ตุ๊กแก") || s.contains("อีกัวน่า") -> Kind.REPTILE
            else -> Kind.OTHER
        }
    }

    /** ร่างเคล็ดลับก่อนรวมชื่อ: key ใช้รวมสัตว์หลายตัวที่ได้เคล็ดลับเดียวกัน */
    private data class Draft(
        val key: String,
        val priority: Int,
        val emoji: String,
        val tag: String,
        val title: (names: String) -> String,
        val message: (names: String) -> String,
    )

    fun tipsFor(pets: List<Pet>, today: LocalDate = LocalDate.now()): List<CareTip> {
        val season = ThaiSeason.of(today)
        val seasonTag = season.label
        val drafts = mutableListOf<Pair<Draft, Pet>>()

        for (pet in pets) {
            val traits = traitsOf(pet.breed)
            val breed = shortBreed(pet.breed)
            val kind = kindOf(pet)
            fun add(d: Draft) = drafts.add(d to pet)

            // --- สายพันธุ์ × ฤดูกาล (ตรงตัวที่สุด ขึ้นก่อน) ---
            if (BreedTrait.THICK_COAT in traits && season == ThaiSeason.HOT) add(
                Draft("heat-thick-${pet.id}", 0, "🥵", "$seasonTag · $breed",
                    { "อากาศร้อน ระวัง${it}เป็นฮีตสโตรก" },
                    { "${breed}ขนหนาทนร้อนได้ไม่ดี เลี่ยงพาเดินช่วง 10 โมงเช้า–4 โมงเย็น เปิดพัดลมหรือแอร์ และมีน้ำเย็นให้ตลอด ถ้าหอบหนัก ลิ้นแดงจัด ซึม ให้รีบพาพบหมอ" }),
            )
            if (BreedTrait.FLAT_FACE in traits) add(
                Draft("flat-face-${pet.id}", if (season == ThaiSeason.HOT) 0 else 1, "😮‍💨", breed,
                    { "${breed}ควรระวังเรื่องการหายใจ" },
                    {
                        val hot = if (season == ThaiSeason.HOT) " และช่วงนี้อากาศร้อนให้อยู่ในที่เย็นเป็นพิเศษ" else ""
                        "สายพันธุ์หน้าสั้นหายใจลำบากง่าย ใช้สายรัดอกแทนปลอกคอ ไม่ออกกำลังหนัก$hot" +
                            " ถ้า${it}หายใจเสียงดังหรือกรนผิดปกติควรพบหมอ"
                    }),
            )
            if (BreedTrait.FLOPPY_EARS in traits && season == ThaiSeason.RAINY) add(
                Draft("ears-rain-${pet.id}", 0, "👂", "$seasonTag · $breed",
                    { "หน้าฝนอับชื้น ระวังหูอักเสบ" },
                    { "${breed}หูตก ความชื้นสะสมง่าย เช็ดหู${it}ให้แห้งหลังอาบน้ำหรือโดนฝน ถ้ามีกลิ่นหรือเกาหูบ่อยควรพบหมอ" }),
            )
            if (BreedTrait.SKIN_FOLDS in traits) add(
                Draft("folds-${pet.id}", if (season == ThaiSeason.RAINY) 0 else 2, "🧼", breed,
                    { "ทำความสะอาดรอยพับผิวหนังของ$it" },
                    {
                        val rainy = if (season == ThaiSeason.RAINY) " หน้าฝนอับชื้น เชื้อราขึ้นง่ายเป็นพิเศษ" else ""
                        "เช็ดรอยย่นบนหน้าและตัวให้สะอาดและแห้งทุกวัน$rainy"
                    }),
            )
            if (BreedTrait.HAIRLESS in traits) add(
                Draft("hairless-${pet.id}", 1, if (season == ThaiSeason.COOL) "🧥" else "🧴", breed,
                    { if (season == ThaiSeason.COOL) "อากาศเย็น ${it}อาจหนาวง่าย" else "${breed}ผิวไหม้แดดง่าย" },
                    { if (season == ThaiSeason.COOL) "แมวไม่มีขนเก็บความอบอุ่นได้น้อย ให้นอนในที่อุ่น มีผ้าห่ม และหลีกเลี่ยงลมโกรก" else "ให้อยู่ในร่ม หลีกเลี่ยงแดดจัด และเช็ดคราบมันบนผิวสัปดาห์ละครั้ง" }),
            )

            // --- สายพันธุ์ (ทุกฤดู) ---
            if (BreedTrait.LONG_BACK in traits) add(
                Draft("back-${pet.id}", 2, "🦴", breed,
                    { "${breed}หลังยาว ระวังหมอนรองกระดูก" },
                    { "อย่าให้${it}กระโดดขึ้นลงโซฟาหรือบันไดบ่อย อุ้มโดยประคองทั้งหน้าอกและสะโพก และคุมน้ำหนักไม่ให้อ้วน" }),
            )
            if (BreedTrait.TOY in traits) add(
                Draft("toy-${pet.id}", 2, "🦷", breed,
                    { "พันธุ์เล็กมีปัญหาฟันได้ง่าย" },
                    { "แปรงฟัน${it}สัปดาห์ละ 2–3 ครั้ง และระวังการตกจากที่สูงเพราะกระดูกเปราะ" }),
            )
            if (BreedTrait.LARGE in traits) add(
                Draft("large-${pet.id}", 3, "🏋️", breed,
                    { "พันธุ์ใหญ่ ระวังข้อสะโพก" },
                    { "คุมน้ำหนัก${it}ให้พอดี เลี่ยงวิ่งบนพื้นลื่น และตรวจข้อสะโพกเมื่ออายุมากขึ้น" }),
            )
            if (BreedTrait.LONG_HAIR_CAT in traits) add(
                Draft("hairball-${pet.id}", 2, "🪮", breed,
                    { "แปรงขน${it}ทุกวัน ลดก้อนขน" },
                    { "แมวขนยาวกลืนขนเยอะ แปรงขนทุกวันช่วยลดการอาเจียนก้อนขนและขนพันกัน" }),
            )
            if (BreedTrait.FOLD_EARS in traits) add(
                Draft("fold-${pet.id}", 2, "🦵", breed,
                    { "${breed}ควรสังเกตข้อต่อ" },
                    { "สายพันธุ์หูพับเสี่ยงโรคกระดูกอ่อนผิดปกติ ถ้า${it}เดินกะเผลกหรือไม่อยากกระโดดควรพบหมอ" }),
            )

            // --- ชนิดสัตว์ × ฤดูกาล (รวมหลายตัวเป็นใบเดียว) ---
            when (season) {
                ThaiSeason.HOT -> when (kind) {
                    Kind.DOG, Kind.CAT -> add(Draft("hot-water", 4, "💧", seasonTag,
                        { "อากาศร้อน ให้${it}ดื่มน้ำให้พอ" },
                        { "เติมน้ำสะอาดหลายจุด เปลี่ยนน้ำบ่อย ๆ และระวังพื้นถนนตอนกลางวันร้อนจนลวกอุ้งเท้า" }))
                    Kind.RABBIT -> add(Draft("hot-rabbit", 1, "🐰", seasonTag,
                        { "กระต่ายทนร้อนไม่ได้" },
                        { "ให้${it}อยู่ในห้องที่อากาศถ่ายเท ใส่ขวดน้ำแช่แข็งห่อผ้าไว้ในกรง ถ้าหูร้อนจัดหายใจเร็วให้รีบระบายความร้อน" }))
                    Kind.FISH -> add(Draft("hot-fish", 2, "🌡️", seasonTag,
                        { "น้ำในตู้ร้อนเกินไป" },
                        { "อย่าวางตู้ปลาโดนแดด ช่วงร้อนจัดเพิ่มออกซิเจนในน้ำ และเปลี่ยนน้ำทีละน้อย" }))
                    Kind.RODENT, Kind.BIRD -> add(Draft("hot-small", 2, "🌬️", seasonTag,
                        { "ระวัง${it}ร้อนเกินไป" },
                        { "สัตว์ตัวเล็กร้อนง่าย วางกรงในที่ร่ม อากาศถ่ายเท ไม่โดนแดดตรง" }))
                    else -> {}
                }
                ThaiSeason.RAINY -> when (kind) {
                    Kind.DOG, Kind.CAT -> {
                        add(Draft("rain-ticks", 1, "🕷️", seasonTag,
                            { "หน้าฝนเห็บหมัดเยอะ" },
                            { "หยอดหรือป้อนยากันเห็บหมัดให้${it}ตรงเวลา และตรวจตามซอกหู ง่ามนิ้ว หลังพาออกไปข้างนอก" }))
                        if (kind == Kind.DOG) add(Draft("rain-mosquito", 3, "🦟", seasonTag,
                            { "ยุงชุก ระวังพยาธิหนอนหัวใจ" },
                            { "ให้${it}กินยาป้องกันพยาธิหนอนหัวใจทุกเดือน และกำจัดน้ำขังรอบบ้าน" }))
                    }
                    Kind.BIRD, Kind.RODENT, Kind.RABBIT -> add(Draft("rain-damp", 2, "💨", seasonTag,
                        { "หน้าฝนอับชื้น ดูแลกรงให้แห้ง" },
                        { "เปลี่ยนวัสดุรองกรงของ${it}บ่อยขึ้น ความชื้นทำให้เกิดเชื้อราและโรคทางเดินหายใจ" }))
                    else -> {}
                }
                ThaiSeason.COOL -> when (kind) {
                    Kind.DOG, Kind.CAT -> add(Draft("cool-skin", 4, "🧴", seasonTag,
                        { "อากาศแห้ง ระวังผิวแห้งคัน" },
                        { "อาบน้ำให้${it}ห่างขึ้น แปรงขนบ่อยขึ้น และให้อาหารที่มีโอเมก้า 3" }))
                    Kind.RODENT -> add(Draft("cool-hamster", 1, "❄️", seasonTag,
                        { "อากาศเย็น ${it}อาจเข้าภาวะจำศีล" },
                        { "รักษาอุณหภูมิห้องให้อุ่น ถ้าตัวเย็นนิ่งผิดปกติให้ค่อย ๆ อุ่นตัวและพาพบหมอ" }))
                    Kind.REPTILE -> add(Draft("cool-reptile", 1, "🔆", seasonTag,
                        { "อากาศเย็น ${it}อาจกินน้อยลง" },
                        { "สัตว์เลื้อยคลานต้องการความอบอุ่น ตรวจไฟให้ความร้อนและหลอด UVB ให้ทำงานปกติ" }))
                    Kind.FISH -> add(Draft("cool-fish", 3, "🐟", seasonTag,
                        { "น้ำเย็นลง ปลากินน้อยลง" },
                        { "ลดปริมาณอาหาร และพิจารณาใช้ฮีตเตอร์ถ้าอุณหภูมิน้ำต่ำกว่า 24°C" }))
                    else -> {}
                }
            }
        }

        // รวมสัตว์หลายตัวที่ได้เคล็ดลับเดียวกัน แล้วเรียงตามความสำคัญ
        val tips = drafts.groupBy({ it.first.key }, { it })
            .map { (key, entries) ->
                val draft = entries.first().first
                val petsInTip = entries.map { it.second }.distinctBy { it.id }
                val names = joinNames(petsInTip.map { it.name })
                draft.priority to CareTip(
                    id = key,
                    emoji = draft.emoji,
                    title = draft.title(names),
                    message = draft.message(names),
                    tag = draft.tag,
                    petIds = petsInTip.map { it.id },
                )
            }
            .sortedBy { it.first }
            .map { it.second }

        return tips.ifEmpty { listOf(generalTip(season)) }
    }

    /** ใช้เมื่อยังไม่มีสัตว์เลี้ยง หรือไม่มีข้อมูลพอ */
    fun generalTip(season: ThaiSeason): CareTip = when (season) {
        ThaiSeason.HOT -> CareTip("general-hot", "☀️", "หน้าร้อนดูแลน้อง ๆ ให้เย็นสบาย",
            "มีน้ำสะอาดตลอด เลี่ยงพาออกไปข้างนอกช่วงแดดจัด", season.label)
        ThaiSeason.RAINY -> CareTip("general-rainy", "🌧️", "หน้าฝนเห็บหมัดเยอะ",
            "ป้องกันเห็บหมัดตรงเวลา และเช็ดตัวให้แห้งหลังโดนฝน", season.label)
        ThaiSeason.COOL -> CareTip("general-cool", "❄️", "หน้าหนาวอากาศแห้ง",
            "แปรงขนบ่อยขึ้น และมีที่นอนอุ่น ๆ ให้น้อง ๆ", season.label)
    }
}
