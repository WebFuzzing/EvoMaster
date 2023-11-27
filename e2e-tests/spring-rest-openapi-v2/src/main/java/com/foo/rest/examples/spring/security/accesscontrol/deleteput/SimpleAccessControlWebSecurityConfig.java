package com.foo.rest.examples.spring.security.accesscontrol.deleteput;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
public class SimpleAccessControlWebSecurityConfig extends WebSecurityConfigurerAdapter {

    protected void configure(HttpSecurity http) throws Exception {
        http.authorizeRequests()
                .antMatchers("/v2/api-docs").permitAll()
                .antMatchers("/api/**").permitAll()
                .and()
                .httpBasic()
                .and()
                .csrf().disable();
    }

    @Bean
    @Override
    public UserDetailsService userDetailsService() {
        UserDetails user =
                User.withDefaultPasswordEncoder()
                        .username("creator")
                        .password("creator_password")
                        .roles("CREATOR")
                        .build();
        UserDetails user2 =
                User.withDefaultPasswordEncoder()
                        .username("consumer1")
                        .password("consumer1_password")
                        .roles("CONSUMER")
                        .build();
        InMemoryUserDetailsManager manager = new InMemoryUserDetailsManager();

        UserDetails user3 =
                User.withDefaultPasswordEncoder()
                        .username("consumer2")
                        .password("consumer2_password")
                        .roles("CONSUMER")
                        .build();

        manager.createUser(user);
        manager.createUser(user2);
        manager.createUser(user3);
        return manager;
    }
}
