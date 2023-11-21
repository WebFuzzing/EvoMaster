package com.foo.rest.examples.spring.securitytest;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
public class SecurityWebConfig extends WebSecurityConfigurerAdapter {

    @Override
    public void configure(HttpSecurity http)  throws Exception {

        http.authorizeRequests()
                .antMatchers("/v2/api-docs").permitAll()
                .antMatchers("/login").permitAll()
                .antMatchers("/publicInformation").permitAll()
                .antMatchers(HttpMethod.GET, "/userA").permitAll()
                .antMatchers(HttpMethod.POST, "/userA").hasRole("USERA")
                .antMatchers(HttpMethod.DELETE, "/userA").hasRole("USERA")
                .antMatchers(HttpMethod.GET, "/userB").permitAll()
                .antMatchers(HttpMethod.POST, "/userB").hasRole("USERB")
                .antMatchers(HttpMethod.DELETE, "/userB").hasRole("USERB")
                .antMatchers(HttpMethod.GET, "/userAPrivate").hasRole("USERA")
                .and()
                .formLogin()
                .and()
                .exceptionHandling()
                .defaultAuthenticationEntryPointFor(getRestAuthenticationEntryPoint(), new AntPathRequestMatcher("/**"))
                .and()
                .csrf().disable()
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED);
    }

    private AuthenticationEntryPoint getRestAuthenticationEntryPoint() {
        return new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED);
    }

    @Override
    public void configure(AuthenticationManagerBuilder auth)  throws Exception {

        auth.inMemoryAuthentication()
                .withUser("UserA").password("{noop}userA").roles("USERA").and()
                .withUser("UserB").password("{noop}userB").roles("USERB");

    }
}
