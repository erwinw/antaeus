package io.pleo.antaeus.core.services

import io.mockk.every
import io.mockk.mockk
import io.pleo.antaeus.core.exceptions.InvoiceNotFoundException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.Invoice
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertTrue


class BillingServiceTest {
    val paymentProvider = object: PaymentProvider {
        override fun charge(invoice: Invoice): Boolean {
                return true
        }
    }

    private val billingService = BillingService(paymentProvider = paymentProvider)

    @Test
    fun `test the test`() {
        assertTrue(billingService.createMonthly())
    }
}
