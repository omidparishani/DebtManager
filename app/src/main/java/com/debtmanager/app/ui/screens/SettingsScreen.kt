package com.debtmanager.app.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import com.debtmanager.app.ui.components.ConfirmDialog
import com.debtmanager.app.ui.components.SecondaryTopBar
import com.debtmanager.app.ui.navigation.Screen
import com.debtmanager.app.util.NotificationSoundHelper
import com.debtmanager.app.util.PersianDateUtil
import com.debtmanager.app.viewmodel.MainViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: MainViewModel, navController: NavController) {
    val context = LocalContext.current
    val darkMode by viewModel.darkMode.collectAsState()
    val reminderDays by viewModel.reminderDays.collectAsState()
    val reminderHour by viewModel.reminderHour.collectAsState()
    val notificationSound by viewModel.notificationSound.collectAsState()
    val vibrationEnabled by viewModel.vibrationEnabled.collectAsState()
    val remindOnDueDay by viewModel.remindOnDueDay.collectAsState()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsState()
    val pinEnabled by viewModel.pinEnabled.collectAsState()
    val biometricEnabled by viewModel.biometricEnabled.collectAsState()

    var showPinDialog by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }
    val snackbarHost = remember { SnackbarHostState() }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            try {
                val input = context.contentResolver.openInputStream(it)
                val tempFile = File(context.cacheDir, "import_backup.json")
                input?.use { stream -> tempFile.outputStream().use { out -> stream.copyTo(out) } }
                viewModel.importBackup(tempFile) { success ->
                    snackbarMessage = if (success) "بازیابی با موفقیت انجام شد" else "خطا در بازیابی"
                }
            } catch (e: Exception) {
                snackbarMessage = "خطا: ${e.message}"
            }
        }
    }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHost.showSnackbar(it)
            snackbarMessage = null
        }
    }

    Scaffold(
        topBar = {
            SecondaryTopBar(
                title = "تنظیمات",
                onBack = { navController.popBackStack() }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHost) }
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SettingsSection("ظاهر") {
                SettingsSwitchRow("تم تاریک", darkMode) { viewModel.setDarkMode(it) }
            }

            SettingsSection("اعلان‌ها") {
                SettingsSwitchRow("فعال‌سازی اعلان‌ها", notificationsEnabled) {
                    viewModel.setNotificationsEnabled(it)
                }
                SettingsSwitchRow("لرزش هنگام اعلان", vibrationEnabled, enabled = notificationsEnabled) {
                    viewModel.setVibrationEnabled(it)
                }
                SettingsSwitchRow("یادآوری در روز سررسید", remindOnDueDay, enabled = notificationsEnabled) {
                    viewModel.setRemindOnDueDay(it)
                }

                Text("چند روز قبل از سررسید:", style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(1, 3, 7, 14).forEach { days ->
                        FilterChip(
                            selected = reminderDays == days,
                            onClick = { viewModel.setReminderDays(days) },
                            label = { Text("${PersianDateUtil.toPersianDigits(days)} روز") },
                            enabled = notificationsEnabled
                        )
                    }
                }

                Text("ساعت ارسال یادآوری:", style = MaterialTheme.typography.bodyMedium)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf(7, 9, 12, 18, 21).forEach { hour ->
                        FilterChip(
                            selected = reminderHour == hour,
                            onClick = { viewModel.setReminderHour(hour) },
                            label = { Text("${PersianDateUtil.toPersianDigits(hour)}:۰۰") },
                            enabled = notificationsEnabled
                        )
                    }
                }

                Text("صدای هشدار:", style = MaterialTheme.typography.bodyMedium)
                NotificationSoundHelper.options.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { sound ->
                            FilterChip(
                                selected = notificationSound == sound.id,
                                onClick = { viewModel.setNotificationSound(sound.id) },
                                label = { Text(sound.label) },
                                modifier = Modifier.weight(1f),
                                enabled = notificationsEnabled
                            )
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }

            SettingsSection("امنیت") {
                SettingsSwitchRow("قفل PIN", pinEnabled) {
                    if (it) showPinDialog = true else viewModel.disablePin()
                }
                SettingsSwitchRow("اثرانگشت", biometricEnabled, enabled = pinEnabled) {
                    viewModel.setBiometric(it)
                }
                if (pinEnabled) {
                    TextButton(onClick = { showPinDialog = true }) { Text("تغییر PIN") }
                }
            }

            SettingsSection("پروفایل") {
                OutlinedButton(
                    onClick = { navController.navigate(Screen.Profile.route) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("ویرایش پروفایل و نام اعلان‌ها") }
            }

            SettingsSection("پشتیبان‌گیری") {
                Button(
                    onClick = {
                        viewModel.exportBackup { file ->
                            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "application/json"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(intent, "خروجی پشتیبان"))
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("خروجی JSON") }

                OutlinedButton(
                    onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("بازیابی از JSON") }
            }

            SettingsSection("خطرناک", titleColor = MaterialTheme.colorScheme.error) {
                OutlinedButton(
                    onClick = { showClearConfirm = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("پاک کردن کل دیتابیس") }
            }

            Text("نسخه ۱.۰.۰", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("تمام داده‌ها فقط روی دستگاه شما ذخیره می‌شود.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }

    if (showClearConfirm) {
        ConfirmDialog(
            title = "پاک کردن همه داده‌ها",
            message = "تمام وام‌ها، چک‌ها، بدهی‌ها، اقساط دوره‌ای و تاریخچه پرداخت حذف می‌شود. این عمل غیرقابل بازگشت است.",
            confirmLabel = "بله، پاک شود",
            onConfirm = {
                viewModel.clearAllData {
                    showClearConfirm = false
                    snackbarMessage = "دیتابیس با موفقیت پاک شد"
                }
            },
            onDismiss = { showClearConfirm = false }
        )
    }

    if (showPinDialog) {
        SetPinDialog(
            onDismiss = { showPinDialog = false },
            onConfirm = { pin ->
                viewModel.setPin(pin)
                showPinDialog = false
            }
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    titleColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = titleColor)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    label: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label)
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}

@Composable
fun SetPinDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var pin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تنظیم PIN") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(pin, { pin = it }, label = { Text("PIN (۴ رقم)") }, singleLine = true)
                OutlinedTextField(confirm, { confirm = it }, label = { Text("تکرار PIN") }, singleLine = true)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                when {
                    pin.length < 4 -> error = "PIN باید حداقل ۴ رقم باشد"
                    pin != confirm -> error = "PIN مطابقت ندارد"
                    else -> onConfirm(pin)
                }
            }) { Text("ذخیره") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}
