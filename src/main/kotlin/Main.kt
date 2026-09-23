package Coffe

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class PremiumCoffeApplication

fun main(args: Array<String>) {
    runApplication<PremiumCoffeApplication>(*args)
}