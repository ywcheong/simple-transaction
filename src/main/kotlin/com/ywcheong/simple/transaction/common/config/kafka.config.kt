package com.ywcheong.simple.transaction.common.config

import org.apache.kafka.clients.admin.NewTopic
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class KafkaConfig (
    @Value("\${st.kafka-topic}") private val kafkaTopicName: String
){
    @Bean
    fun newKafkaTopic(): NewTopic = NewTopic(kafkaTopicName, 3, 1)
}