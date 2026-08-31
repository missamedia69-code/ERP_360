package com.missa.b360.ui.admin

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.missa.b360.core.data.entity.JournalEntryEntity
import com.missa.b360.core.domain.usecase.JournalUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Journal (RA-18) : consultation immuable + purge 12 mois. */
@HiltViewModel
class JournalViewModel @Inject constructor(
    private val useCases: JournalUseCases,
) : ViewModel() {

    val entries: Flow<List<JournalEntryEntity>> = useCases.observer()
    var purgeOk by mutableStateOf(false)
        private set

    fun purger() {
        viewModelScope.launch {
            useCases.purge()
            purgeOk = true
        }
    }
}