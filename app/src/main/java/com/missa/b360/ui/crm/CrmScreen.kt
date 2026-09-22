package com.missa.b360.ui.crm

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.missa.b360.R
import com.missa.b360.core.domain.model.FicheCrm
import com.missa.b360.core.domain.model.SegmentClient
import com.missa.b360.ui.components.MissaEmptyState
import com.missa.b360.ui.components.MissaTopAppBar
import com.missa.b360.ui.icons.Iv
import com.missa.b360.ui.navigation.AppModule
import com.missa.b360.ui.stock.fmtValeur
import com.missa.b360.ui.theme.MissaBorder
import com.missa.b360.ui.theme.MissaInk
import com.missa.b360.ui.theme.MissaMuted
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Magenta caractéristique du CRM — source unique : [AppModule.CRM]. */
private val MagentaCrm: Color get() = AppModule.CRM.couleur

private data class TuileMatriceSpec(
    val icone: Int,
    val titre: String,
    val sousTitre: String,
    val estActif: Boolean,
    val onClick: () -> Unit,
)

@Composable
fun CrmScreen(
    onBack: () -> Unit,
    onNaviguer: (String) -> Unit = {},
    vm: CrmViewModel = hiltViewModel(),
) {
    val etat by vm.etat.collectAsStateWithLifecycle()
    val devise by vm.devise.collectAsStateWithLifecycle()
    val filtre by vm.filtre.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        MissaTopAppBar(
            title = stringResource(R.string.module_crm),
            onBack = onBack,
            couleurFond = AppModule.CRM.couleurPale,
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // --- Carte Synthèse Portefeuille Commercial ---
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MagentaCrm.copy(alpha = 0.16f),
                ) {
                    Column(Modifier.fillMaxWidth().padding(14.dp)) {
                        Text(
                            stringResource(R.string.crm_titre_synthese),
                            fontSize = 11.sp,
                            color = MissaMuted,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            fmtValeur(etat.chiffreAffaires, devise),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MissaInk,
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                stringResource(R.string.crm_panier_moyen, fmtValeur(etat.panierMoyen, devise)),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MagentaCrm,
                            )
                            Text(
                                stringResource(R.string.crm_taux_conversion, String.format(Locale.ROOT, "%.1f%%", etat.tauxConversion)),
                                fontSize = 11.sp,
                                color = MissaMuted,
                            )
                        }
                    }
                }
            }

            // --- Matrice 4 Tuiles (Grille Carrée d'actions et segmentation) ---
            item {
                Text(
                    stringResource(R.string.crm_matrice_titre),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MissaInk,
                )
            }

            item {
                val tuiles = listOf(
                    TuileMatriceSpec(
                        icone = Iv.Group,
                        titre = stringResource(R.string.crm_tuile_tous),
                        sousTitre = stringResource(R.string.crm_nb_count, etat.fiches.size),
                        estActif = filtre == null,
                        onClick = { vm.filtrer(null) },
                    ),
                    TuileMatriceSpec(
                        icone = Iv.Call,
                        titre = stringResource(R.string.crm_seg_a_relancer),
                        sousTitre = stringResource(R.string.crm_nb_count, etat.relances.size),
                        estActif = filtre == SegmentClient.A_RELANCER,
                        onClick = { vm.filtrer(SegmentClient.A_RELANCER) },
                    ),
                    TuileMatriceSpec(
                        icone = Iv.PersonAdd,
                        titre = stringResource(R.string.crm_seg_prospect),
                        sousTitre = stringResource(R.string.crm_nb_count, etat.compteurs.firstOrNull { it.segment == SegmentClient.PROSPECT }?.nombre ?: 0),
                        estActif = filtre == SegmentClient.PROSPECT,
                        onClick = { vm.filtrer(SegmentClient.PROSPECT) },
                    ),
                    TuileMatriceSpec(
                        icone = Iv.QualityBadge,
                        titre = stringResource(R.string.crm_seg_fidele),
                        sousTitre = stringResource(R.string.crm_nb_count, etat.compteurs.firstOrNull { it.segment == SegmentClient.FIDELE }?.nombre ?: 0),
                        estActif = filtre == SegmentClient.FIDELE,
                        onClick = { vm.filtrer(SegmentClient.FIDELE) },
                    ),
                )

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    tuiles.forEach { tuile ->
                        TuileCrm(
                            icone = tuile.icone,
                            titre = tuile.titre,
                            sousTitre = tuile.sousTitre,
                            estActif = tuile.estActif,
                            modifier = Modifier.weight(1f),
                            onClick = tuile.onClick,
                        )
                    }
                }
            }

            // --- Registre des Contacts & Opportunités ---
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.crm_titre_registre),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MissaInk,
                        modifier = Modifier.weight(1f),
                    )
                    if (filtre != null) {
                        Surface(shape = RoundedCornerShape(8.dp), color = MagentaCrm.copy(alpha = 0.2f)) {
                            Text(
                                stringResource(filtre!!.libelleRes),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MagentaCrm,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                }
            }

            if (etat.fiches.isEmpty()) {
                item {
                    MissaEmptyState(
                        icon = Iv.Group,
                        title = stringResource(R.string.crm_aucun_contact),
                        description = stringResource(R.string.crm_aucun_desc),
                        modifier = Modifier.padding(16.dp),
                    )
                }
            } else {
                items(etat.fiches, key = { it.client.id }) { fiche ->
                    CarteFicheCrm(
                        fiche = fiche,
                        devise = devise,
                        onConsulter = { onNaviguer(AppModule.CLIENTS.route) },
                    )
                }
            }
        }
    }
}

