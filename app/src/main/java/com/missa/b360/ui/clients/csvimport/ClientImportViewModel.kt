package com.missa.b360.ui.clients.csvimport

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.missa.b360.core.domain.model.ClientCsvImport
import com.missa.b360.core.domain.model.ClientImportPreview
import com.missa.b360.core.domain.usecase.ClientDefaultsUseCase
import com.missa.b360.core.domain.usecase.ClientImportReport
import com.missa.b360.core.domain.usecase.ClientImportUseCase
import com.missa.b360.core.util.Iso4217
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class ImportEtape { CHOIX, ANALYSE, APERCU, IMPORT, TERMINE }

@Immutable
data class ClientImportUiState(
    val etape: ImportEtape = ImportEtape.CHOIX,
    val apercu: ClientImportPreview = ClientImportPreview(),
    val fait: Int = 0,
    val total: Int = 0,
    val rapport: ClientImportReport? = null,
    val lectureImpossible: Boolean = false,
)

/**
 * Import CSV en trois temps : lecture et validation (rien n'est écrit), aperçu avec rapport
 * d'erreurs, puis création des seules lignes valides après confirmation.
 */
@HiltViewModel
class ClientImportViewModel @Inject constructor(
    @ApplicationContext private val contexte: Context,
    private val defaults: ClientDefaultsUseCase,
    private val importer: ClientImportUseCase,
) : ViewModel() {

    private val _etat = MutableStateFlow(ClientImportUiState())
    val etat: StateFlow<ClientImportUiState> = _etat.asStateFlow()

    fun choisirFichier(uri: Uri) {
        _etat.value = ClientImportUiState(etape = ImportEtape.ANALYSE)
        viewModelScope.launch {
            try {
                val indicatif = defaults().codePays?.let { Iso4217.indicatifTelephone(it) }
                val apercu = withContext(Dispatchers.IO) {
                    val texte = lire(uri)
                    if (texte == null) null else ClientCsvImport.analyser(texte, indicatif)
                }
                _etat.value = if (apercu == null) {
                    ClientImportUiState(lectureImpossible = true)
                } else {
                    ClientImportUiState(etape = ImportEtape.APERCU, apercu = apercu)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _etat.value = ClientImportUiState(lectureImpossible = true)
            }
        }
    }

    fun confirmer() {
        val lignes = _etat.value.apercu.valides
        if (lignes.isEmpty() || _etat.value.etape != ImportEtape.APERCU) return
        _etat.update { it.copy(etape = ImportEtape.IMPORT, fait = 0, total = lignes.size) }
        viewModelScope.launch {
            val rapport = try {
                importer(lignes) { fait, total -> _etat.update { it.copy(fait = fait, total = total) } }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                ClientImportReport(crees = 0, doublons = 0, echecs = lignes.size, interrompu = true)
            }
            _etat.update { it.copy(etape = ImportEtape.TERMINE, rapport = rapport) }
        }
    }

    fun recommencer() {
        _etat.value = ClientImportUiState()
    }

    private fun lire(uri: Uri): String? =
        contexte.contentResolver.openInputStream(uri)?.use { flux ->
            val octets = flux.readBytes()
            if (octets.size > TAILLE_MAX) null else String(octets, Charsets.UTF_8)
        }

    private companion object {
        const val TAILLE_MAX = 5 * 1024 * 1024
    }
}
