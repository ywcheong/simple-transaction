package com.ywcheong.simple.transaction.teller.auto.domain.cash

import com.ywcheong.simple.transaction.common.exception.UserFaultException

sealed class CashException(msg: String) : UserFaultException(msg)
class NegativeCashException : CashException("현찰의 양은 음수가 될 수 없습니다.")