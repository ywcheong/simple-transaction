package com.ywcheong.simple.transaction.common.service

import com.ywcheong.simple.transaction.common.config.DomaConfig
import org.seasar.doma.jdbc.tx.TransactionManager
import org.springframework.stereotype.Service

@Service
class TransactionService (
    private val domaConfig: DomaConfig
) {
    fun transaction() : TransactionManager {
        return domaConfig.transactionManager
    }
}