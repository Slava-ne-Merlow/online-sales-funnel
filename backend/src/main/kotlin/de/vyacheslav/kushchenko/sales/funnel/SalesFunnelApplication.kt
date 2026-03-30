package de.vyacheslav.kushchenko.sales.funnel

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.ConfigurableApplicationContext

@SpringBootApplication
class SalesFunnelApplication

fun main(args: Array<String>) {
    runApplication<SalesFunnelApplication>(*args)
}

fun run(
    args: Array<String>,
    init: SpringApplication.() -> Unit = {},
): ConfigurableApplicationContext {
    return runApplication<SalesFunnelApplication>(*args, init = init)
}
