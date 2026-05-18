package com.example.batchlab;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.mybatis.spring.annotation.MapperScan;

@SpringBootApplication
@MapperScan("com.example.batchlab.mapper")
public class BatchLabApplication {

	public static void main(String[] args) {
		SpringApplication.run(BatchLabApplication.class, args);
	}

}
