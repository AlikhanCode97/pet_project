package com.example.Games.config.test;

import com.example.Games.config.security.JwtAuthenticationFilter;
import com.example.Games.config.security.JwtService;
import com.example.Games.config.security.TokenBlacklistService;
import com.example.Games.config.security.CustomUserDetailsService;
import com.example.Games.config.security.SecurityConfig;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.core.annotation.AliasFor;
import org.springframework.test.context.ActiveProfiles;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@WebMvcTest(
    excludeFilters = {
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
            SecurityConfig.class,           // Our custom security configuration
            JwtAuthenticationFilter.class,  // Our JWT authentication filter
            JwtService.class,              // Our JWT service
            TokenBlacklistService.class,   // Our token blacklist service
            CustomUserDetailsService.class // Our user details service
        })
    }
)
@ImportAutoConfiguration(exclude = {SecurityAutoConfiguration.class})
@ActiveProfiles("test")
public @interface WebMvcTestWithoutSecurity {

    /**
     * Specifies the controllers to test. This is an alias for the WebMvcTest controllers attribute.
     */
    @AliasFor(annotation = WebMvcTest.class, attribute = "controllers")
    Class<?>[] value() default {};

    /**
     * Specifies the controllers to test. This is an alias for the WebMvcTest controllers attribute.
     */
    @AliasFor(annotation = WebMvcTest.class, attribute = "controllers")
    Class<?>[] controllers() default {};
}
