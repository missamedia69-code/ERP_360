package com.missa.b360.core.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.missa.b360.MainActivity
import com.missa.b360.R
import com.missa.b360.core.data.dao.NotificationDao
import com.missa.b360.core.data.entity.NotificationEntity
import com.missa.b360.ui.navigation.AppModule
import com.missa.b360.ui.navigation.Routes
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/** Clés et routes stables utilisées par la liste interne et les notifications Android. */
object NotificationRoutes {
    const val EXTRA_ROUTE = "notification_route"

    fun pourType(type: String): String = when {
        type.startsWith("STOCK") || type == "INVENTAIRE" -> AppModule.STOCK.route
        type.startsWith("VENTE") || type.startsWith("CLIENT") -> AppModule.VENTE.route
        type.startsWith("ACHAT") || type.startsWith("RECEPTION") || type.startsWith("RETOUR_ACHAT") -> AppModule.ACHATS.route
        type.startsWith("FOURNISSEUR") -> AppModule.FOURNISSEURS.route
        type.startsWith("TRESORERIE") || type.startsWith("PAIEMENT") -> AppModule.TRESORERIE.route
        type.startsWith("PRODUCTION") -> AppModule.PRODUCTION.route
        type.startsWith("SERVICE") || type.startsWith("INTERVENTION") -> AppModule.SERVICES.route
        type.startsWith("PROJET") || type.startsWith("TACHE") -> AppModule.PROJETS.route
        type.startsWith("RH") || type.startsWith("CONGE") -> AppModule.RH.route
        type.startsWith("QUALITE") || type.startsWith("NON_CONFORMITE") -> AppModule.QUALITE.route
        type.startsWith("MAINTENANCE") -> AppModule.MAINTENANCE.route
        type.startsWith("LIVRAISON") || type.startsWith("LOGISTIQUE") -> AppModule.LOGISTIQUE.route
        type.startsWith("COMPTA") -> AppModule.COMPTABILITE.route
        else -> Routes.HOME
    }
}

/**
 * Centre de notifications offline-first : persistance Room, badge réactif et miroir
 * Android. La notification reste dans l'application même si l'autorisation système
 * est refusée ; aucune information métier n'est donc perdue.
 */
@Singleton
class AppNotifier @Inject constructor(
    private val notificationDao: NotificationDao,
    @ApplicationContext private val context: Context,
) {
    private val manager = context.getSystemService(NotificationManager::class.java)

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ACTIVITY, "Activité", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Événements importants de l’activité"
                },
            )
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ALERTS, "Alertes", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Alertes nécessitant une action"
                    enableVibration(true)
                },
            )
        }
    }

    suspend fun notifier(type: String, titre: String, message: String, date: Long = System.currentTimeMillis()) {
        // Anti-bruit : une même alerte émise plusieurs fois par une chaîne transactionnelle
        // n'apparaît qu'une fois pendant cinq minutes.
        if (notificationDao.compterRecentes(type, message, date - DEDUPLICATION_MS) > 0) return
        val id = notificationDao.insert(
            NotificationEntity(type = type, titre = titre, message = message, date = date),
        )
        afficherSysteme(id, type, titre, message)
    }

    fun observeAll(): Flow<List<NotificationEntity>> = notificationDao.observeAll()
    fun observeNonLues(): Flow<Int> = notificationDao.observeNonLues()
    suspend fun marquerLue(id: Long) = notificationDao.marquerLue(id)
    suspend fun marquerToutesLues() = notificationDao.marquerToutesLues()

    private fun afficherSysteme(id: Long, type: String, titre: String, message: String) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val route = NotificationRoutes.pourType(type)
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(NotificationRoutes.EXTRA_ROUTE, route)
        }
        val pending = PendingIntent.getActivity(
            context,
            id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val alerte = type.contains("ALERTE") || type.contains("BLOQUE") || type.contains("RETARD") || type.contains("ECHEC")
        val notification = NotificationCompat.Builder(context, if (alerte) CHANNEL_ALERTS else CHANNEL_ACTIVITY)
            .setSmallIcon(R.drawable.ph_bell)
            .setContentTitle(titre)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setCategory(if (alerte) NotificationCompat.CATEGORY_ALARM else NotificationCompat.CATEGORY_STATUS)
            .setPriority(if (alerte) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .build()
        manager.notify(id.toInt(), notification)
    }

    private companion object {
        const val CHANNEL_ACTIVITY = "missa_activity"
        const val CHANNEL_ALERTS = "missa_alerts"
        const val DEDUPLICATION_MS = 5 * 60 * 1_000L
    }
}
