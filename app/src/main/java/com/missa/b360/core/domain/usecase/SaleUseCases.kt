package com.missa.b360.core.domain.usecase

import androidx.room.withTransaction
import com.missa.b360.core.data.dao.OperationRecordDao
import com.missa.b360.core.data.dao.SaleDao
import com.missa.b360.core.data.dao.StockDao
import com.missa.b360.core.data.dao.UserDao
import com.missa.b360.core.data.datastore.SettingsStore
import com.missa.b360.core.data.db.AppDatabase
import com.missa.b360.core.permissions.PermissionChecker
import com.missa.b360.core.data.entity.OperationDirection
import com.missa.b360.core.data.entity.OperationModule
import com.missa.b360.core.data.entity.OperationRecordEntity
import com.missa.b360.core.data.entity.OperationStatus
import com.missa.b360.core.data.entity.SaleEntity
import com.missa.b360.core.data.entity.SaleLineEntity
import com.missa.b360.core.data.entity.SalePaymentEntity
import com.missa.b360.core.data.entity.SaleReceivableEntity
import com.missa.b360.core.data.entity.SaleReceivableStatus
import com.missa.b360.core.data.entity.SaleStatus
import com.missa.b360.core.data.entity.StockMovementEntity
import com.missa.b360.core.data.entity.StockMovementType
import com.missa.b360.core.domain.model.SaleFormCalculator
import com.missa.b360.core.domain.model.SaleFormError
import com.missa.b360.core.domain.model.SaleFormInput
import com.missa.b360.core.domain.model.SaleFormLine
import com.missa.b360.core.domain.model.SaleLine
import com.missa.b360.core.domain.model.SaleMoney
import com.missa.b360.core.domain.model.SaleRecordCodec
import com.missa.b360.core.domain.model.SaleRecordPayload
import com.missa.b360.core.domain.model.SaleSaveOutcome
import com.missa.b360.core.journal.JournalManager
import com.missa.b360.core.licensing.LicenceManager
import com.missa.b360.core.numbering.DocType
import com.missa.b360.core.numbering.SequenceManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/** Détail complet d'une vente réelle, utilisé par l'aperçu/facture. */
data class SaleDetail(
    val sale: SaleEntity,
    val lines: List<SaleLineEntity>,
    val payment: SalePaymentEntity?,
    val receivable: SaleReceivableEntity?,
)

/** Résultat d'une mutation (annulation, validation de brouillon). */
sealed interface SaleMutationResult {
    data object Success : SaleMutationResult
    data object ReadOnly : SaleMutationResult
    data class Failed(val error: SaleFormError) : SaleMutationResult
}

/**
 * UseCases du module Vente : calcul métier pur, re-contrôle du stock à la sauvegarde,
 * numérotation atomique et écritures transactionnelles (vente + lignes + paiement +
 * créance si partielle + mouvements de stock + pièce financière + journal).
 */
