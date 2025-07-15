package com.ywcheong.simple.transaction

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class SimpleTransactionApplication

fun main(args: Array<String>) {
    runApplication<SimpleTransactionApplication>(*args)
}
