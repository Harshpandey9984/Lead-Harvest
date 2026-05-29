package com.company.scraper.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   @Value("${security.api-key:}") String apiKey) throws Exception {
        ApiKeyAuthFilter filter = new ApiKeyAuthFilter(apiKey);
        http.csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/**").permitAll()
                .anyRequest().authenticated())
            .addFilterBefore(filter, UsernamePasswordAuthenticationFilter.class)
            .httpBasic(httpBasic -> httpBasic.disable())
            .formLogin(form -> form.disable());
        return http.build();
    }
}
