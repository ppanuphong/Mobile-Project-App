package com.petcare.app.model

/**
 * สายพันธุ์สัตว์สำหรับช่องเลือกในฟอร์มนัดหมาย
 * - en: ชื่ออังกฤษ (จาก API หรือรายการในเครื่อง)
 * - th: ชื่อไทย มีเฉพาะพันธุ์ยอดนิยม
 */
data class Breed(val en: String, val th: String? = null) {
    val label: String
        get() = if (th != null) "$th ($en)" else en

    /** ค้นหาได้ทั้งชื่อไทยและอังกฤษ */
    fun matches(query: String): Boolean {
        val q = query.trim()
        if (q.isEmpty()) return true
        return en.contains(q, ignoreCase = true) || th?.contains(q, ignoreCase = true) == true || label == q
    }
}

/**
 * แหล่งรายชื่อพันธุ์ตามชนิดสัตว์ ใช้คำเดียวกับ [SPECIES_OPTIONS] และ speciesEmoji
 * - สุนัข/แมว: API ภายนอก (dog.ceo, catfact.ninja)
 * - ชนิดอื่น: PetCare Catalog API หมวด [catalogId] (hosting/api/pet-catalog.json)
 * - [popular]: รายการสำรองในเครื่อง (ออฟไลน์) และชื่อไทยของพันธุ์ยอดนิยม
 */
enum class BreedGroup(val catalogId: String, val popular: List<Breed>) {
    DOG("dog", POPULAR_DOGS),
    CAT("cat", POPULAR_CATS),
    RABBIT("rabbit", RABBITS),
    BIRD("bird", BIRDS),
    FISH("fish", FISHES),
    RODENT("rodent", RODENTS),
    REPTILE("reptile", REPTILES),
    EXOTIC("exotic", EXOTICS);

    companion object {
        fun fromSpecies(species: String): BreedGroup? = when {
            // ชื่อหมวดใน SPECIES_OPTIONS ก่อน แล้วค่อยเป็นชื่อชนิดเฉพาะ (ข้อมูลเก่า/พิมพ์เอง)
            species.contains("สัตว์ฟันแทะ") -> RODENT
            species.contains("สัตว์เลื้อยคลาน") -> REPTILE
            species.contains("สัตว์เลี้ยงพิเศษ") -> EXOTIC
            species.contains("สุนัข") || species.contains("หมา") -> DOG
            species.contains("แมว") -> CAT
            species.contains("กระต่าย") -> RABBIT
            species.contains("นก") -> BIRD
            species.contains("ปลา") -> FISH
            listOf("หนู", "แฮมสเตอร์", "ชินชิล่า", "เดกู").any { species.contains(it) } -> RODENT
            listOf("เต่า", "งู", "กิ้งก่า", "ตุ๊กแก", "อีกัวน่า", "อิกัวน่า").any { species.contains(it) } -> REPTILE
            listOf("เม่น", "ชูก้าไกลเดอร์", "เฟอร์เร็ต").any { species.contains(it) } -> EXOTIC
            else -> null
        }
    }
}

/** คำที่ dog.ceo เขียนติดกัน แปลงเป็นชื่อที่คนคุ้น */
private val DOG_WORD_FIXES = mapOf(
    "shihtzu" to "Shih Tzu",
    "stbernard" to "St. Bernard",
    "husky" to "Siberian Husky",
    "pembroke" to "Pembroke Welsh Corgi",
    "sharpei" to "Shar Pei",
    "mexicanhairless" to "Mexican Hairless",
    "cotondetulear" to "Coton de Tulear",
    "germanlonghair" to "German Longhaired",
    "kerryblue" to "Kerry Blue",
    "westhighland" to "West Highland",
    "flatcoated" to "Flat-Coated",
    "danishswedish" to "Danish-Swedish",
    "bullterrier" to "Bull Terrier",
    "cattledog" to "Cattle Dog",
    "mix" to "Mixed Breed",
)

/** dog.ceo ใช้คำคุณศัพท์เป็นชื่อหลักในบางพันธุ์ เช่น `{"german": ["shepherd"]}` ต้องเรียง "หลัก รอง" */
private val DOG_MAIN_FIRST = setOf("australian", "african", "german", "finnish", "danishswedish", "rough")

