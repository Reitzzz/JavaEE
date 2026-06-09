package com.example.smartlibrary.config;

import com.example.smartlibrary.service.DatabaseUserDetailsService;
import com.example.smartlibrary.service.CaptchaService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CaptchaLoginFilter captchaLoginFilter(CaptchaService captchaService) {
        return new CaptchaLoginFilter(captchaService);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            DatabaseUserDetailsService userDetailsService,
            CaptchaLoginFilter captchaLoginFilter
    ) throws Exception {
        return http
                .userDetailsService(userDetailsService)
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login", "/login.html", "/register", "/register.html", "/css/**", "/js/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/captcha").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/register").permitAll()
                        .requestMatchers("/api/users", "/api/users/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/books/**", "/api/categories/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/books/**", "/api/categories/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/books/**", "/api/categories/**").hasRole("ADMIN")
                        .requestMatchers("/api/ai/models", "/api/ai/models/**").hasRole("ADMIN")
                        .requestMatchers("/api/ai/settings", "/api/ai/settings/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/borrows").hasRole("ADMIN")
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .defaultSuccessUrl("/", true)
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login")
                        .permitAll())
                .addFilterBefore(captchaLoginFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
