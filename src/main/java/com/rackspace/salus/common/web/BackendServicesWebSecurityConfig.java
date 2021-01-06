/*
 * Copyright 2020 Rackspace US, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.rackspace.salus.common.web;

import com.rackspace.salus.common.config.RoleProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.firewall.DefaultHttpFirewall;
import org.springframework.security.web.firewall.HttpFirewall;

/**
 * This class is used to populate roles into the spring security context.
 *
 * That context will later be used by {@link RoleBasedJsonViewControllerAdvice} to perform
 * specific json view serializations based on the access level the roles provide.
 */
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties({RoleProperties.class})
@Slf4j
public class BackendServicesWebSecurityConfig extends WebSecurityConfigurerAdapter {

  /**
   * This is required to populate the spring security context with the roles
   * passed down from the public/admin api.
   *
   * As authentication has already been performed by those apis this can allow all
   * requests through without further validation.
   *
   * @param http
   * @throws Exception
   */
  @Override
  protected void configure(HttpSecurity http) throws Exception {
    log.debug("Configuring tenant web security");
    http
        .csrf().disable()
        .addFilterBefore(
            new ReposeHeaderFilter(false),
            BasicAuthenticationFilter.class)
        .authorizeRequests()
        .antMatchers("/api/**")
        .permitAll();
  }

  /**
   * The default firewall is strict and does not allow for cross-service
   * requests to include ":"
   *
   * StrictHttpFirewall should still be used at the initial entry point but is not needed for
   * cross-service requests.
   */
  @Bean
  public HttpFirewall nonStrictHttpFirewall() {
    return new DefaultHttpFirewall();
  }
}
