package com.missa.b360

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.missa.b360.core.data.datastore.SettingsStore
import com.missa.b360.core.data.seed.DemoDataSeeder
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

    @Inject
    lateinit var demoDataSeeder: DemoDataSeeder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Ne plus bloquer le thread principal : charge en arrière-plan
        lifecycleScope.launch {
            demoDataSeeder.seedIfEmpty()
            applyStoredLocaleAsync()
            applyStoredFormatsAsync()
        }
        setContent {
            Erp360Theme {
                AppNavHost()
            }
        }
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
