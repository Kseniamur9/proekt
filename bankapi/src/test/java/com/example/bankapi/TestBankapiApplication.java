package com.example.bankapi;

import org.springframework.boot.SpringApplication;

public class TestBankapiApplication {

	public static void main(String[] args) {
		SpringApplication.from(BankapiApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
