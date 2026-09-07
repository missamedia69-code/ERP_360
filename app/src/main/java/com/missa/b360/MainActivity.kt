package com.missa.b360

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.missa.b360.core.data.datastore.SettingsStore
import com.missa.b360.core.util.FormatPrefs
import com.missa.b360.ui.navigation.AppNavHost
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        applyStoredLocale()
        applyStoredFormats()
        setContent {
            Erp360Theme {
                // L'introduction est décidée par le graphe de navigation, qui
                // sait si l'installation est déjà configurée. La jouer ici la
                // relançait à chaque recréation d'activité.
                AppNavHost()
            }
        }
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

    /** Charge les préférences d'affichage (fuseau, format date, nombres) pour toute l'app. */
    private fun applyStoredFormats() {
        val result = runBlocking {
            withTimeoutOrNull(2_000) {
                listOf(
                    settingsStore.observe(SettingsStore.Keys.FUSEAU_HORAIRE).first(),
                    settingsStore.observe(SettingsStore.Keys.FORMAT_DATE).first(),
                    settingsStore.observe(SettingsStore.Keys.FORMAT_NOMBRES).first(),
                )
            }
        } ?: return
        FormatPrefs.appliquer(result[0], result[1], result[2])
    }

    private companion object {
    }
}
