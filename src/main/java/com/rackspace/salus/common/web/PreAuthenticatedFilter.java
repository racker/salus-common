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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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

  // some requests don't come through with a `tenantHeader` to validate
  // but do have a list of tenants to log for audit purposes.
  private final static String EXTRA_TENANT_HEADER = "X-Tenant-Id";

  private final String tenantHeader;
  private final List<String> rolesHeaders;
  private final boolean requireTenantId;

  public PreAuthenticatedFilter(String tenantHeader, List<String> rolesHeaders, boolean requireTenantId) {
    this.tenantHeader = tenantHeader;
    this.rolesHeaders = rolesHeaders;
    this.requireTenantId = requireTenantId;
  }

  @Override
  public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse,
                       FilterChain chain) throws IOException, ServletException {
    if (servletRequest instanceof HttpServletRequest) {
      final HttpServletRequest req = (HttpServletRequest) servletRequest;
      Optional<PreAuthenticatedToken> token = getToken(req);
      if (token.isPresent()) {
        final PreAuthenticatedToken auth = token.get();
        log.debug("Processed Repose-driven authentication={}", auth);
        SecurityContextHolder.getContext().setAuthentication(auth);
      }
    }
    chain.doFilter(servletRequest, servletResponse);
  }

  Optional<PreAuthenticatedToken> getToken(HttpServletRequest req) {
    log.debug("Getting PreAuthenticatedToken for request");
    final Set<String> rolesSet = new HashSet<>();
    for (String header : rolesHeaders) {
      String roleString = req.getHeader(header);
      if (roleString != null) {
        List<String> roleValues = Arrays.asList(roleString.split(","));
        rolesSet.addAll(roleValues);
      }
    }
    final String tenant = req.getHeader(tenantHeader);
    final String tenantList = req.getHeader(EXTRA_TENANT_HEADER);
    log.trace("Found tenant {} with roles {} while authenticating", tenant, rolesSet);

    if (requireTenantId && !StringUtils.hasText(tenant)) {
      log.debug("Failed PreAuthenticatedToken creation due to missing {} header."
          + " {}={}, roles={}", tenantHeader, EXTRA_TENANT_HEADER, tenantList, rolesSet);
      return Optional.empty();
    }

    if (!rolesSet.isEmpty()) {
      final List<SimpleGrantedAuthority> roles = rolesSet.stream()
          .map(role ->
              role
                  .replace(':', '_')
                  .replace('-', '_')
                  .toUpperCase()
          )
          .map(a -> new SimpleGrantedAuthority("ROLE_" + a))
          .collect(toList());

      return Optional.of(new PreAuthenticatedToken(tenant, roles));
    } else {
      log.debug("Failed PreAuthenticatedToken creation due to empty roles list."
          + " tenant={}, roles={}", tenant, rolesSet);
      return Optional.empty();
    }
  }
}
