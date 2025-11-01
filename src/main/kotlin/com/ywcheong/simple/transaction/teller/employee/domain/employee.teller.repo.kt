package com.ywcheong.simple.transaction.teller.employee.domain

import com.ywcheong.simple.transaction.teller.base.domain.TellerId

interface EmployeeTellerRepository {
    fun select(tellerId: TellerId): EmployeeTeller?

    fun insert(employeeTeller: EmployeeTeller)
    fun update(employeeTeller: EmployeeTeller)
    fun delete(employeeTeller: EmployeeTeller)
}