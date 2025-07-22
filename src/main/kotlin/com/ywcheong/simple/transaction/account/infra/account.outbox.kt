package com.ywcheong.simple.transaction.account.infra

import com.ywcheong.simple.transaction.account.domain.AccountEvent
import com.ywcheong.simple.transaction.account.domain.AccountEventOutboxRepository
import com.ywcheong.simple.transaction.common.service.TimeService
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class AccountOutboxService(
    private val timeService: TimeService,
    private val outboxRepository: AccountEventOutboxRepository,
    private val kafkaTemplate: KafkaTemplate<String, AccountEvent>,
    @param:Value("\${st.kafka-topic}") private val kafkaTopicName: String
) {
    @Scheduled(fixedRate = 2000)
    fun publishEvent() {
        // TODO 멱등성 보장이 없는 코드 개선
        val waitingEvents = outboxRepository.findNotPublished()
        waitingEvents.forEach { kafkaTemplate.send(kafkaTopicName, it) }
        outboxRepository.markAsPublished(waitingEvents, timeService.nowDate())
    }
}