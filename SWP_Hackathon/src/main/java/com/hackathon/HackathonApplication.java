package com.hackathon;

import com.hackathon.service.DatabaseService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class HackathonApplication {

    public static void main(String[] args) {
//        SpringApplication.run(HackathonApplication.class, args);
        ConfigurableApplicationContext context =
                SpringApplication.run(HackathonApplication.class, args);

        DatabaseService databaseService =
                context.getBean(DatabaseService.class);

        databaseService.createDatabase();
    }

}
