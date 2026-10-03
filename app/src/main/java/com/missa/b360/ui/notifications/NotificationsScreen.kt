package com.missa.b360.ui.notifications

import com.missa.b360.ui.theme.OnbConfigCard
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.missa.b360.R
import com.missa.b360.core.data.entity.NotificationEntity
import com.missa.b360.core.notifications.AppNotifier
import com.missa.b360.core.notifications.NotificationRoutes
import com.missa.b360.ui.components.MissaEmptyState
import com.missa.b360.ui.components.MissaTopAppBar
import com.missa.b360.ui.icons.Iv
import com.missa.b360.ui.theme.MissaBorder
import com.missa.b360.ui.theme.MissaCanvas
import com.missa.b360.ui.theme.MissaInk
import com.missa.b360.ui.theme.MissaMuted
import com.missa.b360.ui.theme.MissaSoftBlue
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val notifier: AppNotifier,
) : ViewModel() {
    val notifications = notifier.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun ouvrir(notification: NotificationEntity, naviguer: (String) -> Unit) {
        viewModelScope.launch {
            notifier.marquerLue(notification.id)
            naviguer(NotificationRoutes.pourType(notification.type))
        }
    }

    fun toutLire() {
        viewModelScope.launch { notifier.marquerToutesLues() }
    }
}

/** Centre réel des notifications : historique persistant, lecture et accès au module source. */
@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    onNaviguer: (String) -> Unit,
    viewModel: NotificationsViewModel = hiltViewModel(),
) {
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize()) {
        MissaTopAppBar(
            title = stringResource(R.string.notifications),
            onBack = onBack,
            actions = {
                if (notifications.any { !it.lue }) {
                    TextButton(onClick = viewModel::toutLire) {
                        Text(stringResource(R.string.notifications_tout_lire), color = MissaInk, fontSize = 11.sp)
                    }
                }
            },
        )
        if (notifications.isEmpty()) {
            MissaEmptyState(
                icon = Iv.Notifications,
                title = stringResource(R.string.notifications_vide_titre),
                description = stringResource(R.string.notifications_vide_description),
                modifier = Modifier.fillMaxSize().padding(20.dp),
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                items(notifications, key = { it.id }) { notification ->
                    NotificationLigne(notification) {
                        viewModel.ouvrir(notification, onNaviguer)
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationLigne(notification: NotificationEntity, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = if (notification.lue) OnbConfigCard else Color.White,
        border = if (notification.lue) null else BorderStroke(1.dp, MissaInk.copy(alpha = .28f)),
    ) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.Top) {
            Surface(
                modifier = Modifier.size(34.dp),
                shape = CircleShape,
                color = if (notification.lue) MissaCanvas else MissaInk,
            ) {
                Icon(
                    painter = painterResource(Iv.Notifications),
                    contentDescription = null,
                    tint = if (notification.lue) MissaInk else Color.White,
                    modifier = Modifier.padding(8.dp),
                )
            }
            Spacer(Modifier.width(9.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        notification.titre,
                        modifier = Modifier.weight(1f),
                        color = MissaInk,
                        fontWeight = if (notification.lue) FontWeight.Medium else FontWeight.Bold,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (!notification.lue) {
                        Spacer(Modifier.width(6.dp))
                        Surface(Modifier.size(7.dp), CircleShape, color = MissaInk) {}
                    }
                }
                Text(notification.message, color = MissaMuted, fontSize = 11.5.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
                Text(
                    DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(notification.date)),
                    color = MissaMuted,
                    fontSize = 9.5.sp,
                )
            }
        }
    }
}
