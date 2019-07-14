package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.core.services.InvoiceService
import io.pleo.antaeus.core.exceptions.*
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

class BillingService(
    private val invoiceService: InvoiceService,
    private val paymentProvider: PaymentProvider
) {
    private val MAX_RETRIES: Int = 3

    private fun attemptToPayInvoice(invoice: Invoice, attempt: Int = 0): Boolean {
        try {
            if (this.paymentProvider.charge(invoice)) {
                logger.info { "Charged invoice ${invoice.id}" }
                return true;
            }
            // @TODO what else should happen in this case?
            logger.info { "Failed to charge invoice ${invoice.id}" }
        } catch(e: CustomerNotFoundException) {
            // @TODO what else should happen in this case?
            logger.error(e) { "Customer not found when trying to charge invoice ${invoice.id}" }
        } catch(e: CurrencyMismatchException) {
            // @TODO what else should happen in this case?
            logger.error(e) { "Currency mismatched when trying to charge invoice ${invoice.id}" }
        } catch(e: NetworkException) {
            if (attempt < this.MAX_RETRIES) {
                logger.debug(e) { "Network exception when trying to charge invoice ${invoice.id} -- retrying" }
                return this.attemptToPayInvoice(invoice, attempt+1)
            }
            logger.error(e) { "Repeated network exception when trying to charge invoice ${invoice.id} -- giving up" }
            // @TODO what else should happen in this case?
        }

        return false;
    }

    fun createMonthly(): Boolean {
        val invoices = this.invoiceService.fetchAllUnpaid()
        for (invoice in invoices) {
            if (this.attemptToPayInvoice(invoice) ) {
                this.invoiceService.markInvoicePaid(invoice)
            }
        }
        return true
    }
}
