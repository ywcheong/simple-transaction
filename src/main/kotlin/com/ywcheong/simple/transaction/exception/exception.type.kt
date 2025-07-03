package com.ywcheong.simple.transaction.exception

open class UserFaultException(
    override val message: String?
) : Exception(message)