package com.eniac.weavroa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 应用启动类
 */
@SpringBootApplication(scanBasePackages = "com.eniac.weavroa")
public class ImageStandaloneApplication {

    public static void main(String[] args) {
        SpringApplication.run(ImageStandaloneApplication.class, args);
    }
}
