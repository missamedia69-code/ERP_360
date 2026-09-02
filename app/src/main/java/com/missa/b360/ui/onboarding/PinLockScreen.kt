package com.missa.b360.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.missa.b360.R
import com.missa.b360.core.domain.usecase.ValidatePinUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Verrou PIN à chaque ouverture (RA-01/RA-02) : 5 échecs → blocage croissant. */
@HiltViewModel
class PinLockViewModel @Inject constructor(
    private val validatePin: ValidatePinUseCase,
) : ViewModel() {

    data class UiState(
        val essaisRestants: Int? = null,
        val bloqueJusquA: Long? = null,
        val erreur: Boolean = false,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state

    val saisie = MutableStateFlow("")
    val deverrouille = MutableStateFlow(false)

    fun ajouterChiffre(chiffre: Char) {
        if (_state.value.bloqueJusquA != null) return
        if (saisie.value.length >= 6) return
        saisie.value += chiffre
        if (saisie.value.length in 4..6) verifier()
    }

    fun effacer() {
        saisie.value = saisie.value.dropLast(1)
    }

    fun verifier() {
        val pin = saisie.value
        viewModelScope.launch {
            when (val resultat = validatePin(pin)) {
                is ValidatePinUseCase.Outcome.AccesAccorde,
                is ValidatePinUseCase.Outcome.PinNonConfigure,
                -> deverrouille.value = true
                is ValidatePinUseCase.Outcome.Refuse -> {
                    saisie.value = ""
                    _state.value = UiState(essaisRestants = resultat.essaisRestants, erreur = true)
                }
                is ValidatePinUseCase.Outcome.Bloque -> {
                    saisie.value = ""
                    _state.value = UiState(bloqueJusquA = resultat.jusquA)
                    compterBloque(resultat.jusquA)
                }
            }
        }
    }

    private suspend fun compterBloque(jusquA: Long) {
        while (System.currentTimeMillis() < jusquA) {
            delay(1_000)
        }
        _state.value = UiState()
    }
}

/** Écran de verrouillage PIN — demandé à chaque ouverture de l'app (RA-01). */
@Composable
fun PinLockScreen(
    onUnlocked: () -> Unit,
    viewModel: PinLockViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val saisie by viewModel.saisie.collectAsState()
    val deverrouille by viewModel.deverrouille.collectAsState()

    LaunchedEffect(deverrouille) {
        if (deverrouille) onUnlocked()
    }

    Surface(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                stringResource(R.string.lock_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(24.dp))

            // Points de saisie (6 positions max)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(6) { index ->
                    val rempli = index < saisie.length
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .background(
                                color = if (rempli) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                },
                                shape = CircleShape,
                            ),
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            when {
                state.bloqueJusquA != null -> Text(
                    stringResource(R.string.lock_bloque),
                    color = MaterialTheme.colorScheme.error,
                )
                state.erreur && state.essaisRestants != null -> Text(
                    stringResource(R.string.lock_essais_restants, state.essaisRestants ?: 0),
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Spacer(Modifier.height(24.dp))

            Keypad(viewModel)
        }
    }
}

@Composable
private fun Keypad(viewModel: PinLockViewModel) {
    val lignes = listOf(
        listOf('1', '2', '3'),
        listOf('4', '5', '6'),
        listOf('7', '8', '9'),
    )
    lignes.forEach { ligne ->
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            ligne.forEach { chiffre ->
                KeypadButton(chiffre.toString()) { viewModel.ajouterChiffre(chiffre) }
            }
        }
        Spacer(Modifier.height(12.dp))
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(Modifier.size(72.dp))
        KeypadButton("0") { viewModel.ajouterChiffre('0') }
        OutlinedButton(
            onClick = viewModel::effacer,
            modifier = Modifier.size(72.dp),
        ) {
            Text("⌫")
        }
    }
    Spacer(Modifier.height(24.dp))
    Button(
        onClick = viewModel::verifier,
        enabled = viewModel.saisie.value.length in 4..6,
    ) {
        Text(stringResource(R.string.lock_deverrouiller))
    }
}

@Composable
private fun KeypadButton(label: String, onClick: () -> Unit) {
    FilledTonalButton(onClick = onClick, modifier = Modifier.size(72.dp)) {
        Text(label, style = MaterialTheme.typography.titleLarge)
    }
}

