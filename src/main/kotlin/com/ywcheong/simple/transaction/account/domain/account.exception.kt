package com.ywcheong.simple.transaction.account.domain

import com.ywcheong.simple.transaction.common.exception.UserFaultException

sealed class AccountException(msg: String) : UserFaultException(msg)
class InvalidAccountIdException : AccountException("계좌번호는 유효한 UUID4 양식이어야 합니다.")
class NegativeBalanceException : AccountException("계좌 잔고는 음수일 수 없습니다.")
class NonPositiveBalanceChangeException : AccountException("계좌 거래 단위는 양수여야 합니다.")
class InsufficientBalanceException : AccountException("계좌 잔고가 부족합니다.")

class AccountNotFoundException : AccountException("계좌번호에 해당하는 계좌를 찾을 수 없습니다.")
class AccountNotOwnedException : AccountException("계좌의 소유주만 접근 가능합니다.")
class AccountBalanceNotZeroException : AccountException("계좌에 잔액이 남아 있으면 계좌를 폐쇄할 수 없습니다.")

sealed class UnexpectAccountException(msg: String) : RuntimeException(msg)
class UnexpectedAccountRepositoryFailedException : RuntimeException("계좌 저장에 문제가 발생했습니다.")