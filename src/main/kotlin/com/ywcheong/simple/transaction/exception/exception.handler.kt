package com.ywcheong.simple.transaction.exception

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

val logger_ = KotlinLogging.logger {  }

@RestControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(Exception::class)
    fun handleServerError(ex: Exception): ResponseEntity<String> {
        logger_.error { "Unexpected error: $ex" }      // keep stack-trace in logs only
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("internal server error!")
    }
}
