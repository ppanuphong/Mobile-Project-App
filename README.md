# PetCare — แอปดูแลสัตว์เลี้ยง

แอป Android (Kotlin + Jetpack Compose) สำหรับบันทึกสัตว์เลี้ยง นัดหมอ และวัคซีน พร้อมแจ้งเตือนล่วงหน้า
สถาปัตยกรรม **MVVM + Repository** ฐานข้อมูล **Firebase (Cloud Firestore + Authentication)**

## ฟีเจอร์

| หน้าจอ | รายละเอียด |
|---|---|
| เข้าสู่ระบบ / สมัครสมาชิก | Firebase Auth (อีเมล + รหัสผ่าน), แสดง loading/error เป็นภาษาไทย, ปุ่มบัญชีเดโม, จำ session ด้วย DataStore |
| หน้าหลัก | สรุปจำนวนนัดเลยกำหนด / ใกล้ถึง / เสร็จเดือนนี้, รายการนัดที่เลยกำหนดและใกล้ถึงใน 7 วัน, รายชื่อสัตว์เลี้ยง |
| สัตว์เลี้ยง | ค้นหา เพิ่ม แก้ไข ลบ, โปรไฟล์แยกรายตัวพร้อมแท็บ "กำลังจะถึง / ประวัติวัคซีน / ประวัติหาหมอ" |
| นัดหมาย | เพิ่ม แก้ไข ลบ (ปัดซ้าย), ทำเครื่องหมายว่าเสร็จ, กรองตามสถานะและตามสัตว์เลี้ยง |
| แจ้งเตือน | เปิด/ปิดการแจ้งเตือน, ขอสิทธิ์ระบบ, ส่งทดสอบ, รายการที่ถึงเวลาเตือน / เลยกำหนด / ตั้งไว้ล่วงหน้า |

การแจ้งเตือนใช้ **WorkManager**: นัดแต่ละรายการจะเตือนเวลา 08:00 น. ของวัน (วันนัด − `reminderDaysBefore`)
ถ้าบันทึกนัดที่เลยเวลาเตือนไปแล้วแต่ยังไม่ถึงวันนัด จะเตือนทันที

## โครงสร้างโปรเจกต์

```
app/src/main/java/com/petcare/app/
├── model/          data class: User, Pet, VetAppointment + enum สถานะ/ประเภท
├── data/           Repository (Auth, User, Pet, Appointment), SessionManager (DataStore), DemoDataSeeder
│   └── remote/     ชื่อ collection + ตัวแปลง Firestore snapshot listener เป็น Flow (realtime)
├── viewmodel/      ViewModel แยกต่อหน้าจอ ใช้ StateFlow เก็บ UI state
├── ui/
│   ├── theme/      สี (light/dark), ฟอนต์, Typography
│   ├── navigation/ เส้นทาง + NavHost + Bottom Navigation 4 แท็บ
│   ├── components/ การ์ดนัด, avatar, date/time picker, dialog ฯลฯ
│   └── screens/    login, home, pets, appointments, notifications
├── notification/   ReminderScheduler (WorkManager), ReminderWorker, NotificationHelper
├── di/             AppContainer (สร้าง dependency ทั้งหมดแบบ manual)
└── util/           DateUtils (วันที่แบบไทย พ.ศ., อายุ, นับวัน)
```

การไหลของข้อมูล: `Screen (Compose)` → `ViewModel (StateFlow)` → `Repository` → `Firestore / Auth`
Repository ส่งข้อมูลกลับเป็น `Flow` ที่มาจาก `addSnapshotListener` จึงอัปเดตหน้าจอทันทีเมื่อข้อมูลเปลี่ยน

## โครงสร้างข้อมูลใน Firestore

| Collection | เอกสาร | หมายเหตุ |
|---|---|---|
| `users/{uid}` | `User` | uid ตรงกับ Firebase Auth |
| `pets/{petId}` | `Pet` | `ownerId` = uid เจ้าของ |
| `appointments/{id}` | `VetAppointment` | มี `ownerId` เพิ่มเพื่อ query และ security rules |

