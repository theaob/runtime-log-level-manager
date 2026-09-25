package tr.com.onurbaykal.loglevel.sample.orders

import org.slf4j.LoggerFactory
import kotlin.random.Random

class OrderService(private val repository: OrderRepository) {
    private val log = LoggerFactory.getLogger(OrderService::class.java)

    fun placeOrder() {
        val id = Random.nextInt(1000, 9999)
        log.trace("Validating order {}", id)
        log.debug("Placing order {}", id)
        repository.save(id)
        log.info("Order {} placed", id)
        if (id % 7 == 0) log.warn("Order {} needs manual review", id)
    }
}

class OrderRepository {
    private val log = LoggerFactory.getLogger(OrderRepository::class.java)

    fun save(id: Int) {
        log.debug("INSERT INTO orders VALUES ({})", id)
        log.trace("Transaction committed for {}", id)
    }
}
