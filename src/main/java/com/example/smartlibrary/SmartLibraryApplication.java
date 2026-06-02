package com.example.smartlibrary;

import com.example.smartlibrary.config.LlmProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(LlmProperties.class)
public class SmartLibraryApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartLibraryApplication.class, args);
    }
}
