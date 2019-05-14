/*
 * Copyright 2019 Rackspace US, Inc.
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

import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.GenericFilterBean;

/**
 * This is a general purpose Spring Security filter which translates the given tenant/user and roles
 * headers into the equivalent values in a {@link PreAuthenticatedToken}
 */
@Slf4j
public class PreAuthenticatedFilter extends GenericFilterBean {

  private final String tenantHeader;
  private final String rolesHeader;

  public PreAuthenticatedFilter(String tenantHeader, String rolesHeader) {
    this.tenantHeader = tenantHeader;
    this.rolesHeader = rolesHeader;
  }

  @Override
  public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse,
                       FilterChain chain) throws IOException, ServletException {
    if (servletRequest instanceof HttpServletRequest) {
      final HttpServletRequest req = (HttpServletRequest) servletRequest;

      final Enumeration<String> roleValues = req.getHeaders(rolesHeader);
      final String tenant = req.getHeader(tenantHeader);

      if (roleValues.hasMoreElements() && (StringUtils.hasText(tenant))) {
        final List<String> rolesList = new ArrayList<>();
        while (roleValues.hasMoreElements()) {
          rolesList.add(roleValues.nextElement());
        }

        final List<SimpleGrantedAuthority> roles = rolesList.stream()
            .map(role ->
                role
                    .replace(':', '_')
                    .replace('-', '_')
                    .toUpperCase()
            )
            .map(a -> new SimpleGrantedAuthority("ROLE_" + a))
            .collect(toList());

        final PreAuthenticatedToken auth = new PreAuthenticatedToken(tenant, roles);

        log.debug("Processed Repose-driven authentication={}", auth);
        SecurityContextHolder.getContext().setAuthentication(auth);
      }
    }

    chain.doFilter(servletRequest, servletResponse);
  }
}