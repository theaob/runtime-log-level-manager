package io.github.theaob.loglevel.sample.billing

import org.slf4j.LoggerFactory
import kotlin.random.Random

class InvoiceService {
    private val log = LoggerFactory.getLogger(InvoiceService::class.java)

    fun createInvoice() {
        val amount = Random.nextInt(10, 500)
        log.debug("Calculating invoice for amount {}", amount)
        log.info("Invoice created: {} EUR", amount)
        if (amount > 450) log.error("Invoice amount {} exceeds the limit", amount)
    }
}
