package com.missa.b360.ui.onboarding

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.hardware.fingerprint.FingerprintManager
import android.os.Build
import android.os.CancellationSignal
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import java.util.concurrent.Executor

/** Disponibilité réelle d'une empreinte enregistrée, sans ajouter de dépendance au projet. */
@Suppress("DEPRECATION")
internal fun empreinteDisponible(context: Context): Boolean {
    val manager = context.getSystemService(FingerprintManager::class.java) ?: return false
    return manager.isHardwareDetected && manager.hasEnrolledFingerprints()
}

/**
 * Lance l'API système : BiometricPrompt à partir d'Android 9, API empreinte historique
 * sur Android 8/8.1 (minSdk 26). Le signal est annulé dès que le composable disparaît.
 */
@Composable
internal fun DemandeEmpreinte(
    declencheur: Int,
    active: Boolean,
    titre: String,
    sousTitre: String,
    annuler: String,
    onSucces: () -> Unit,
    onEchec: () -> Unit,
) {
    val context = LocalContext.current
    DisposableEffect(declencheur, active) {
        if (!active || !empreinteDisponible(context)) return@DisposableEffect onDispose { }
        val signal = CancellationSignal()
        val activity = context.trouverActivity()
        if (activity != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                lancerPromptModerne(activity, signal, titre, sousTitre, annuler, onSucces, onEchec)
            } else {
                lancerEmpreinteHistorique(context, signal, onSucces, onEchec)
            }
        }
        onDispose { signal.cancel() }
    }
}

private tailrec fun Context.trouverActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.trouverActivity()
    else -> null
}

@RequiresApi(Build.VERSION_CODES.P)
private fun lancerPromptModerne(
    activity: Activity,
    signal: CancellationSignal,
    titre: String,
    sousTitre: String,
    annuler: String,
    onSucces: () -> Unit,
    onEchec: () -> Unit,
) {
    val executor = Executor { activity.runOnUiThread(it) }
    val prompt = android.hardware.biometrics.BiometricPrompt.Builder(activity)
        .setTitle(titre)
        .setSubtitle(sousTitre)
        .setNegativeButton(annuler, executor) { _, _ -> }
        .build()
    prompt.authenticate(
        signal,
        executor,
        object : android.hardware.biometrics.BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: android.hardware.biometrics.BiometricPrompt.AuthenticationResult?) = onSucces()
            override fun onAuthenticationFailed() = onEchec()
        },
    )
}

@Suppress("DEPRECATION")
private fun lancerEmpreinteHistorique(
    context: Context,
    signal: CancellationSignal,
    onSucces: () -> Unit,
    onEchec: () -> Unit,
) {
    val manager = context.getSystemService(FingerprintManager::class.java) ?: return
    manager.authenticate(
        null,
        signal,
        0,
        object : FingerprintManager.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: FingerprintManager.AuthenticationResult?) = onSucces()
            override fun onAuthenticationFailed() = onEchec()
        },
        null,
    )
}
