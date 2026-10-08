package Coffee

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class PremiumCoffeeApplication

fun main(args: Array<String>) {
    runApplication<PremiumCoffeeApplication>(*args)
}
