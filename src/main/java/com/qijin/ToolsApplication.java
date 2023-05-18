package com.qijin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


@Configuration
@EnableAutoConfiguration(exclude={DataSourceAutoConfiguration.class})
@ComponentScan(basePackages={"com.qijin"})
public class ToolsApplication implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/doc.html")
                .addResourceLocations("classpath:/META-INF/resources/");
        registry.addResourceHandler("/webjars/**").
                addResourceLocations("classpath:/META-INF/resources/webjars/");
    }

    public static void main(String[] args) {
        SpringApplication.run(ToolsApplication.class, args);
    }
//    @Bean
//    public MultipartConfigElement multipartConfigElement(@Value("${multipart.maxFileSize}")String maxFileSize, @Value("${multipart.maxRequestSize}") String maxRequestSize) {
//        MultipartConfigFactory factory = new MultipartConfigFactory();
//        factory.setMaxFileSize(DataSize.parse(maxFileSize));
//        factory.setMaxRequestSize(DataSize.parse(maxRequestSize));
//        return factory.createMultipartConfig();
//    }

}
