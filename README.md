# مدیریت بدهی (Debt Manager)

اپلیکیشن اندروید برای مدیریت شخصی بدهی‌ها، اقساط، چک‌ها و تعهدات مالی — کاملاً آفلاین با ذخیره‌سازی محلی.

## ویژگی‌ها

- **وام‌ها:** ثبت وام، اقساط خودکار، پرداخت قسط، محاسبه باقی‌مانده
- **چک‌ها:** ثبت چک صادرشده، وضعیت (در انتظار / وصول‌شده / برگشتی / باطل)
- **بدهکاری‌ها:** بدهی به افراد و فروشگاه‌ها با پرداخت جزئی/کامل
- **اقساط دوره‌ای:** بیمه، اقساط فروشگاه و موارد مشابه
- **داشبورد:** خلاصه ماه، پرداخت‌های پیش‌رو و معوق
- **تاریخچه پرداخت‌ها:** با فیلتر و جستجو
- **یادآوری:** نوتیفیکیشن قبل از سررسید (۱، ۳ یا ۷ روز)
- **تنظیمات:** تم تاریک، PIN/اثرانگشت، پشتیبان‌گیری JSON

## الزامات

- Android 7.0+ (API 24)
- Android Studio Hedgehog (2023.1.1) یا جدیدتر
- JDK 17

## ساخت APK

### روش ۱: Android Studio (پیشنهادی)

1. پوشه پروژه را در Android Studio باز کنید: `e:\JAVA\Debt Manager`
2. صبر کنید تا Gradle Sync کامل شود
3. از منو: **Build → Build Bundle(s) / APK(s) → Build APK(s)**
4. فایل APK در مسیر زیر ساخته می‌شود:
   ```
   app\build\outputs\apk\debug\app-debug.apk
   ```

### روش ۲: خط فرمان

```powershell
cd "e:\JAVA\Debt Manager"

# اگر Android SDK نصب است، مسیر را در local.properties تنظیم کنید:
# sdk.dir=C\:\\Users\\YOUR_USER\\AppData\\Local\\Android\\Sdk

.\gradlew.bat assembleDebug
```

APK خروجی: `app\build\outputs\apk\debug\app-debug.apk`

برای نسخه Release:

```powershell
.\gradlew.bat assembleRelease
```

### نصب روی گوشی

1. APK را به گوشی منتقل کنید
2. در تنظیمات اندروید، «نصب از منابع ناشناخته» را فعال کنید
3. فایل APK را باز کرده و نصب کنید

## ساختار پروژه

```
app/src/main/java/com/debtmanager/app/
├── data/           # Room DB، Entity، DAO، Repository
├── ui/             # Compose UI، Theme، Screens
├── viewmodel/      # MainViewModel
├── worker/         # WorkManager یادآوری
├── security/       # PIN
└── util/           # تقویم شمسی، فرمت ریال
```

## واحد پول

تمام مبالغ به **ریال** نمایش داده می‌شوند (با جداکننده هزارگان فارسی).

## پشتیبان‌گیری

از **تنظیمات → خروجی JSON** فایل پشتیبان بگیرید و با **بازیابی از JSON** روی گوشی دیگر وارد کنید.

## حریم خصوصی

هیچ داده‌ای به سرور ارسال نمی‌شود. تمام اطلاعات فقط در پایگاه داده محلی (`debt_manager.db`) ذخیره می‌شود.
# DebtManager
