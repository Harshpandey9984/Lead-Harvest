package com.company.scraper.worker.config;

import com.company.scraper.common.dto.ScrapeTask;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

@Configuration
public class KafkaConsumerConfig {

    @Bean
    public ConsumerFactory<String, ScrapeTask> scrapeTaskConsumerFactory(KafkaProperties properties) {
        var props = properties.buildConsumerProperties(null);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.company.scraper.common.dto");
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 100);
        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(),
            new JsonDeserializer<>(ScrapeTask.class));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, ScrapeTask> kafkaListenerContainerFactory(
        ConsumerFactory<String, ScrapeTask> consumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, ScrapeTask> factory =
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setConcurrency(8);
        return factory;
    }
}
