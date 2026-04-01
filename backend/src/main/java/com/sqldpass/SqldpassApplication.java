package com.sqldpass;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class SqldpassApplication {

	public static void main(String[] args) {
		SpringApplication.run(SqldpassApplication.class, args);
	}

}
