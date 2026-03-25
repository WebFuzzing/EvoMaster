package com.foo.rest.examples.spring.formlogin;

import org.springframework.context.annotation.Configuration;
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
public class FormLoginWebSecurityConfig extends WebSecurityConfigurerAdapter {

    @Override
    public void configure(HttpSecurity http)  throws Exception {

        http.authorizeRequests()
                .antMatchers("/v2/api-docs").permitAll()
                .antMatchers("/login").permitAll()
                .antMatchers("/api/formlogin/openToAll").permitAll()
                .antMatchers("/api/formlogin/forUsers").hasRole("USER")
                .antMatchers("/api/formlogin/forAdmins").hasRole("ADMIN")
                .anyRequest().denyAll()
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
                .withUser("foo").password("{noop}123456").roles("USER").and()
                .withUser("admin").password("{noop}bar").roles("ADMIN", "USER");
    }
}