package com.example.bankapi;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestсontainersConfiguration.class)
@SpringBootTest
class BankapiApplicationTests {

	@Test
	void contextLoads() {
	}

}
