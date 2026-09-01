package com.missa.b360.ui.notifications

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.missa.b360.R
import com.missa.b360.core.data.entity.NotificationEntity
import com.missa.b360.core.notifications.AppNotifier
import com.missa.b360.core.util.DateUtils
import com.missa.b360.ui.components.MissaEmptyState
import com.missa.b360.ui.components.MissaLayout
import com.missa.b360.ui.components.MissaPanel
import com.missa.b360.ui.components.MissaSectionTitle
import com.missa.b360.ui.components.MissaTopAppBar
import com.missa.b360.ui.theme.MissaCanvas
import com.missa.b360.ui.theme.MissaInk
import com.missa.b360.ui.theme.MissaMuted
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
        viewModelScope.launch { appNotifier.marquerToutesLues() }
    }
}

/** Centre de notifications, aligné sur la grille blanche/bleue compacte des modules. */
@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    viewModel: NotificationsViewModel = hiltViewModel(),
) {
    val notifications by viewModel.notifications.collectAsState(initial = emptyList())
    val nonLues = notifications.count { !it.lue }

    Scaffold(
        containerColor = MissaCanvas,
        topBar = {
            MissaTopAppBar(
                title = androidx.compose.ui.res.stringResource(R.string.notifications),
                onBack = onBack,
                actions = {
                    IconButton(onClick = viewModel::marquerToutesLues, modifier = Modifier.size(42.dp)) {
                        BadgedBox(badge = {
                            if (nonLues > 0) Badge { Text(nonLues.coerceAtMost(99).toString(), fontSize = 9.sp) }
                        }) {
                            Icon(
                                Icons.Outlined.DoneAll,
                                contentDescription = androidx.compose.ui.res.stringResource(R.string.mark_all_read),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(21.dp),
                            )
                        }
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(
                horizontal = MissaLayout.screenHorizontal,
                vertical = MissaLayout.screenVertical,
            ),
            verticalArrangement = Arrangement.spacedBy(MissaLayout.itemGap),
        ) {
            item {
                MissaSectionTitle(
                    title = androidx.compose.ui.res.stringResource(R.string.notifications),
                    subtitle = if (nonLues > 0) nonLues.toString() else null,
                )
            }
            if (notifications.isEmpty()) {
                item {
                    MissaEmptyState(
                        icon = Icons.Outlined.NotificationsNone,
                        title = androidx.compose.ui.res.stringResource(R.string.no_notifications),
                    )
                }
            } else {
                items(notifications, key = { it.id }) { notification ->
                    NotificationRow(notification)
                }
            }
        }
    }
}

@Composable
private fun NotificationRow(notification: NotificationEntity) {
    MissaPanel(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.Top) {
            Surface(
                modifier = Modifier.size(34.dp),
                shape = CircleShape,
                color = if (notification.lue) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primaryContainer,
            ) {
                Icon(
                    imageVector = Icons.Outlined.NotificationsNone,
                    contentDescription = null,
                    tint = if (notification.lue) MissaMuted else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(8.dp),
                )
            }
            androidx.compose.foundation.layout.Spacer(Modifier.width(10.dp))
            androidx.compose.foundation.layout.Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(notification.titre, color = MissaInk, fontWeight = if (notification.lue) FontWeight.Medium else FontWeight.Bold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(notification.message, color = MissaMuted, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(DateUtils.formatDateHeure(notification.date), color = MissaMuted, fontSize = 9.sp)
            }
            if (!notification.lue) Badge(modifier = Modifier.padding(start = 5.dp))
        }
    }
}
