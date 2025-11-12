package com.kore.king;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;


@SpringBootApplication
@EnableRetry
public class KingApplication {

	public static void main(String[] args) {
		SpringApplication.run(KingApplication.class, args);
		System.out.println("Hi");
	}

}
