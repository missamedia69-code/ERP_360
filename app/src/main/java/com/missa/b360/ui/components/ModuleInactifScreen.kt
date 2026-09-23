package com.missa.b360.ui.components

import com.missa.b360.ui.icons.Iv
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.missa.b360.R
import com.missa.b360.core.domain.model.ActivationProfil
import com.missa.b360.core.domain.model.ModuleCode
import com.missa.b360.core.domain.model.ModulesPersonnalises
import com.missa.b360.ui.navigation.AppModule
import com.missa.b360.ui.theme.BrandBlue
import com.missa.b360.ui.theme.MissaBorder
import com.missa.b360.ui.theme.MissaInk
import com.missa.b360.ui.theme.MissaMuted
import com.missa.b360.ui.theme.MissaSoftBlue

/**
 * Écran affiché quand l'utilisateur tente d'accéder à un module non actif
 * selon le profil courant. Il explique pourquoi le module est désactivé et
 * propose de l'activer depuis les réglages.
 */
@Composable
fun ModuleInactifScreen(
    module: AppModule,
    activation: ActivationProfil,
    onBack: () -> Unit,
    onActiver: (() -> Unit)? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            modifier = Modifier.size(72.dp),
            shape = RoundedCornerShape(18.dp),
            color = MissaSoftBlue,
        ) {
            Icon(
                painter = painterResource(Iv.Lock),
                contentDescription = null,
                tint = BrandBlue,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp),
            )
        }
        Spacer(Modifier.height(20.dp))
        Text(
            text = stringResource(R.string.module_inactif_titre, stringResource(module.titleRes)),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MissaInk,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(10.dp))
        val profilLabel = activation.profil?.let { stringResource(ModulesPersonnalises.libelleRes(it)) }
            ?: stringResource(R.string.home_not_configured)
        Text(
            text = stringResource(
                R.string.module_inactif_message,
                stringResource(module.titleRes),
                profilLabel,
            ),
            fontSize = 13.sp,
            color = MissaMuted,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.module_inactif_explication),
            fontSize = 12.sp,
            color = MissaMuted,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onBack,
            shape = RoundedCornerShape(12.dp),
        ) {
            Text(stringResource(R.string.module_inactif_retour))
        }
        if (onActiver != null) {
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = onActiver,
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(stringResource(R.string.module_inactif_activer))
            }
        }
    }
}

/**
 * Helper supprimé : utiliser ModulesPersonnalises.libelleRes(profil)
 */

/**
 * Wrapper qui vérifie si le module est actif, sinon affiche l'écran d'inactivité
 */
@Composable
fun ModuleGuard(
    module: AppModule,
    activation: ActivationProfil,
    onBack: () -> Unit,
    onActiver: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    if (activation.modulesActifs.isEmpty() || activation.isModuleActif(module.moduleCode)) {
        content()
    } else {
        ModuleInactifScreen(
            module = module,
            activation = activation,
            onBack = onBack,
            onActiver = onActiver,
        )
    }
}
