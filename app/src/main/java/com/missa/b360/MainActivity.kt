package com.missa.b360

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.missa.b360.core.data.datastore.SettingsStore
import com.missa.b360.core.util.FormatPrefs
import com.missa.b360.ui.navigation.AppNavHost
import com.missa.b360.ui.theme.Erp360Theme
import dagger.hilt.android.AndroidEntryPoint
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var settingsStore: SettingsStore

    private var notificationRoute by mutableStateOf<String?>(null)
    private val permissionNotifications = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* Les notifications internes restent actives même en cas de refus. */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        notificationRoute = intent.getStringExtra(com.missa.b360.core.notifications.NotificationRoutes.EXTRA_ROUTE)
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            permissionNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        // Ne plus bloquer le thread principal : charge en arrière-plan
        lifecycleScope.launch {
            applyStoredLocaleAsync()
            applyStoredFormatsAsync()
        }
        setContent {
            Erp360Theme {
                AppNavHost(
                    notificationRoute = notificationRoute,
                    onNotificationRouteConsumed = { notificationRoute = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        notificationRoute = intent.getStringExtra(com.missa.b360.core.notifications.NotificationRoutes.EXTRA_ROUTE)
    }

    private suspend fun applyStoredLocaleAsync() {
        val stored = withTimeoutOrNull(2_000) {
            settingsStore.observe(SettingsStore.Keys.LANGUE).first()
        } ?: return
        val current = androidx.appcompat.app.AppCompatDelegate
            .getApplicationLocales().toLanguageTags()
        if (stored.isNotEmpty() && current != stored) {
            androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(
                androidx.core.os.LocaleListCompat.forLanguageTags(stored),
            )
        }
    }

    private suspend fun applyStoredFormatsAsync() {
        val result = withTimeoutOrNull(2_000) {
            listOf(
                settingsStore.observe(SettingsStore.Keys.FUSEAU_HORAIRE).first(),
                settingsStore.observe(SettingsStore.Keys.FORMAT_DATE).first(),
                settingsStore.observe(SettingsStore.Keys.FORMAT_NOMBRES).first(),
            )
        } ?: return
        FormatPrefs.appliquer(result[0], result[1], result[2])
    }
}
