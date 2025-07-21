package com.ywcheong.simple.transaction

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity

@SpringBootApplication
@EnableMethodSecurity(prePostEnabled = true)
@EnableScheduling
class SimpleTransactionApplication

fun main(args: Array<String>) {
    runApplication<SimpleTransactionApplication>(*args)
}
