package com.ams.streams.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotNull;

@Configuration
@ConfigurationProperties("topics")
@Getter
@Setter
@Validated
public class KafkaStreamsProperties {

    @NotNull private String visitEventsTopic;
}
