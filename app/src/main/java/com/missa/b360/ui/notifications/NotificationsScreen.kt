package com.missa.b360.ui.notifications

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.missa.b360.R
import com.missa.b360.core.notifications.AppNotifier
import com.missa.b360.core.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val appNotifier: AppNotifier,
) : ViewModel() {
    val notifications = appNotifier.observeAll()

    fun marquerToutesLues() {
        // La lecture déclenche le recalcul du badge (RA-23).
        viewModelScope.launch {
            appNotifier.marquerToutesLues()
        }
    }
}

/** Centre de notifications (RA-23) : liste, badge non-lues, « tout marquer lu ». */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    viewModel: NotificationsViewModel = hiltViewModel(),
) {
    val notifications by viewModel.notifications.collectAsState(initial = emptyList())
    val nonLues = notifications.count { !it.lue }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.notifications)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.marquerToutesLues() }) {
                        BadgedBox(badge = { if (nonLues > 0) Badge { Text(nonLues.toString()) } }) {
                            Icon(
                                Icons.Outlined.DoneAll,
                                contentDescription = stringResource(R.string.mark_all_read),
                            )
                        }
                    }
                },
            )
        },
    ) { padding ->
        if (notifications.isEmpty()) {
            Text(
                text = stringResource(R.string.no_notifications),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
            )
        } else {
            LazyColumn(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                items(notifications) { notification ->
                    ListItem(
                        headlineContent = { Text(notification.titre) },
                        supportingContent = {
                            Text("${notification.message} · ${DateUtils.formatDateHeure(notification.date)}")
                        },
                        trailingContent = {
                            if (!notification.lue) {
                                Badge(modifier = Modifier.padding(4.dp))
                            }
                        },
                    )
                }
            }
        }
    }
}
