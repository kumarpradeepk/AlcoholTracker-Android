package com.mtss.alcoholtracker

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import com.mtss.alcoholtracker.ui.AppViewModel
import com.mtss.alcoholtracker.ui.Root

class MainActivity : FragmentActivity() {

    private lateinit var vm: AppViewModel

    private val notifPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> vm.onNotifPermissionResult(granted) }

    private val exportBackup = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? -> uri?.let { vm.onExportDocumentPicked(it) } }

    private val importBackup = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? -> uri?.let { vm.onImportDocumentPicked(it) } }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        vm = ViewModelProvider(this, AppViewModel.factory(application))[AppViewModel::class.java]
        vm.hostActions = object : AppViewModel.HostActions {
            override fun requestNotificationPermission() {
                if (Build.VERSION.SDK_INT >= 33 &&
                    ContextCompat.checkSelfPermission(
                        this@MainActivity, Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    vm.onNotifPermissionResult(true)
                }
            }

            override fun launchExportDocument(suggestedName: String) =
                exportBackup.launch(suggestedName)

            override fun launchImportDocument() =
                importBackup.launch(arrayOf("application/json", "text/plain", "*/*"))

            override fun shareIntent(intent: Intent) =
                startActivity(Intent.createChooser(intent, null))

            override fun openUrl(url: String) {
                runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
            }

            override fun openLanguageSettings() {
                val intent = if (Build.VERSION.SDK_INT >= 33) {
                    Intent(android.provider.Settings.ACTION_APP_LOCALE_SETTINGS)
                        .setData(Uri.fromParts("package", packageName, null))
                } else {
                    Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        .setData(Uri.fromParts("package", packageName, null))
                }
                runCatching { startActivity(intent) }
            }

            override fun contactSupport(customerId: String) {
                val body = "\n\n—\nCustomer ID: $customerId\nApp 1.0.0 · Android ${Build.VERSION.RELEASE} · ${Build.MANUFACTURER} ${Build.MODEL}"
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:")
                    putExtra(Intent.EXTRA_EMAIL, arrayOf("support@alcoholtracker.app"))
                    putExtra(Intent.EXTRA_SUBJECT, getString(R.string.support_email_subject))
                    putExtra(Intent.EXTRA_TEXT, body)
                }
                runCatching { startActivity(intent) }
            }

            override fun applyIcon(index: Int) = switchLauncherIcon(index)

            override fun showBiometricPrompt() = authenticate()
        }

        setContent { Root(vm) }
    }

    override fun onStart() {
        super.onStart()
        if (::vm.isInitialized) vm.onAppForegrounded()
    }

    private fun authenticate() {
        val manager = BiometricManager.from(this)
        val allowed = BiometricManager.Authenticators.BIOMETRIC_WEAK or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL
        if (manager.canAuthenticate(allowed) != BiometricManager.BIOMETRIC_SUCCESS) {
            // No credential set up on this device — never lock the user out of
            // their own diary.
            vm.onUnlocked()
            return
        }
        val prompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    vm.onUnlocked()
                }
            }
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.applock_prompt_title))
            .setSubtitle(getString(R.string.applock_prompt_sub))
            .setAllowedAuthenticators(allowed)
            .build()
        prompt.authenticate(info)
    }

    private fun switchLauncherIcon(index: Int) {
        val aliases = listOf("IconDefault", "IconGift", "IconHoliday")
        aliases.forEachIndexed { i, alias ->
            packageManager.setComponentEnabledSetting(
                ComponentName(this, "$packageName.$alias"),
                if (i == index) PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
        }
    }
}
