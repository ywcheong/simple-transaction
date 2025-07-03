package com.ywcheong.simple.transaction.common.exception

open class UserFaultException(
    override val message: String?
) : Exception(message)