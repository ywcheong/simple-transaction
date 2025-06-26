package com.ywcheong.simple.transaction

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class SimpleTransactionApplication

fun main(args: Array<String>) {
    runApplication<SimpleTransactionApplication>(*args)
}
