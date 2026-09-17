package com.missa.b360.ui.onboarding

import com.missa.b360.ui.icons.Iv
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.missa.b360.R
import com.missa.b360.core.domain.usecase.ValidatePinUseCase
import com.missa.b360.ui.components.MissaBrandMark
import com.missa.b360.ui.theme.BrandBlue
import com.missa.b360.ui.theme.MissaCanvas
import com.missa.b360.ui.theme.MissaInk
import com.missa.b360.ui.theme.MissaMuted
import com.missa.b360.ui.theme.MissaSurface
import com.missa.b360.ui.theme.MissaBorder
import com.missa.b360.ui.theme.Red40
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Verrou PIN à chaque ouverture (RA-01/RA-02) : 5 échecs → blocage croissant. */
@HiltViewModel
class PinLockViewModel @Inject constructor(
    private val validatePin: ValidatePinUseCase,
) : ViewModel() {

    data class UiState(
        val essaisRestants: Int? = null,
        val bloqueJusquA: Long? = null,
        val erreur: Boolean = false,
        val verificationEnCours: Boolean = false,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state

    val saisie = MutableStateFlow("")
    val deverrouille = MutableStateFlow(false)

    /**
     * Saisie d'un chiffre, puis vérification automatique au quatrième : le PIN
     * Missa vaut toujours quatre chiffres (RA-01, maquette), comme celui saisi
     * deux fois à l'onboarding.
     */
    fun ajouterChiffre(chiffre: Char) {
        if (!saisieAutorisee() || saisie.value.length >= PIN_LONGUEUR) return
        saisie.value += chiffre
        _state.value = _state.value.copy(erreur = false, essaisRestants = null)
        if (saisie.value.length == PIN_LONGUEUR) verifier()
    }

    fun effacer() {
        if (!saisieAutorisee()) return
        saisie.value = saisie.value.dropLast(1)
        _state.value = _state.value.copy(erreur = false, essaisRestants = null)
    }

    fun verifier() {
        if (!saisieAutorisee() || saisie.value.length != PIN_LONGUEUR) return
        val pin = saisie.value
        _state.value = _state.value.copy(
            erreur = false,
            essaisRestants = null,
            verificationEnCours = true,
        )
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

    private fun saisieAutorisee(): Boolean =
        _state.value.bloqueJusquA == null && !_state.value.verificationEnCours

    private suspend fun compterBloque(jusquA: Long) {
        while (System.currentTimeMillis() < jusquA) {
            delay(1_000)
        }
        _state.value = UiState()
    }

    companion object {
        /** Le PIN Missa compte toujours quatre chiffres (RA-01). */
        const val PIN_LONGUEUR = 4
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

    Surface(Modifier.fillMaxSize(), color = MissaCanvas) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            MissaBrandMarkDome()
            Spacer(Modifier.height(14.dp))
            Text(
                text = stringResource(R.string.lock_title),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MissaInk,
            )
            Spacer(Modifier.height(20.dp))

            // Quatre positions de saisie, comme le PIN créé à l'onboarding.
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                repeat(PinLockViewModel.PIN_LONGUEUR) { index ->
                    val rempli = index < saisie.length
                    val enErreur = state.erreur || state.bloqueJusquA != null
                    val couleur = when {
                        enErreur -> Red40
                        rempli -> BrandBlue
                        else -> Color.White
                    }
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .background(couleur, CircleShape)
                            .border(
                                width = if (rempli || enErreur) 0.dp else 1.5.dp,
                                color = if (enErreur) Red40 else MissaBorder,
                                shape = CircleShape,
                            ),
                    )
                }
            }

            Spacer(Modifier.height(18.dp))
            when {
                state.bloqueJusquA != null -> Text(
                    text = stringResource(R.string.lock_bloque),
                    color = Red40,
                    fontWeight = FontWeight.SemiBold,
                )
                state.erreur && state.essaisRestants != null -> Text(
                    text = stringResource(R.string.lock_essais_restants, state.essaisRestants ?: 0),
                    color = Red40,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.height(22.dp))

            Keypad(
                saisie = saisie,
                interactionActive = state.bloqueJusquA == null && !state.verificationEnCours,
                onDigit = viewModel::ajouterChiffre,
                onErase = viewModel::effacer,
            )
        }
    }
}

@Composable
private fun Keypad(
    saisie: String,
    interactionActive: Boolean,
    onDigit: (Char) -> Unit,
    onErase: () -> Unit,
) {
    val chiffresCompatibles = interactionActive && saisie.length < PinLockViewModel.PIN_LONGUEUR
    listOf("123", "456", "789").map { it.map(Char::toString) }.forEachIndexed { index, ligne ->
        if (index > 0) Spacer(Modifier.height(11.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            ligne.forEach { chiffre ->
                KeypadTouche(actif = chiffresCompatibles, onClick = { onDigit(chiffre[0]) }) {
                    Text(chiffre, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = MissaInk)
                }
            }
        }
    }
    Spacer(Modifier.height(11.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Spacer(Modifier.weight(1f))
        KeypadTouche(actif = chiffresCompatibles, onClick = { onDigit('0') }) {
            Text("0", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = MissaInk)
        }
        KeypadTouche(actif = interactionActive && saisie.isNotEmpty(), onClick = onErase) {
            Icon(
                imageVector = Iv.Backspace,
                contentDescription = null,
                tint = MissaMuted,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/** Touche du pavé : carré arrondi blanc, bord fin, légère élévation quand active. */
@Composable
private fun KeypadTouche(
    actif: Boolean,
    onClick: () -> Unit,
    contenu: @Composable () -> Unit,
) {
    Surface(
        onClick = onClick,
        enabled = actif,
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, MissaBorder),
        shadowElevation = if (actif) 2.dp else 0.dp,
        modifier = Modifier
            .size(64.dp)
            .alpha(if (actif) 1f else 0.4f),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            contenu()
        }
    }
}

/** Le logo de marque, posé dans un médaillon clair, au-dessus du pavé. */
@Composable
private fun MissaBrandMarkDome() {
    Surface(
        shape = CircleShape,
        color = MissaSurface,
        modifier = Modifier.size(84.dp),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            MissaBrandMark(size = 52.dp)
        }
    }
}

