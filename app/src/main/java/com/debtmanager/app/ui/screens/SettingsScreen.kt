package com.debtmanager.app.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import com.debtmanager.app.ui.components.ConfirmDialog
import com.debtmanager.app.viewmodel.MainViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: MainViewModel, navController: NavController) {
    val context = LocalContext.current
    val darkMode by viewModel.darkMode.collectAsState()
    val reminderDays by viewModel.reminderDays.collectAsState()
    val pinEnabled by viewModel.pinEnabled.collectAsState()
    val biometricEnabled by viewModel.biometricEnabled.collectAsState()

    var showPinDialog by remember { mutableStateOf(false) }
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
            TopAppBar(
                title = { Text("تنظیمات") },
                navigationIcon = {
                    TextButton(onClick = { navController.popBackStack() }) { Text("بازگشت") }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHost) }
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("ظاهر", style = MaterialTheme.typography.titleMedium)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("تم تاریک")
                Switch(checked = darkMode, onCheckedChange = { viewModel.setDarkMode(it) })
            }

            HorizontalDivider()
            Text("یادآوری", style = MaterialTheme.typography.titleMedium)
            Text("چند روز قبل از سررسید یادآوری شود:")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(1, 3, 7).forEach { days ->
                    FilterChip(
                        selected = reminderDays == days,
                        onClick = { viewModel.setReminderDays(days) },
                        label = { Text("${com.debtmanager.app.util.PersianDateUtil.toPersianDigits(days)} روز") }
                    )
                }
            }

            HorizontalDivider()
            Text("امنیت", style = MaterialTheme.typography.titleMedium)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("قفل PIN")
                Switch(checked = pinEnabled, onCheckedChange = {
                    if (it) showPinDialog = true else viewModel.disablePin()
                })
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("اثرانگشت")
                Switch(
                    checked = biometricEnabled,
                    onCheckedChange = { viewModel.setBiometric(it) },
                    enabled = pinEnabled
                )
            }
            if (pinEnabled) {
                TextButton(onClick = { showPinDialog = true }) { Text("تغییر PIN") }
            }

            HorizontalDivider()
            Text("پروفایل", style = MaterialTheme.typography.titleMedium)
            OutlinedButton(
                onClick = { navController.navigate(com.debtmanager.app.ui.navigation.Screen.Profile.route) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("ویرایش پروفایل و اعلان‌ها") }

            HorizontalDivider()
            Text("پشتیبان‌گیری", style = MaterialTheme.typography.titleMedium)
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

            HorizontalDivider()
            Text("خطرناک", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
            var showClearConfirm by remember { mutableStateOf(false) }
            OutlinedButton(
                onClick = { showClearConfirm = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) { Text("پاک کردن کل دیتابیس") }

            if (showClearConfirm) {
                ConfirmDialog(
                    title = "پاک کردن همه داده‌ها",
                    message = "تمام وام‌ها، چک‌ها، بدهی‌ها، اقساط دوره‌ای و تاریخچه پرداخت حذف می‌شود. این عمل غیرقابل بازگشت است. ادامه می‌دهید؟",
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

            HorizontalDivider()
            Text("مدیریت بدهی", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("نسخه ۱.۰.۰", style = MaterialTheme.typography.bodySmall)
            Text("تمام داده‌ها فقط روی دستگاه شما ذخیره می‌شود.", style = MaterialTheme.typography.bodySmall)
        }
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
