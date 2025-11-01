package com.ywcheong.simple.transaction.teller.auto.domain.cash

@JvmInline
value class Cash(
    val value: Long
) {
    init {
        if (value < 0) throw NegativeCashException()
    }

    operator fun plus(other: Cash): Cash = Cash(this.value + other.value)
    operator fun minus(other: Cash): Cash = Cash(this.value - other.value)
    fun isZero(): Boolean = (value == 0L)

    companion object {
        val ZERO = Cash(0)
    }
}