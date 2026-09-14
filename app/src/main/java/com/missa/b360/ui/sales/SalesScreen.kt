package com.missa.b360.ui.sales

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.missa.b360.R
import com.missa.b360.core.domain.model.MentionsLegales
import com.missa.b360.core.util.DateUtils
import com.missa.b360.core.util.MoneyUtils
import com.missa.b360.ui.components.MissaLayout
import com.missa.b360.ui.components.MissaPanel
import com.missa.b360.ui.components.MissaTopAppBar
import com.missa.b360.ui.theme.BrandBlue
import com.missa.b360.ui.theme.MissaBorder
import com.missa.b360.ui.theme.MissaCanvas
import com.missa.b360.ui.theme.MissaInk
import com.missa.b360.ui.theme.MissaMuted
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Currency
import java.util.Locale

/**
 * Hub Ventes — 3e maillon du bloc ACH-STK-VEN.
 * Cohérent avec Accueil/Stock/Achats, respecte l'option venteSansStock.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesScreen(
    onNavigate: (String) -> Unit = {},
    onOpenClientCreate: () -> Unit = {},
    openCreate: Boolean = false,
    viewModel: SalesHubViewModel = hiltViewModel(),
) {
    val etat by viewModel.etat.collectAsState()
    val devise by viewModel.devise.collectAsState()

    Scaffold(
        topBar = {
            MissaTopAppBar(
                title = stringResource(R.string.module_vente),
                onBack = { onNavigate("home") },
            )
        },
        containerColor = MissaCanvas,
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().background(MissaCanvas).padding(padding),
            contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 92.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                MissaPanel {
                    Text(text = "Tableau de bord ventes", color = MissaInk, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SalesKpi(titre = "Ventes du jour", valeur = MoneyUtils.format(etat.ventesDuJour, devise), sousTitre = "${etat.commandesDuJour} commandes", modifier = Modifier.weight(1f))
                        SalesKpi(titre = "Clients", valeur = etat.clients.toString(), sousTitre = "${etat.devis} devis", modifier = Modifier.weight(1f))
                    }
                    if (!etat.venteSansStock && etat.alertStock) {
                        Spacer(Modifier.height(8.dp))
                        Text(text = "Stock faible — certaines ventes sont en attente", color = Color(0xFFDC2626), fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            item {
                Text(text = "Actions rapides", color = MissaInk, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    SalesAction(titre = "Nouvelle vente", icone = Icons.Outlined.PointOfSale, couleur = BrandBlue, modifier = Modifier.weight(1f), onClick = { onNavigate("vente_create") })
                    SalesAction(titre = "Devis", icone = Icons.Outlined.Assignment, couleur = Color(0xFF059669), modifier = Modifier.weight(1f), onClick = { onNavigate("devis") })
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    SalesAction(titre = "Clients", icone = Icons.Outlined.Group, couleur = Color(0xFF7C3AED), modifier = Modifier.weight(1f), onClick = onOpenClientCreate)
                    SalesAction(titre = "Retours", icone = Icons.Outlined.Receipt, couleur = Color(0xFFDC2626), modifier = Modifier.weight(1f), onClick = { onNavigate("vente_retour") })
                }
            }
            item {
                MissaPanel {
                    Text(text = "Ventes récentes", color = MissaInk, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))
                    if (etat.recent.isEmpty()) {
                        Text(text = "Aucune vente récente", color = MissaMuted, fontSize = 12.sp)
                    } else {
                        etat.recent.take(5).forEach { v ->
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Surface(shape = RoundedCornerShape(6.dp), color = MissaCanvas, modifier = Modifier.size(32.dp)) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Outlined.PointOfSale, contentDescription = null, tint = MissaMuted, modifier = Modifier.size(16.dp)) } }
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) { Text(text = v.client, color = MissaInk, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1); Text(text = v.reference, color = MissaMuted, fontSize = 11.sp) }
                                Text(text = MoneyUtils.format(v.montant, devise), color = MissaInk, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SalesKpi(titre: String, valeur: String, sousTitre: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = MissaCanvas), border = BorderStroke(1.dp, MissaBorder), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(text = titre, color = MissaMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(text = valeur, color = MissaInk, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1)
            Text(text = sousTitre, color = MissaMuted, fontSize = 10.5.sp, maxLines = 1)
        }
    }
}

@Composable
private fun SalesAction(titre: String, icone: ImageVector, couleur: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(modifier = modifier.clickable(onClick = onClick), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, MissaBorder), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(shape = RoundedCornerShape(10.dp), color = couleur.copy(alpha = 0.12f), modifier = Modifier.size(40.dp)) { Box(contentAlignment = Alignment.Center) { Icon(icone, contentDescription = null, tint = couleur, modifier = Modifier.size(22.dp)) } }
            Spacer(Modifier.height(8.dp))
            Text(text = titre, color = MissaInk, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@dagger.hilt.android.lifecycle.HiltViewModel
class SalesHubViewModel @javax.inject.Inject constructor() : androidx.lifecycle.ViewModel() {
    private val _etat = kotlinx.coroutines.flow.MutableStateFlow(SalesHubEtat())
    val etat: kotlinx.coroutines.flow.StateFlow<SalesHubEtat> = _etat
    private val _devise = kotlinx.coroutines.flow.MutableStateFlow("XAF")
    val devise: kotlinx.coroutines.flow.StateFlow<String> = _devise
    data class SalesHubEtat(val ventesDuJour: Double = 0.0, val commandesDuJour: Int = 0, val clients: Int = 0, val devis: Int = 0, val venteSansStock: Boolean = false, val alertStock: Boolean = false, val recent: List<Vente> = emptyList())
    data class Vente(val client: String, val reference: String, val montant: Double)
}

/**
 * Impression de la facture de vente via le framework Android (spec §32).
 */
