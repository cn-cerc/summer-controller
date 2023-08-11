package cn.cerc.mis.core;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = { "cn.cerc", "com.mimrc" })
//@ImportResource("classpath*:summer-mis-spring.xml")
public class SummerSpringConfiguration {
//
//    @Bean
//    public ISystemTable getSystemTable() {
//        return null;
//    }
//
//    @Bean
//    public HttpServletRequest getHttpServletRequest() {
//        return null;
//    }

}
