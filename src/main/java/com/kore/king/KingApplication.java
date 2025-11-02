package com.kore.king;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class KingApplication {

	public static void main(String[] args) {
		SpringApplication.run(KingApplication.class, args);
		System.out.println("Hi");
	}

}
