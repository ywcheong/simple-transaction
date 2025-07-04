package com.ywcheong.simple.transaction.account.domain

import com.ywcheong.simple.transaction.common.exception.UserFaultException

sealed class AccountException(msg: String) : UserFaultException(msg)
class InvalidAccountIdException : AccountException("계좌번호는 유효한 UUID4 양식이어야 합니다.")
class NegativeBalanceException : AccountException("계좌 잔고는 음수일 수 없습니다.")
class NonPositiveBalanceChangeException : AccountException("계좌 거래 단위는 양수여야 합니다.")
class InsufficientBalanceException : AccountException("계좌 잔고가 부족합니다.")