@Singleton
class SaleUseCases @Inject constructor(
    private val database: AppDatabase,
    private val saleDao: SaleDao,
    private val operationDao: OperationRecordDao,
    private val stockDao: StockDao,
    private val sequenceManager: SequenceManager,
    private val licenceManager: LicenceManager,
    private val journalManager: JournalManager,
    private val settingsStore: SettingsStore,
    private val userDao: UserDao,
    private val permissionChecker: PermissionChecker,
) {

    /** Vérifie l'action sur le module VENTE ; sans utilisateur identifié, on reste compatible. */
    private suspend fun canWrite(action: PermissionChecker.Action): Boolean {
        val userId = settingsStore.getLong(SettingsStore.Keys.CURRENT_USER_ID) ?: return true
        val user = userDao.getById(userId) ?: return true
        return permissionChecker.hasPermission(user.roleId, OperationModule.VENTE.name, action)
    }

    fun observeSales(): Flow<List<SaleEntity>> = saleDao.observeAll()
    fun observeDrafts(): Flow<List<SaleEntity>> = saleDao.observeDrafts()
    suspend fun getSale(id: Long): SaleEntity? = saleDao.getById(id)
    suspend fun getByReference(reference: String): SaleEntity? = saleDao.getByReference(reference)
    suspend fun getByOperationRecordId(id: Long): SaleEntity? = saleDao.getByOperationRecordId(id)

    suspend fun getDetail(saleId: Long): SaleDetail? {
        val sale = saleDao.getById(saleId) ?: return null
        return SaleDetail(
            sale = sale,
            lines = saleDao.getLines(saleId),
            payment = saleDao.getPayments(saleId).firstOrNull(),
            receivable = saleDao.getReceivable(saleId),
        )
    }

    /**
     * Enregistre la vente (brouillon si [draft], sinon validation complète).
     * Toutes les écritures sont atomiques ; le stock est revérifié dans la même
     * transaction pour éviter les ventes basées sur l'écran.
     */
    suspend fun save(input: SaleFormInput, draft: Boolean): SaleSaveOutcome {
        if (licenceManager.isReadOnly()) return SaleSaveOutcome.ReadOnly
        if (!canWrite(PermissionChecker.Action.CREATE)) {
            return SaleSaveOutcome.Failed(
                SaleFormError(code = com.missa.b360.core.domain.model.SaleErrorCode.PERMISSION_DENIED, key = "permission"),
            )
        }

        val calc = SaleFormCalculator.calculate(input)
        SaleFormCalculator.validate(input, calc, allowIncomplete = draft)?.let { return SaleSaveOutcome.Failed(it) }

        return try {
            database.withTransaction {
                // Re-contrôle du stock au moment réel de la sauvegarde.
                input.lines
                    .filter { it.productId != null && !it.freeProduct }
                    .forEach { line ->
                        val product = stockDao.getProduct(line.productId!!)
                            ?: return@withTransaction SaleSaveOutcome.Failed(
                                SaleFormError(
                                    code = com.missa.b360.core.domain.model.SaleErrorCode.STOCK_INSUFFICIENT,
                                    productName = line.name,
                                    key = "stock",
                                ),
                            )
                        if (!product.actif || product.quantite <= 0.0 ||
                            product.quantite + 1e-9 < line.quantity
                        ) {
                            return@withTransaction SaleSaveOutcome.Failed(
                                SaleFormError(
                                    code = com.missa.b360.core.domain.model.SaleErrorCode.STOCK_INSUFFICIENT,
                                    productName = line.name,
                                    available = SaleMoney.fromDouble(product.quantite),
                                    key = "stock",
                                ),
                            )
                        }
                    }

                val clientName = if (input.walkIn) CLIENT_COMPTOIR
                else input.clientName.orEmpty().ifBlank { "Client" }
                val now = System.currentTimeMillis()
                val status = if (draft) SaleStatus.DRAFT.code else SaleStatus.VALIDATED.code
                val reference = sequenceManager.next(DocType.FACTURE)

                val saleId = saleDao.insert(
                    SaleEntity(
                        reference = reference,
                        clientId = input.clientId,
                        clientName = clientName,
                        walkIn = input.walkIn,
                        status = status,
                        subtotalCents = calc.subtotalCents,
                        discountCents = calc.discountCents,
                        deliveryCents = calc.deliveryCents,
                        taxRate = calc.taxRate,
                        taxAmountCents = calc.taxAmountCents,
                        totalCents = calc.totalCents,
                        paymentMethod = input.paymentMethod,
                        isCredit = calc.isCredit,
                        paidCents = calc.paidCents,
                        changeCents = calc.changeCents,
                        remainingCents = calc.remainingCents,
                        note = input.note?.trim()?.ifBlank { null },
                        internalReference = input.internalReference?.trim()?.ifBlank { null },
                        sellerName = input.sellerName?.trim()?.ifBlank { null },
                        siteName = input.siteName?.trim()?.ifBlank { null },
                        devise = input.devise,
                        createdAt = now,
                        validatedAt = if (!draft) now else null,
                    ),
                )

                val payload = buildPayload(
                    saleId = saleId,
                    reference = reference,
                    clientId = input.clientId,
                    clientName = clientName,
                    lines = input.lines,
                    calc = calc,
                    paymentMethod = input.paymentMethod,
                    note = input.note,
                )
                val notes = SaleRecordCodec.encode(payload)

                val operationId = operationDao.insert(
                    OperationRecordEntity(
                        module = OperationModule.VENTE.name,
                        reference = reference,
                        title = clientName,
                        counterpart = clientName,
                        amount = SaleMoney.toDouble(calc.totalCents),
                        quantity = input.lines.sumOf { it.quantity },
                        direction = OperationDirection.NONE.name,
                        status = status,
                        notes = notes,
                        createdAt = now,
                    ),
                )
                saleDao.update(
                    saleDao.getById(saleId)!!.copy(operationRecordId = operationId),
                )

                saleDao.insertLines(
                    input.lines.mapIndexed { index, line ->
                        SaleLineEntity(
                            saleId = saleId,
                            productId = line.productId,
                            sku = line.sku,
                            name = line.name,
                            unit = line.unit,
                            unitPriceCents = line.unitPriceCents,
                            quantity = line.quantity,
                            discountPct = line.discountPct,
                            netCents = line.netCents,
                            freeProduct = line.freeProduct,
                        )
                    },
                )

                val paymentOnlyIfValidated = !draft
                if (paymentOnlyIfValidated) {
                    if (calc.paidCents > 0) {
                        saleDao.insertPayments(
                            listOf(
                                SalePaymentEntity(
                                    saleId = saleId,
                                    method = input.paymentMethod,
                                    amountCents = calc.paidCents,
                                    createdAt = now,
                                ),
                            ),
                        )
                    }
                    if (calc.remainingCents > 0) {
                        saleDao.insertReceivable(
                            SaleReceivableEntity(
                                saleId = saleId,
                                clientId = input.clientId,
                                totalCents = calc.totalCents,
                                paidCents = calc.paidCents,
                                remainingCents = calc.remainingCents,
                                status = SaleReceivableStatus.OPEN.code,
                                createdAt = now,
                            ),
                        )
                    }
                    writeStockMovements(reference, clientName, input.lines, now)
                    writeFinanceMovement(reference, clientName, saleId, calc.totalCents, calc.paidCents, input.paymentMethod, OperationDirection.OUT, now, suffix = "ENC")
                }

                val details = buildString {
                    append("Vente $reference — $clientName")
                    if (draft) append(" (brouillon)")
                }
                journalManager.log(
                    module = OperationModule.VENTE.name,
                    action = if (draft) "VENTE_BROUILLON_SAUVEE" else "VENTE_ENREGISTREE",
                    details = details,
                )

                val completed = if (draft) {
                    emptyList()
                } else {
                    mutableListOf("SALE", "PAYMENT", "STOCK", "INVOICE").also {
                        if (calc.paidCents > 0) it.add("FINANCE")
                    }
                }
                SaleSaveOutcome.Success(
                    recordId = saleId,
                    reference = reference,
                    output = payload,
                    draft = draft,
                    completed = completed,
                )
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            SaleSaveOutcome.Failed(
                SaleFormError(code = com.missa.b360.core.domain.model.SaleErrorCode.INTERNAL, key = "sale"),
            )
        }
    }

    /** Annule une vente validée avec mouvements inverses (stock + finances + créance). */
    suspend fun cancel(saleId: Long): SaleMutationResult {
        if (licenceManager.isReadOnly()) return SaleMutationResult.ReadOnly
        if (!canWrite(PermissionChecker.Action.VALIDATE)) {
            return SaleMutationResult.Failed(
                SaleFormError(code = com.missa.b360.core.domain.model.SaleErrorCode.PERMISSION_DENIED, key = "permission"),
            )
        }
        return try {
            database.withTransaction {
                val sale = saleDao.getById(saleId)
                    ?: return@withTransaction SaleMutationResult.Failed(
                        SaleFormError(code = com.missa.b360.core.domain.model.SaleErrorCode.NOT_DRAFT, key = "sale"),
                    )
                if (sale.status != SaleStatus.VALIDATED.code) {
                    return@withTransaction SaleMutationResult.Failed(
                        SaleFormError(code = com.missa.b360.core.domain.model.SaleErrorCode.NOT_DRAFT, key = "sale"),
                    )
                }
                val now = System.currentTimeMillis()
                saleDao.cancelById(saleId, now)
                saleDao.cancelReceivable(saleId, now)

                sale.operationRecordId?.let { recordId ->
                    operationDao.getById(recordId)?.let { record ->
                        operationDao.update(record.copy(status = OperationStatus.CANCELLED.name))
                    }
                }

                val lines = saleDao.getLines(saleId)
                lines.filter { it.productId != null && !it.freeProduct }.forEachIndexed { index, line ->
                    stockDao.insertMovement(
                        StockMovementEntity(
                            reference = "${sale.reference}-RETOUR-${index + 1}",
                            type = StockMovementType.ENTRY.name,
                            productId = line.productId!!,
                            quantity = line.quantity,
                            delta = line.quantity,
                            price = SaleMoney.toDouble(line.unitPriceCents),
                            counterpart = sale.clientName,
                            status = OperationStatus.VALIDATED.name,
                            date = now,
                            notes = "Annulation vente ${sale.reference}",
                            createdAt = now,
                        ),
                    )
                    stockDao.updateProductQuantity(line.productId, line.quantity)
                }

                if (sale.paidCents > 0) {
                    writeFinanceMovement(
                        reference = sale.reference,
                        clientName = sale.clientName,
                        saleId = saleId,
                        totalCents = sale.totalCents,
                        paidCents = sale.paidCents,
                        method = sale.paymentMethod,
                        direction = OperationDirection.IN,
                        now = now,
                        suffix = "RET",
                    )
                }
                journalManager.log(
                    module = OperationModule.VENTE.name,
                    action = "VENTE_ANNULEE",
                    details = "Vente ${sale.reference} annulée — ${sale.clientName}",
                )
                SaleMutationResult.Success
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            SaleMutationResult.Failed(
                SaleFormError(code = com.missa.b360.core.domain.model.SaleErrorCode.INTERNAL, key = "sale"),
            )
        }
    }

    /** Réhausse un brouillon en vente validée. */
    suspend fun validateDraft(saleId: Long): SaleMutationResult {
        if (licenceManager.isReadOnly()) return SaleMutationResult.ReadOnly
        if (!canWrite(PermissionChecker.Action.VALIDATE)) {
            return SaleMutationResult.Failed(
                SaleFormError(code = com.missa.b360.core.domain.model.SaleErrorCode.PERMISSION_DENIED, key = "permission"),
            )
        }
        return try {
            database.withTransaction {
                val sale = saleDao.getById(saleId)
                    ?: return@withTransaction SaleMutationResult.Failed(
                        SaleFormError(code = com.missa.b360.core.domain.model.SaleErrorCode.NOT_DRAFT, key = "sale"),
                    )
                if (sale.status != SaleStatus.DRAFT.code) {
                    return@withTransaction SaleMutationResult.Failed(
                        SaleFormError(code = com.missa.b360.core.domain.model.SaleErrorCode.NOT_DRAFT, key = "sale"),
                    )
                }
                val validatedLines = saleDao.getLines(saleId)
                    .filter { it.productId != null && !it.freeProduct }
                validatedLines.forEach { line ->
                    val product = stockDao.getProduct(line.productId!!)
                    if (product == null || !product.actif || product.quantite + 1e-9 < line.quantity) {
                        return@withTransaction SaleMutationResult.Failed(
                            SaleFormError(
                                code = com.missa.b360.core.domain.model.SaleErrorCode.STOCK_INSUFFICIENT,
                                productName = line.name,
                                available = product?.let { SaleMoney.fromDouble(it.quantite) },
                                key = "stock",
                            ),
                        )
                    }
                }
                val now = System.currentTimeMillis()
                saleDao.update(sale.copy(status = SaleStatus.VALIDATED.code, validatedAt = now))
                validatedLines.forEachIndexed { index, line ->
                    stockDao.insertMovement(
                            StockMovementEntity(
                                reference = "${sale.reference}-${index + 1}",
                                type = StockMovementType.EXIT.name,
                                productId = line.productId!!,
                                quantity = line.quantity,
                                delta = -line.quantity,
                                price = SaleMoney.toDouble(line.unitPriceCents),
                                counterpart = sale.clientName,
                                status = OperationStatus.VALIDATED.name,
                                date = now,
                                notes = "Validation vente ${sale.reference}",
                                createdAt = now,
                            ),
                        )
                        stockDao.updateProductQuantity(line.productId!!, -line.quantity)
                    }
                if (sale.paidCents > 0) {
                    saleDao.insertPayments(
                        listOf(
                            SalePaymentEntity(saleId = saleId, method = sale.paymentMethod, amountCents = sale.paidCents, createdAt = now),
                        ),
                    )
                    writeFinanceMovement(
                        reference = sale.reference,
                        clientName = sale.clientName,
                        saleId = saleId,
                        totalCents = sale.totalCents,
                        paidCents = sale.paidCents,
                        method = sale.paymentMethod,
                        direction = OperationDirection.OUT,
                        now = now,
                        suffix = "ENC",
                    )
                }
                if (sale.remainingCents > 0) {
                    saleDao.insertReceivable(
                        SaleReceivableEntity(
                            saleId = saleId,
                            clientId = sale.clientId,
                            totalCents = sale.totalCents,
                            paidCents = sale.paidCents,
                            remainingCents = sale.remainingCents,
                            status = SaleReceivableStatus.OPEN.code,
                            createdAt = now,
                        ),
                    )
                }
                sale.operationRecordId?.let { operationDao.getById(it) }?.let { record ->
                    operationDao.update(record.copy(status = OperationStatus.VALIDATED.name))
                }
                journalManager.log(
                    module = OperationModule.VENTE.name,
                    action = "VENTE_VALIDEE",
                    details = "Brouillon ${sale.reference} validé",
                )
                SaleMutationResult.Success
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            SaleMutationResult.Failed(
                SaleFormError(code = com.missa.b360.core.domain.model.SaleErrorCode.INTERNAL, key = "sale"),
            )
        }
    }

    private suspend fun writeStockMovements(
        reference: String,
        clientName: String,
        lines: List<SaleFormLine>,
        now: Long,
    ) {
        lines.filter { it.productId != null && !it.freeProduct }.forEachIndexed { index, line ->
            stockDao.insertMovement(
                StockMovementEntity(
                    reference = "${reference}-${index + 1}",
                    type = StockMovementType.EXIT.name,
                    productId = line.productId!!,
                    quantity = line.quantity,
                    delta = -line.quantity,
                    price = SaleMoney.toDouble(line.unitPriceCents),
                    counterpart = clientName,
                    status = OperationStatus.VALIDATED.name,
                    date = now,
                    notes = "Vente $reference",
                    createdAt = now,
                ),
            )
            stockDao.updateProductQuantity(line.productId, -line.quantity)
        }
    }

    private suspend fun writeFinanceMovement(
        reference: String,
        clientName: String,
        saleId: Long,
        totalCents: Long,
        paidCents: Long,
        method: String,
        direction: OperationDirection,
        now: Long,
        suffix: String,
    ) {
        if (paidCents <= 0) return
        operationDao.insert(
            OperationRecordEntity(
                module = OperationModule.FINANCES.name,
                reference = "${reference}-$suffix",
                title = "Vente $reference",
                counterpart = clientName,
                amount = SaleMoney.toDouble(paidCents),
                direction = direction.name,
                status = OperationStatus.VALIDATED.name,
                notes = "Paiement $method — vente $reference (${SaleMoney.toDouble(totalCents)} ${suffix})",
                createdAt = now,
            ),
        )
    }

    private fun buildPayload(
        saleId: Long,
        reference: String,
        clientId: Long?,
        clientName: String,
        lines: List<SaleFormLine>,
        calc: com.missa.b360.core.domain.model.SaleCalculation,
        paymentMethod: String,
        note: String?,
    ): SaleRecordPayload = SaleRecordPayload(
        schemaVersion = 2,
        saleId = saleId,
        reference = reference,
        clientId = clientId,
        clientName = clientName,
        lines = lines.map { line ->
            SaleLine(
                id = line.id,
                name = line.name,
                unitPrice = SaleMoney.toDouble(line.unitPriceCents),
                quantity = line.quantity,
                productId = line.productId,
                sku = line.sku,
                unit = line.unit,
                discountPct = line.discountPct,
                freeProduct = line.freeProduct,
            )
        },
        subtotal = SaleMoney.toDouble(calc.subtotalCents),
        discount = SaleMoney.toDouble(calc.discountCents),
        delivery = SaleMoney.toDouble(calc.deliveryCents),
        taxRate = calc.taxRate,
        taxAmount = SaleMoney.toDouble(calc.taxAmountCents),
        total = SaleMoney.toDouble(calc.totalCents),
        paymentMethod = paymentMethod,
        paidAmount = SaleMoney.toDouble(calc.paidCents),
        note = note?.trim()?.ifBlank { null },
        subtotalCents = calc.subtotalCents,
        discountCents = calc.discountCents,
        deliveryCents = calc.deliveryCents,
        taxAmountCents = calc.taxAmountCents,
        totalCents = calc.totalCents,
        paidCents = calc.paidCents,
        remainingCents = calc.remainingCents,
        changeCents = calc.changeCents,
        isCredit = calc.isCredit,
    )

    companion object {
        const val CLIENT_COMPTOIR = "Client comptoir"
    }
}