/** แปลงผลจาก dog.ceo `{"bulldog": ["french"]}` เป็น "French Bulldog" และ `{"german": ["shepherd"]}` เป็น "German Shepherd" */
fun dogCeoNames(breeds: Map<String, List<String>>): List<String> =
    breeds.flatMap { (main, subs) ->
        when {
            subs.isEmpty() -> listOf(dogWords(main))
            main in DOG_MAIN_FIRST -> subs.map { dogWords("$main $it") }
            else -> subs.map { dogWords("$it $main") }
        }
    }

private fun dogWords(raw: String): String =
    raw.split(' ').filter { it.isNotBlank() }.joinToString(" ") { word ->
        DOG_WORD_FIXES[word] ?: word.replaceFirstChar { it.uppercase() }
    }

/** ล้างชื่อจาก API: ตัดเชิงอรรถ [4] และเติมช่องว่างหน้าวงเล็บที่ติดกัน */
fun cleanBreedName(raw: String): String =
    raw.replace(Regex("""\[\d+]"""), "")
        .replace(Regex("""(?<=\S)\("""), " (")
        .replace(Regex("""\s+"""), " ")
        .trim()

/** คีย์สำหรับหาชื่อไทย: ตัดส่วนขยายหลัง " (" หรือ ", or" ออก เช่น "Persian (Modern Persian Cat)" → "persian" */
private fun baseKey(en: String): String =
    en.substringBefore(" (").substringBefore(", or").trim().lowercase()

/**
 * รวมรายชื่อจาก API กับชื่อไทยของพันธุ์ยอดนิยม
 * พันธุ์ที่มีชื่อไทยขึ้นก่อน ที่เหลือเรียง A–Z และพันธุ์ใน [popular] ที่ API ไม่มีจะถูกเติมเข้าไป
 */
fun buildBreeds(names: List<String>, popular: List<Breed>): List<Breed> {
    val thaiNames = popular.associate { baseKey(it.en) to it.th }
    val fromApi = names.map(::cleanBreedName)
        .filter { it.isNotBlank() }
        .map { Breed(it, thaiNames[baseKey(it)]) }
    val apiKeys = fromApi.map { baseKey(it.en) }.toSet()
    val missing = popular.filter { baseKey(it.en) !in apiKeys }
    return (fromApi + missing)
        .distinctBy { it.en.lowercase() }
        .sortedWith(compareBy({ it.th == null }, { it.en.lowercase() }))
}

private val POPULAR_DOGS = listOf(
    Breed("Pug", "ปั๊ก"),
    Breed("Shih Tzu", "ชิสุห์"),
    Breed("Pomeranian", "ปอมเมอเรเนียน"),
    Breed("Chihuahua", "ชิวาวา"),
    Breed("Poodle", "พุดเดิ้ล"),
    Breed("Toy Poodle", "ทอยพุดเดิ้ล"),
    Breed("Miniature Poodle", "มินิเอเจอร์พุดเดิ้ล"),
    Breed("Standard Poodle", "สแตนดาร์ดพุดเดิ้ล"),
    Breed("Golden Retriever", "โกลเด้น รีทรีฟเวอร์"),
    Breed("Labrador", "ลาบราดอร์"),
    Breed("Beagle", "บีเกิ้ล"),
    Breed("Siberian Husky", "ไซบีเรียน ฮัสกี้"),
    Breed("French Bulldog", "เฟรนช์ บูลด็อก"),
    Breed("English Bulldog", "อิงลิช บูลด็อก"),
    Breed("Pembroke Welsh Corgi", "คอร์กี้ (เพมโบรค)"),
    Breed("Maltese", "มอลทีส"),
    Breed("Yorkshire Terrier", "ยอร์คเชียร์ เทอร์เรีย"),
    Breed("Dachshund", "ดัชชุนด์"),
    Breed("German Shepherd", "เยอรมันเชพเพิร์ด"),
    Breed("Rottweiler", "ร็อตไวเลอร์"),
    Breed("Doberman", "โดเบอร์แมน"),
    Breed("Shiba", "ชิบะ"),
    Breed("Akita", "อากิตะ"),
    Breed("Samoyed", "ซามอยด์"),
    Breed("Border Collie", "บอร์เดอร์ คอลลี่"),
    Breed("Pekinese", "ปักกิ่ง"),
    Breed("Chow", "เชาเชา"),
    Breed("Bichon Frise", "บิชอง ฟริเซ่"),
    Breed("Pitbull", "พิทบูล"),
    Breed("Dalmatian", "ดัลเมเชียน"),
    Breed("Great Dane", "เกรทเดน"),
    Breed("Miniature Schnauzer", "มินิเอเจอร์ ชเนาเซอร์"),
    Breed("Thai Ridgeback", "ไทยหลังอาน"),
    Breed("Thai Bangkaew", "บางแก้ว"),
    Breed("Mixed Breed", "พันธุ์ผสม"),
)

