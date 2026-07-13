package com.debtmanager.app.ui.screens

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.debtmanager.app.security.PinManager
import com.debtmanager.app.viewmodel.MainViewModel

@Composable
fun LockScreen(viewModel: MainViewModel, onUnlocked: () -> Unit) {
    val context = LocalContext.current
    val pinHash by viewModel.pinHash.collectAsState()
    val biometricEnabled by viewModel.biometricEnabled.collectAsState()
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(biometricEnabled) {
        if (biometricEnabled && context is FragmentActivity) {
            val biometricManager = BiometricManager.from(context)
            if (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS) {
                val executor = ContextCompat.getMainExecutor(context)
                val prompt = BiometricPrompt(context, executor, object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        onUnlocked()
                    }
                })
                prompt.authenticate(BiometricPrompt.PromptInfo.Builder()
                    .setTitle("باز کردن قفل")
                    .setSubtitle("اثرانگشت خود را اسکن کنید")
                    .setNegativeButtonText("استفاده از PIN")
                    .build())
            }
        }
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Text("مدیریت بدهی", style = MaterialTheme.typography.headlineMedium)
            Text("برای ورود PIN خود را وارد کنید")
            OutlinedTextField(
                value = pin,
                onValueChange = { if (it.length <= 8) pin = it },
                label = { Text("PIN") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                singleLine = true
            )
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(onClick = {
                val hash = pinHash
                if (hash != null && PinManager.verifyPin(pin, hash)) {
                    onUnlocked()
                } else {
                    error = "PIN اشتباه است"
                    pin = ""
                }
            }) { Text("ورود") }
        }
    }
}
