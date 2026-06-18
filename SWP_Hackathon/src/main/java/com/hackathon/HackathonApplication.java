package com.hackathon;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class HackathonApplication {

    public static void main(String[] args) {
        SpringApplication.run(HackathonApplication.class, args);
//        ConfigurableApplicationContext context =
//                SpringApplication.run(HackathonApplication.class, args);
//
//        DatabaseService databaseService =
//                context.getBean(DatabaseService.class);
//
//        databaseService.createDatabase();
    }

}