private val POPULAR_CATS = listOf(
    Breed("Persian", "เปอร์เซีย"),
    Breed("Siamese", "วิเชียรมาศ"),
    Breed("Korat", "โคราช (สีสวาด)"),
    Breed("Khao Manee", "ขาวมณี"),
    Breed("Suphalak", "ศุภลักษณ์"),
    Breed("Scottish Fold", "สก็อตติช โฟลด์"),
    Breed("British Shorthair", "บริติช ช็อตแฮร์"),
    Breed("American Shorthair", "อเมริกัน ช็อตแฮร์"),
    Breed("Exotic Shorthair", "เอ็กซอติก ช็อตแฮร์"),
    Breed("Maine Coon", "เมนคูน"),
    Breed("Ragdoll", "แร็กดอลล์"),
    Breed("Bengal", "เบงกอล"),
    Breed("Sphynx", "สฟิงซ์"),
    Breed("Munchkin", "มันช์กิ้น"),
    Breed("Russian Blue", "รัสเซียนบลู"),
    Breed("Norwegian Forest cat", "นอร์วีเจียน ฟอเรสต์"),
    Breed("Himalayan", "หิมาลายัน"),
    Breed("Abyssinian", "อะบิสซิเนียน"),
    Breed("Mixed Breed", "พันธุ์ผสม"),
)

private val RABBITS = listOf(
    Breed("Holland Lop", "ฮอลแลนด์ ลอป"),
    Breed("Netherland Dwarf", "เนเธอร์แลนด์ ดวาร์ฟ"),
    Breed("Lionhead", "ไลอ้อนเฮด"),
    Breed("Mini Rex", "มินิเร็กซ์"),
    Breed("Angora", "แองโกร่า"),
    Breed("Mixed Breed", "พันธุ์ผสม"),
)

private val BIRDS = listOf(
    Breed("Budgerigar", "หงส์หยก"),
    Breed("Cockatiel", "ค็อกคาเทล"),
    Breed("Lovebird", "เลิฟเบิร์ด"),
    Breed("African Grey Parrot", "แก้วแอฟริกันเกรย์"),
    Breed("Sun Conure", "ซันคอนัวร์"),
)

private val FISHES = listOf(
    Breed("Goldfish", "ปลาทอง"),
    Breed("Betta", "ปลากัด"),
    Breed("Guppy", "ปลาหางนกยูง"),
    Breed("Koi", "ปลาคาร์ป"),
    Breed("Arowana", "ปลาอะโรวาน่า"),
)

private val RODENTS = listOf(
    Breed("Syrian Hamster", "แฮมสเตอร์ไจแอนท์"),
    Breed("Winter White Hamster", "แฮมสเตอร์วินเทอร์ไวท์"),
    Breed("Roborovski Hamster", "แฮมสเตอร์โรโบรอฟสกี้"),
    Breed("Campbell's Dwarf Hamster", "แฮมสเตอร์แคมป์เบลล์"),
    Breed("Guinea Pig", "หนูตะเภา"),
    Breed("Chinchilla", "ชินชิล่า"),
)

private val REPTILES = listOf(
    Breed("Red-eared Slider", "เต่าแก้มแดง"),
    Breed("Sulcata Tortoise", "เต่าซูคาต้า"),
    Breed("Leopard Tortoise", "เต่าเสือดาว"),
    Breed("Bearded Dragon", "เบียร์ดดราก้อน"),
    Breed("Leopard Gecko", "ตุ๊กแกเลพเพิร์ด"),
    Breed("Ball Python", "งูบอลไพธอน"),
)

private val EXOTICS = listOf(
    Breed("African Pygmy Hedgehog", "เม่นแคระ"),
    Breed("Sugar Glider", "ชูก้าไกลเดอร์"),
    Breed("Ferret", "เฟอร์เร็ต"),
)
