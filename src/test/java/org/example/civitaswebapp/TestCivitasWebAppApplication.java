package org.example.civitaswebapp;

import org.springframework.boot.SpringApplication;

public class TestCivitasWebAppApplication {

    public static void main(String[] args) {
        SpringApplication.from(CivitasWebAppApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
