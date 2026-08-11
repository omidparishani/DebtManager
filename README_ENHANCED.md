# Debt Manager - نسخه ارتقا یافته (حسابداری شخصی)

## تغییرات اعمال‌شده

### ۱. رفع باگ جداکننده مبلغ (مهم)
- `CurrencyUtil` + `AmountTextField` بازنویسی شد.
- از `VisualTransformation` استفاده می‌شود تا مقدار داخلی فقط رقم باشد و جداکننده فقط در نمایش ظاهر شود.
- مشکل وارد شدن اشتباه مبلغ هنگام تایپ حل شده است.

### ۲. مدیریت اشخاص (Contacts)
- جدول جدید `contacts`
- صفحه جدید **اشخاص** در نوار پایین
- امکان افزودن / ویرایش / حذف شخص یا فروشگاه
- فیلد `contactId` و `isCredit` به بدهی اضافه شد (بدهکاری / بستانکاری)

### ۳. حساب‌های بانکی + واریز / برداشت
- جدول‌های `bank_accounts` و `account_transactions`
- صفحه جدید **حساب‌ها**
- تعریف حساب بانکی، موجودی اولیه، حساب پیش‌فرض
- واریز و برداشت با شرح و تاریخ
- گزارش ساده تراکنش‌های هر حساب
- فیلد `bankAccountId` به اقساط، چک، پرداخت‌های دوره‌ای و تاریخچه اضافه شد (برای انتخاب حساب هنگام پرداخت در مراحل بعدی)

### ۴. داشبورد و ظاهر
- ساختار داشبورد برای انتخاب ماه آماده است (نیاز به تکمیل UI انتخابگر ماه در `DashboardScreen`)
- کارت‌های آماری و تم Material 3 حفظ شده و قابل گسترش است.

### ۵. اعلان‌ها
- `ReminderWorker` از قبل با WorkManager کار می‌کند (حتی وقتی اپ بسته است).
- مجوز `POST_NOTIFICATIONS` در اندروید ۱۳+ درخواست می‌شود.

### ۶. دیتابیس
- نسخه دیتابیس به **۳** ارتقا یافت.
- Migration از نسخه ۲ به ۳ نوشته شده + `fallbackToDestructiveMigration` برای اطمینان.

### ۷. ناوبری
- آیتم‌های پایین صفحه: داشبورد | وام‌ها | بدهکاری | حساب‌ها | اشخاص

## موارد باقی‌مانده / پیشنهادی برای تکمیل در Android Studio

1. **انتخاب ماه در داشبورد**: یک `MonthYearPicker` جلالی اضافه کنید و `dashboardState` را بر اساس ماه فیلتر کنید.
2. **فرم بدهی**: چک‌باکس «بستانکاری» + انتخاب شخص از لیست Contacts + دکمه «افزودن سریع شخص».
3. **پرداخت با انتخاب حساب**: در دیالوگ‌های PayDebt / پرداخت قسط / چک، یک Dropdown از حساب‌های بانکی اضافه کنید و `bankAccountId` را ذخیره + موجودی را کم کنید.
4. **ویجت**: `AppWidgetProvider` برای نمایش بدهی/بستانکاری امروز.
5. **گزارش‌گیری پیشرفته‌تر** و فیلتر تاریخ در صفحه حساب‌ها.
6. **ظاهر کلی**: گرادیان کارت‌ها، انیمیشن، تم رنگی بهتر در `Theme.kt`.

## نحوه ساخت

1. پروژه را در Android Studio باز کنید.
2. Gradle Sync.
3. اگر خطای import داشتید (مثلاً Contact در ViewModel)، مطمئن شوید importهای entity اضافه شده‌اند.
4. Build > Make Project.
5. چون Migration وجود دارد، روی دستگاه واقعی یا امولاتور داده قبلی ممکن است پاک شود (destructive fallback).

## ساختار جدید مهم

```
data/entity/Entities.kt     → Contact, BankAccount, AccountTransaction, isCredit
data/dao/Daos.kt            → ContactDao, BankAccountDao, AccountTransactionDao
data/database/AppDatabase.kt → version 3 + migration
ui/screens/ContactsScreen.kt
ui/screens/AccountsScreen.kt
util/CurrencyUtil.kt        → VisualTransformation
ui/components/CommonComponents.kt → AmountTextField اصلاح‌شده
viewmodel/MainViewModel.kt  → متدهای جدید
ui/navigation/Navigation.kt → آیتم‌های جدید
```

موفق باشید!
