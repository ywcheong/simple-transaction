package com.ywcheong.simple.transaction.exception

class UserFaultException(
    override val message: String?
) : Exception(message)

fun check_domain(condition: Boolean, message_lambda: () -> String) {
    if (!condition) throw UserFaultException(message_lambda())
}