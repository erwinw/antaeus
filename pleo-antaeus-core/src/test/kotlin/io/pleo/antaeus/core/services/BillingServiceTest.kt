package io.pleo.antaeus.core.services

import io.mockk.*
import io.pleo.antaeus.core.exceptions.*
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.*
import java.math.BigDecimal
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertTrue


class BillingServiceTest {

    val customerId = 1
    val invoiceId = 1

    val invoice = Invoice(
        id = invoiceId,
        customerId = customerId,
        amount = Money( value = BigDecimal(12.34), currency = Currency.EUR),
        status = InvoiceStatus.PENDING
    )

    @Test
    fun `empty invoice test`() {
        val dal = mockk<AntaeusDal> {
            every { fetchUnpaidInvoices() } returns emptyList<Invoice>()
        }

        val invoiceService = InvoiceService(dal = dal)
        val paymentProvider = mockkClass(PaymentProvider::class)

        val billingService = BillingService(
            invoiceService = invoiceService,
            paymentProvider = paymentProvider
        )

        assertTrue(billingService.createMonthly())
    }

    @Test
    fun `successful charge test`() {
        val dal = mockk<AntaeusDal> {
            every { fetchUnpaidInvoices() } returns listOf(invoice)
            every { markInvoicePaid(invoiceId) } just Runs
        }

        val invoiceService = InvoiceService(dal = dal)
        val paymentProvider = mockkClass(PaymentProvider::class)
        every { paymentProvider.charge(invoice) } returns true

        val billingService = BillingService(
            invoiceService = invoiceService,
            paymentProvider = paymentProvider
        )

        assertTrue(billingService.createMonthly())
        verify( atLeast = 1, atMost = 1 ) { paymentProvider.charge(invoice) }
    }

    @Test
    fun `failed charge test`() {
        val dal = mockk<AntaeusDal> {
            every { fetchUnpaidInvoices() } returns listOf(invoice)
            verify (atLeast = 0, atMost = 0) { markInvoicePaid(invoiceId) }
        }

        val invoiceService = InvoiceService(dal = dal)
        val paymentProvider = mockkClass(PaymentProvider::class)
        every { paymentProvider.charge(invoice) } returns false

        val billingService = BillingService(
            invoiceService = invoiceService,
            paymentProvider = paymentProvider
        )

        assertTrue(billingService.createMonthly())
        verify( atLeast = 1, atMost = 1 ) { paymentProvider.charge(invoice) }
    }

    @Test
    fun `CustomerNotFoundException test`() {
        val dal = mockk<AntaeusDal> {
            every { fetchUnpaidInvoices() } returns listOf(invoice)
            verify (atLeast = 0, atMost = 0) { markInvoicePaid(invoiceId) }
        }

        val invoiceService = InvoiceService(dal = dal)
        val paymentProvider = mockkClass(PaymentProvider::class)
        every { paymentProvider.charge(invoice) } throws CustomerNotFoundException(customerId)

        val billingService = BillingService(
            invoiceService = invoiceService,
            paymentProvider = paymentProvider
        )

        assertTrue(billingService.createMonthly())
        verify(atLeast=1, atMost=1) { paymentProvider.charge(invoice) }
    }

    @Test
    fun `CurrencyMismatchException test`() {
        val dal = mockk<AntaeusDal> {
            every { fetchUnpaidInvoices() } returns listOf(invoice)
            verify (atLeast = 0, atMost = 0) { markInvoicePaid(invoiceId) }
        }

        val invoiceService = InvoiceService(dal = dal)
        val paymentProvider = mockkClass(PaymentProvider::class)
        every { paymentProvider.charge(invoice) } throws CurrencyMismatchException(invoiceId=invoiceId, customerId=customerId)

        val billingService = BillingService(
            invoiceService = invoiceService,
            paymentProvider = paymentProvider
        )

        assertTrue(billingService.createMonthly())
        verify( atLeast = 1, atMost = 1 ) { paymentProvider.charge(invoice) }
    }

    @Test
    fun `NetworkException Exception test`() {
        val dal = mockk<AntaeusDal> {
            every { fetchUnpaidInvoices() } returns listOf(invoice)
            every { markInvoicePaid(invoiceId) } just Runs
        }

        val invoiceService = InvoiceService(dal = dal)
        var firstCall = true
        val paymentProvider = object: PaymentProvider {
            override fun charge(invoice: Invoice): Boolean {
                if(firstCall) {
                    firstCall = false
                    throw NetworkException()
                }
                return true
            }
        }

        val billingService = BillingService(
            invoiceService = invoiceService,
            paymentProvider = paymentProvider
        )

        assertTrue(billingService.createMonthly())
        verify (atLeast = 1, atMost = 1) { dal.markInvoicePaid(invoiceId) }
    }
}
