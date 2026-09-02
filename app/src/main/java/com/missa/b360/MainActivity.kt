package com.missa.b360

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.missa.b360.core.data.datastore.SettingsStore
import com.missa.b360.ui.navigation.AppNavHost
import com.missa.b360.ui.screens.SplashVideoScreen
import com.missa.b360.ui.theme.Erp360Theme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var settingsStore: SettingsStore

    /**
     * État de secours pour qu'une recréation exceptionnelle ne relance pas
     * l'introduction vidéo au milieu de l'onboarding ou des réglages.
     */
    private var introTerminee = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        introTerminee = savedInstanceState?.getBoolean(STATE_INTRO_TERMINEE, false) ?: false
        enableEdgeToEdge()
        applyStoredLocale()
        setContent {
            Erp360Theme {
                var showSplashVideo by remember { mutableStateOf(!introTerminee) }
                if (showSplashVideo) {
                    SplashVideoScreen(
                        onFinished = {
                            introTerminee = true
                            showSplashVideo = false
                        },
                    )
                } else {
                    AppNavHost()
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(STATE_INTRO_TERMINEE, introTerminee)
        super.onSaveInstanceState(outState)
    }

    /** Applique au démarrage la langue déjà enregistrée lorsqu'elle diffère réellement. */
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

    private companion object {
        const val STATE_INTRO_TERMINEE = "intro_terminee"
    }
}
