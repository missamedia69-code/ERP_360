package com.missa.b360.ui.onboarding

import com.missa.b360.ui.icons.Iv
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
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
import com.missa.b360.ui.theme.MissaBorder
import com.missa.b360.ui.theme.MissaCanvas
import com.missa.b360.ui.theme.MissaInk
import com.missa.b360.ui.theme.MissaMuted
import com.missa.b360.ui.theme.MissaSoftBlue
import com.missa.b360.ui.theme.MissaSurface
import com.missa.b360.ui.theme.Red20
import com.missa.b360.ui.theme.Red80
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
     * Missa vaut toujours quatre chiffres (RA-01), comme celui saisi deux fois
     * à l'onboarding.
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
    val descriptionEffacer = stringResource(R.string.obn_pin_effacer)

    LaunchedEffect(deverrouille) {
        if (deverrouille) onUnlocked()
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MissaCanvas) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(MissaSoftBlue, MissaCanvas, MissaCanvas),
                    ),
                )
                .safeDrawingPadding(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = maxHeight)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                LockBrandHeader()
                Spacer(Modifier.height(14.dp))
                LockPinCard(state = state, saisie = saisie)
                Spacer(Modifier.height(12.dp))
                Keypad(
                    saisie = saisie,
                    interactionActive = state.bloqueJusquA == null && !state.verificationEnCours,
                    descriptionEffacer = descriptionEffacer,
                    onDigit = viewModel::ajouterChiffre,
                    onErase = viewModel::effacer,
                )
            }
        }
    }
}

/** Le logo Missa reste carré à bords arrondis, jamais circulaire. */
@Composable
private fun LockBrandHeader() {
    val forme = RoundedCornerShape(20.dp)
    Row(
        modifier = Modifier
            .widthIn(max = 340.dp)
            .fillMaxWidth()
            .clip(forme)
            .background(MissaSurface)
            .border(1.dp, MissaBorder.copy(alpha = 0.65f), forme)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MissaSurface,
            border = BorderStroke(1.dp, MissaBorder.copy(alpha = 0.7f)),
            shadowElevation = 2.dp,
        ) {
            Box(
                modifier = Modifier.padding(7.dp),
                contentAlignment = Alignment.Center,
            ) {
                MissaBrandMark(size = 48.dp)
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = stringResource(R.string.app_name),
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = MissaInk,
        )
    }
}

/** Carte blanche de saisie : titre localisé, quatre cases lisibles et état. */
@Composable
private fun LockPinCard(
    state: PinLockViewModel.UiState,
    saisie: String,
) {
    val forme = RoundedCornerShape(22.dp)
    Surface(
        modifier = Modifier
            .widthIn(max = 340.dp)
            .fillMaxWidth(),
        shape = forme,
        color = MissaSurface,
        border = BorderStroke(1.dp, MissaBorder.copy(alpha = 0.75f)),
        shadowElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.lock_title),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MissaInk,
            )
            Spacer(Modifier.height(13.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                repeat(PinLockViewModel.PIN_LONGUEUR) { index ->
                    if (index > 0) Spacer(Modifier.width(11.dp))
                    LockPinSlot(
                        filled = index < saisie.length,
                        error = state.erreur || state.bloqueJusquA != null,
                    )
                }
            }

            when {
                state.bloqueJusquA != null -> {
                    Spacer(Modifier.height(11.dp))
                    LockStateMessage(
                        text = stringResource(R.string.lock_bloque),
                        blocked = true,
                    )
                }
                state.erreur && state.essaisRestants != null -> {
                    Spacer(Modifier.height(11.dp))
                    LockStateMessage(
                        text = stringResource(
                            R.string.lock_essais_restants,
                            state.essaisRestants,
                        ),
                        blocked = true,
                    )
                }
                state.verificationEnCours -> {
                    Spacer(Modifier.height(11.dp))
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = BrandBlue,
                        strokeWidth = 2.dp,
                    )
                }
            }
        }
    }
}

/** Case de saisie rectangulaire arrondie : chiffre masqué, marque bleue, erreur rouge. */
@Composable
private fun LockPinSlot(
    filled: Boolean,
    error: Boolean,
) {
    val bord = when {
        error -> Red20
        filled -> BrandBlue
        else -> MissaMuted.copy(alpha = 0.75f)
    }
    val fond = when {
        error -> Red80
        filled -> MissaSoftBlue
        else -> MissaSurface
    }
    Box(
        modifier = Modifier
            .size(width = 48.dp, height = 52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(fond)
            .border(1.5.dp, bord, RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center,
    ) {
        if (filled) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(if (error) Red20 else BrandBlue),
            )
        }
    }
}

/** État d'erreur ou de blocage dans un bandeau doux et contrasté. */
@Composable
private fun LockStateMessage(
    text: String,
    blocked: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(11.dp))
            .background(if (blocked) Red80 else MissaSoftBlue)
            .semantics(mergeDescendants = true) {
                liveRegion = LiveRegionMode.Polite
            }
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(
            painter = painterResource(Iv.Warning),
            contentDescription = null,
            tint = if (blocked) Red20 else BrandBlue,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(7.dp))
        Text(
            text = text,
            color = if (blocked) Red20 else MissaInk,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
    }
}

/** Pavé numérique centré, quatre rangées, le 0 sur sa colonne centrale. */
@Composable
private fun Keypad(
    saisie: String,
    interactionActive: Boolean,
    descriptionEffacer: String,
    onDigit: (Char) -> Unit,
    onErase: () -> Unit,
) {
    val chiffresCompatibles = interactionActive && saisie.length < PinLockViewModel.PIN_LONGUEUR
    val forme = RoundedCornerShape(22.dp)
    Surface(
        modifier = Modifier
            .widthIn(max = 340.dp)
            .fillMaxWidth(),
        shape = forme,
        color = MissaSurface,
        border = BorderStroke(1.dp, MissaBorder.copy(alpha = 0.65f)),
        shadowElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            listOf("123", "456", "789").forEach { ligne ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    ligne.forEach { chiffre ->
                        KeypadTouche(
                            actif = chiffresCompatibles,
                            modifier = Modifier.weight(1f),
                            onClick = { onDigit(chiffre) },
                        ) {
                            Text(
                                text = chiffre,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MissaInk,
                            )
                        }
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                Spacer(Modifier.weight(1f))
                KeypadTouche(
                    actif = chiffresCompatibles,
                    modifier = Modifier.weight(1f),
                    onClick = { onDigit('0') },
                ) {
                    Text(
                        text = "0",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MissaInk,
                    )
                }
                KeypadTouche(
                    actif = interactionActive && saisie.isNotEmpty(),
                    modifier = Modifier
                        .weight(1f)
                        .semantics { contentDescription = descriptionEffacer },
                    onClick = onErase,
                ) {
                    Icon(
                        painter = painterResource(Iv.Backspace),
                        contentDescription = null,
                        tint = MissaMuted,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

/** Touche tactile : dimensions cohérentes, état désactivé clair et ripple Compose. */
@Composable
private fun KeypadTouche(
    actif: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    contenu: @Composable () -> Unit,
) {
    Surface(
        onClick = onClick,
        enabled = actif,
        shape = RoundedCornerShape(15.dp),
        color = MissaSurface,
        border = BorderStroke(
            1.dp,
            if (actif) MissaBorder else MissaBorder.copy(alpha = 0.55f),
        ),
        shadowElevation = if (actif) 1.dp else 0.dp,
        modifier = modifier
            .height(56.dp)
            .alpha(if (actif) 1f else 0.45f),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            contenu()
        }
    }
}
