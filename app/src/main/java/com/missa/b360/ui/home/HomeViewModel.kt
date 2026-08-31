package com.missa.b360.ui.home

import androidx.lifecycle.ViewModel
import com.missa.b360.core.notifications.AppNotifier
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** ViewModel de l'écran d'accueil — badge cloche (RA-23, recalculé après lecture). */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val appNotifier: AppNotifier,
) : ViewModel() {

    val notificationsNonLues: Flow<Int> = appNotifier.observeNonLues()
}
