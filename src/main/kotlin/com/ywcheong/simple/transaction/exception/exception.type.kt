package com.ywcheong.simple.transaction.exception

class UserFaultException(
    override val message: String?
) : Exception(message)