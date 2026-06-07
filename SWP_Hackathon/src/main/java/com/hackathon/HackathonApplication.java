package com.hackathon;

import com.hackathon.entity.*;
import com.hackathon.entity.enums.EventStatus;
import com.hackathon.entity.enums.OrganizationType;
import com.hackathon.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@SpringBootApplication
public class HackathonApplication {

    public static void main(String[] args) {
        SpringApplication.run(HackathonApplication.class, args);
    }


}