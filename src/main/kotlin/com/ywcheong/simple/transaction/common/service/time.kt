package com.ywcheong.simple.transaction.common.service

import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*

@Service
class TimeService {
    fun nowDate(): Date = Date()
    fun nowInstant(): Instant = Instant.now()
}