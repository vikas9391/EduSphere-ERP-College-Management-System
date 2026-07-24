package com.collegeerp.Backend;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

@SpringBootApplication
public class BackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(BackendApplication.class, args);
    }

    @Bean
    CommandLineRunner checkEnv(Environment env) {
        return args -> {
            System.out.println("=================================");
            System.out.println("DB_URL      = " + env.getProperty("DB_URL"));
            System.out.println("DB_USERNAME = " + env.getProperty("DB_USERNAME"));
            System.out.println("=================================");
        };
    }
    @Bean
    CommandLineRunner printDb(Environment env) {
        return args -> {
            System.out.println("DB_URL = " + env.getProperty("DB_URL"));
            System.out.println("DB_USERNAME = " + env.getProperty("DB_USERNAME"));
        };
    }
}