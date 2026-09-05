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
import com.missa.b360.R
import com.missa.b360.core.util.DateUtils
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Currency
import java.util.Locale

/**
 * Impression de la facture de vente via le framework Android (spec §32).
 * Le contenu provient exclusivement de la pièce persistée — aucune donnée fictive.
 */
internal fun Context.printSaleReceipt(receipt: SaleReceipt, devise: String) {
    val printManager = getSystemService(PrintManager::class.java) ?: return
    printManager.print(
        "${getString(R.string.sales_receipt_name)}-${receipt.reference}",
        SalePrintAdapter(receipt, devise),
        null,
    )
}

private class SalePrintAdapter(
    private val receipt: SaleReceipt,
    private val devise: String,
) : PrintDocumentAdapter() {
    override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes?,
        cancellationSignal: CancellationSignal?,
        callback: LayoutResultCallback,
        extras: android.os.Bundle?,
    ) {
        if (cancellationSignal?.isCanceled == true) {
            callback.onLayoutCancelled()
            return
        }
        callback.onLayoutFinished(
            PrintDocumentInfo.Builder("${receipt.reference}.pdf")
                .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                .setPageCount(1)
                .build(),
            oldAttributes != newAttributes,
        )
    }

    override fun onWrite(
        pages: Array<PageRange>,
        destination: ParcelFileDescriptor,
        cancellationSignal: CancellationSignal?,
        callback: WriteResultCallback,
    ) {
        val document = PdfDocument()
        try {
            if (cancellationSignal?.isCanceled == true) {
                callback.onWriteCancelled()
                return
            }
            val page = document.startPage(PdfDocument.PageInfo.Builder(595, 842, 1).create())
            val canvas = page.canvas
            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.rgb(21, 84, 232)
                textSize = 22f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }
            val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.rgb(16, 28, 67)
                textSize = 13f
            }
            canvas.drawText("MISSA BUSINESS 360", 48f, 68f, titlePaint)
            canvas.drawText(receipt.reference, 48f, 108f, bodyPaint)
            canvas.drawText(DateUtils.formatDateHeure(receipt.createdAt), 48f, 132f, bodyPaint)
            canvas.drawText(receipt.clientName, 48f, 156f, bodyPaint)
            var y = 195f
            receipt.payload.lines.take(22).forEach { line ->
                canvas.drawText("${line.name.take(30)} × ${line.quantity.saleQuantity()}  ${saleMoney(line.total, devise)}", 48f, y, bodyPaint)
                y += 23f
            }
            y += 15f
            canvas.drawText("TVA ${receipt.payload.taxRate.saleRate()}% : ${saleMoney(receipt.payload.taxAmount, devise)}", 48f, y, bodyPaint)
            y += 27f
            canvas.drawText("Total : ${saleMoney(receipt.total, devise)}", 48f, y, titlePaint)
            y += 27f
            canvas.drawText("Payé : ${saleMoney(receipt.paidAmount, devise)}", 48f, y, bodyPaint)
            y += 25f
            canvas.drawText(receipt.paymentMethod, 48f, y, bodyPaint)
            document.finishPage(page)
            ParcelFileDescriptor.AutoCloseOutputStream(destination).use { output -> document.writeTo(output) }
            callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
        } catch (error: Exception) {
            callback.onWriteFailed(error.message)
        } finally {
            document.close()
        }
    }
}

/** Montant formaté dans la devise de l'entreprise (jamais de conversion — spec §6). */
internal fun saleMoney(amount: Double, devise: String): String {
    val fractionDigits = runCatching { Currency.getInstance(devise).defaultFractionDigits }.getOrDefault(2)
    val pattern = if (fractionDigits == 0) "#,##0" else "#,##0.${"0".repeat(fractionDigits.coerceAtMost(2))}"
    val formatter = DecimalFormat(pattern, DecimalFormatSymbols(Locale.getDefault()))
    return "${formatter.format(amount)} $devise"
}

private fun Double.saleQuantity(): String = if (this % 1.0 == 0.0) toInt().toString() else DecimalFormat("0.##").format(this)
private fun Double.saleRate(): String = DecimalFormat("0.##", DecimalFormatSymbols(Locale.getDefault())).format(this)
