package com.ywcheong.simple.transaction.teller.employee.infra

import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController

data class CreateEmployeeTellerRequest(
    val id: String
)

@RestController("/tellers/employee/")
class EmployeeTellerController(

) : EmployeeTellerControllerSpec {

    @PostMapping("/employees")
    override fun createEmployeeTeller(request: CreateEmployeeTellerRequest) {
        // 요청 준비

        // 의존성 조율
//        val tx = transactionService.transaction()
//        tx.required {
//            autoTellerRepository.insert(emp)
//        }

        // 결과 반환
        return;
    }
}