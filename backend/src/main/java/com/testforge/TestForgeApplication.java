package com.testforge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class TestForgeApplication {

  public static void main(String[] args) {
    SpringApplication.run(TestForgeApplication.class, args);
  }
}
