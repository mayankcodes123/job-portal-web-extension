package com.jobcrm.reminders;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ReminderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReminderServiceApplication.class, args);
    }
}
