package com.qihui.profilegenerator;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@Slf4j
public class ProfileGeneratorApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProfileGeneratorApplication.class, args);
    }

    @Bean
    public CommandLineRunner init() {
        return args -> {
            log.info("简历转换工具启动成功");
            log.info("请访问: http://localhost:8080 使用Web界面");
        };
    }
}
