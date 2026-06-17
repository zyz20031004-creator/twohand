package com.campus.twohand;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@ComponentScan(
        basePackages = "com.campus.twohand",
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.REGEX,
                pattern = "com\\.campus\\.twohand\\.controller\\..*"
        )
)
public class TwohandApplication {

    public static void main(String[] args) {
        System.out.println("✅✅✅ 当前启动的是 twohand 后端（com.campus.twohand）");
        SpringApplication.run(TwohandApplication.class, args);
        //密码加密
        // System.out.println(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("123456"));
        System.out.println("后端启动成功！！！");
    }
}
