package com.example.embabelagent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class EmbabelAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(EmbabelAgentApplication.class, args);
    }

}
