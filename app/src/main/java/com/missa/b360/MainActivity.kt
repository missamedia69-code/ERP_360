package com.missa.b360

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.missa.b360.core.data.datastore.SettingsStore
import com.missa.b360.ui.navigation.AppNavHost
import com.missa.b360.ui.screens.SplashVideoScreen
import com.missa.b360.ui.theme.Erp360Theme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var settingsStore: SettingsStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        applyStoredLocale()
        setContent {
            Erp360Theme {
                // Vidéo d'intro jouée juste après le splash système Android,
                // puis bascule vers la coquille de navigation principale.
                var showSplashVideo by rememberSaveable { mutableStateOf(true) }
                if (showSplashVideo) {
                    SplashVideoScreen(onFinished = { showSplashVideo = false })
                } else {
                    AppNavHost()
                }
            }
        }
    }

    /**
     * Applique la langue choisie à l'onboarding (RA-12 — 5 langues, arabe RTL).
     * AppCompatDelegate déclenche une recréation d'activité si la locale change.
     */
    private fun applyStoredLocale() {
        val stored = runBlocking {
            withTimeoutOrNull(2_000) {
                settingsStore.observe(SettingsStore.Keys.LANGUE).first()
            }
        } ?: return
        val current = androidx.appcompat.app.AppCompatDelegate
            .getApplicationLocales().toLanguageTags()
        if (stored.isNotEmpty() && current != stored) {
            androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(
                androidx.core.os.LocaleListCompat.forLanguageTags(stored),
            )
        }
    }
}