- `date` = `"yyyy-MM-dd"`, `time` = `"HH:mm"`, `status` = `"PENDING"` / `"DONE"`
- สถานะ "เลยกำหนด" คำนวณจากวันที่ตอนแสดงผล ไม่ได้เก็บลงฐานข้อมูล
- กฎความปลอดภัยอยู่ที่ [firestore.rules](firestore.rules): ผู้ใช้อ่าน/เขียนได้เฉพาะข้อมูลของตัวเอง

## วิธีรัน

ต้องใช้ Android Studio (หรือ JDK 17+) และ Android SDK 37

### แบบที่ 1: ใช้ Firebase จริงบนคลาวด์ (ตั้งค่าไว้แล้ว)

แอปเชื่อมกับโปรเจกต์ Firebase `petcare-5x7gf-a3068` แล้ว กดรันจาก Android Studio หรือสั่ง `./gradlew installDebug` ได้เลย

| บริการ | ค่าที่ตั้งไว้ |
|---|---|
| Android app | `com.petcare.app` ([app/google-services.json](app/google-services.json)) |
| Authentication | เปิด Email/Password |
| Firestore | Standard edition, `asia-southeast1` (สิงคโปร์), rules จาก `firestore.rules` |

ถ้าแก้ `firestore.rules` แล้วต้องการอัปโหลดขึ้นคลาวด์:

```bash
npx firebase-tools deploy --only firestore:rules
```

ถ้าจะย้ายไปใช้โปรเจกต์ Firebase อื่น ให้เพิ่มแอป Android `com.petcare.app` ในโปรเจกต์นั้น โหลด `google-services.json` มาวางทับ
เปิด Email/Password ใน Authentication สร้าง Firestore แล้วแก้ `default` ใน `.firebaserc` เป็น ID ของโปรเจกต์ใหม่

### แบบที่ 2: ใช้ Firebase Local Emulator (ข้อมูลอยู่ในเครื่อง ไม่แตะข้อมูลบนคลาวด์)

```bash
# เทอร์มินัลที่ 1: เปิด emulator ของ Auth + Firestore (ต้องมี Node.js และ Java)
npx firebase-tools emulators:start --only auth,firestore

# เทอร์มินัลที่ 2: ให้โทรศัพท์/emulator ต่อเข้าเครื่องเราผ่าน adb แล้วติดตั้งแอปโหมด emulator
adb reverse tcp:9099 tcp:9099
adb reverse tcp:8080 tcp:8080
./gradlew installDebug -PuseFirebaseEmulator=true
```

ข้อมูลใน emulator จะหายเมื่อปิด emulator
ใช้ `adb reverse` + `127.0.0.1` แทน `10.0.2.2` เพราะ Android 17 จำกัดไม่ให้แอปเข้าถึงเครือข่ายภายในโดยตรง

### บัญชีเดโม

กด **"ทดลองใช้ด้วยบัญชีเดโม"** ที่หน้าเข้าสู่ระบบ (`demo@petcare.app` / `demo1234`)
ครั้งแรกแอปจะสร้างบัญชีนี้ พร้อมสัตว์เลี้ยง 2 ตัวและนัดหมายตัวอย่างที่อิงจากวันที่ปัจจุบัน

## ทดสอบ

```bash
./gradlew testDebugUnitTest   # unit test: วันที่แบบไทย, สถานะนัด, เวลาแจ้งเตือน
./gradlew lintDebug
```

## ธีมและฟอนต์

- พื้นหลังเขียวเทาอ่อน `#EEF2EF`, แถบบนเขียวมรกตเข้ม `#0F4C4A`, accent อำพัน `#F2A93B`, สนิม `#B5532E` (เลยกำหนด) และรองรับ dark mode
- Fraunces และ Manrope ไม่มีอักษรไทย จึงใช้:
  - **Noto Serif Thai** สำหรับหัวข้อ (เซอริฟที่รองรับภาษาไทย)
  - **Fraunces** สำหรับโลโก้และตัวเลขใหญ่
  - **IBM Plex Sans Thai** สำหรับเนื้อหา
- ไฟล์ฟอนต์อยู่ใน `app/src/main/res/font/` ทั้งหมดใช้สัญญาอนุญาต SIL Open Font License
