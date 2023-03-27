package com.vd5.beyondb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class BeyondBApplication {

    public static void main(String[] args) {
        SpringApplication.run(BeyondBApplication.class, args);
    }

}