@Composable
private fun TuileCrm(
    icone: Int,
    titre: String,
    sousTitre: String,
    estActif: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (estActif) MagentaCrm.copy(alpha = 0.15f) else Color.White,
        border = BorderStroke(1.dp, if (estActif) MagentaCrm else MissaBorder),
        modifier = modifier
            .height(82.dp)
            .clickable(onClick = onClick),
    ) {
        Column(
            Modifier.padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(painterResource(icone), null, tint = MissaInk, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(4.dp))
            Text(titre, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MissaInk, maxLines = 1)
            Text(sousTitre, fontSize = 9.sp, color = MissaMuted, maxLines = 1)
        }
    }
}

@Composable
private fun CarteFicheCrm(
    fiche: FicheCrm,
    devise: String,
    onConsulter: () -> Unit,
) {
    val dateAchatStr = remember(fiche.dernierAchat) {
        fiche.dernierAchat?.let {
            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(it))
        }
    }

    val couleurSegment = when (fiche.segment) {
        SegmentClient.A_RELANCER -> Color(0xFFDC2626)
        SegmentClient.PROSPECT -> Color(0xFFF59E0B)
        SegmentClient.NOUVEAU -> Color(0xFF2563EB)
        SegmentClient.FIDELE -> Color(0xFF16A34A)
        SegmentClient.OCCASIONNEL -> Color(0xFF4B5563)
        SegmentClient.DORMANT -> Color(0xFF9333EA)
        SegmentClient.INACTIF -> Color(0xFF9CA3AF)
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = BorderStroke(1.dp, MissaBorder),
    ) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(fiche.client.nom, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MissaInk)
                Spacer(Modifier.weight(1f))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = couleurSegment.copy(alpha = 0.15f),
                ) {
                    Text(
                        stringResource(fiche.segment.libelleRes),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = couleurSegment,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
            if (!fiche.client.telephone.isNullOrBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(fiche.client.telephone!!, fontSize = 11.5.sp, color = MissaMuted)
            }
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    stringResource(R.string.crm_achats_count, fiche.nombreAchats),
                    fontSize = 11.sp,
                    color = MissaMuted,
                )
                Text(
                    fmtValeur(fiche.chiffreAffaires, devise),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MissaInk,
                )
            }
            if (dateAchatStr != null) {
                Text(
                    stringResource(R.string.crm_dernier_achat, dateAchatStr, fiche.joursDepuisAchat ?: 0),
                    fontSize = 10.sp,
                    color = MissaMuted,
                )
            } else {
                Text(stringResource(R.string.crm_aucun_achat_encore), fontSize = 10.sp, color = MissaMuted)
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onConsulter,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MagentaCrm, contentColor = Color.White),
            ) {
                Text(stringResource(R.string.crm_action_ouvrir_fiche), fontSize = 11.sp, color = Color.White)
            }
        }
    }
}
