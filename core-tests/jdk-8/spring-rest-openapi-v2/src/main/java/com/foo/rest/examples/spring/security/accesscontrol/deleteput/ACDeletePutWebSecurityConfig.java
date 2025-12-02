package com.foo.rest.examples.spring.security.accesscontrol.deleteput;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

@Configuration
@EnableWebSecurity
public class ACDeletePutWebSecurityConfig extends WebSecurityConfigurerAdapter {

    // security configuration
    protected void configure(HttpSecurity http) throws Exception {
        http.authorizeRequests()
                .antMatchers("/v2/api-docs").permitAll()
                .antMatchers(HttpMethod.GET, "/api/{x}").permitAll()
                .antMatchers(HttpMethod.PUT, "/api/{x}").hasAnyRole("CREATOR", "CONSUMER")
                .antMatchers(HttpMethod.DELETE, "/api/{x}").hasRole("CREATOR")
                .anyRequest().authenticated()
                .and()
                .httpBasic()
                .and()
                .csrf().disable();
    }

    // user credentials.
    @Bean
    @Override
    public UserDetailsService userDetailsService() {
        UserDetails creator0 =
                User.withDefaultPasswordEncoder()
                        .username("creator0")
                        .password("creator_password")
                        .roles("CREATOR")
                        .build();
        UserDetails creator1 =
                User.withDefaultPasswordEncoder()
                        .username("creator1")
                        .password("creator_password")
                        .roles("CREATOR")
                        .build();
//        UserDetails user2 =
//                User.withDefaultPasswordEncoder()
//                        .username("consumer1")
//                        .password("consumer1_password")
//                        .roles("CONSUMER")
//                        .build();
//
//        UserDetails user3 =
//                User.withDefaultPasswordEncoder()
//                        .username("consumer2")
//                        .password("consumer2_password")
//                        .roles("CONSUMER")
//                        .build();

        InMemoryUserDetailsManager manager = new InMemoryUserDetailsManager();

        manager.createUser(creator0);
        manager.createUser(creator1);
//        manager.createUser(user2);
//        manager.createUser(user3);
        return manager;
    }
}