internal fun Context.printSaleReceipt(
    receipt: SaleReceipt,
    devise: String,
    mentions: MentionsLegales,
) {
    val printManager = getSystemService(PrintManager::class.java) ?: return
    printManager.print(
        "${getString(R.string.sales_receipt_name)}-${receipt.reference}",
        SalePrintAdapter(receipt, devise, mentions),
        null,
    )
}

private class SalePrintAdapter(
    private val receipt: SaleReceipt,
    private val devise: String,
    private val mentions: MentionsLegales,
) : PrintDocumentAdapter() {
    override fun onLayout(oldAttributes: PrintAttributes?, newAttributes: PrintAttributes?, cancellationSignal: CancellationSignal?, callback: LayoutResultCallback, extras: android.os.Bundle?) {
        if (cancellationSignal?.isCanceled == true) { callback.onLayoutCancelled(); return }
        callback.onLayoutFinished(PrintDocumentInfo.Builder("${receipt.reference}.pdf").setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT).setPageCount(1).build(), oldAttributes != newAttributes)
    }
    override fun onWrite(pages: Array<PageRange>, destination: ParcelFileDescriptor, cancellationSignal: CancellationSignal?, callback: WriteResultCallback) {
        val document = PdfDocument()
        try {
            if (cancellationSignal?.isCanceled == true) { callback.onWriteCancelled(); return }
            val page = document.startPage(PdfDocument.PageInfo.Builder(595, 842, 1).create())
            val canvas = page.canvas
            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.rgb(21, 84, 232); textSize = 22f; typeface = android.graphics.Typeface.DEFAULT_BOLD }
            val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.rgb(16, 28, 67); textSize = 13f }
            val smallPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.rgb(108, 122, 155); textSize = 10f }
            canvas.drawText(mentions.nom, 48f, 68f, titlePaint)
            var headerY = 86f
            mentions.lignes.forEach { ligne -> canvas.drawText(ligne, 48f, headerY, smallPaint); headerY += 14f }
            canvas.drawText(receipt.reference, 48f, headerY + 18f, bodyPaint)
            canvas.drawText(DateUtils.formatDateHeure(receipt.createdAt), 48f, headerY + 42f, bodyPaint)
            canvas.drawText(receipt.clientName, 48f, headerY + 66f, bodyPaint)
            var y = headerY + 105f
            receipt.payload.lines.take(22).forEach { line -> canvas.drawText("${line.name.take(30)} × ${line.quantity.saleQuantity()}  ${saleMoney(line.total, devise)}", 48f, y, bodyPaint); y += 23f }
            y += 15f
            canvas.drawText("TVA ${receipt.payload.taxRate.saleRate()}% : ${saleMoney(receipt.payload.taxAmount, devise)}", 48f, y, bodyPaint); y += 27f
            canvas.drawText("Total : ${saleMoney(receipt.total, devise)}", 48f, y, titlePaint); y += 27f
            canvas.drawText("Payé : ${saleMoney(receipt.paidAmount, devise)}", 48f, y, bodyPaint); y += 25f
            canvas.drawText(receipt.paymentMethod, 48f, y, bodyPaint)
            document.finishPage(page)
            ParcelFileDescriptor.AutoCloseOutputStream(destination).use { output -> document.writeTo(output) }
            callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
        } catch (error: Exception) { callback.onWriteFailed(error.message) } finally { document.close() }
    }
}
internal fun saleMoney(amount: Double, devise: String): String {
    val fractionDigits = runCatching { Currency.getInstance(devise).defaultFractionDigits }.getOrDefault(2)
    val pattern = if (fractionDigits == 0) "#,##0" else "#,##0.${"0".repeat(fractionDigits.coerceAtMost(2))}"
    val formatter = DecimalFormat(pattern, DecimalFormatSymbols(Locale.getDefault()))
    return "${formatter.format(amount)} $devise"
}
private fun Double.saleQuantity(): String = if (this % 1.0 == 0.0) toInt().toString() else DecimalFormat("0.##").format(this)
private fun Double.saleRate(): String = DecimalFormat("0.##", DecimalFormatSymbols(Locale.getDefault())).format(this)
