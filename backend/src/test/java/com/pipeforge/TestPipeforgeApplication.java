package com.pipeforge;

import org.springframework.boot.SpringApplication;

public class TestPipeforgeApplication {

	public static void main(String[] args) {
		SpringApplication.from(PipeforgeApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
