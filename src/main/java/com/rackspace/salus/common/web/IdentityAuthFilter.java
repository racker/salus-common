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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rackspace.salus.common.model.TokenValidationResponse;
import com.rackspace.salus.common.model.TokenValidationResponse.Role;
import com.rackspace.salus.common.services.IdentityTokenValidationService;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.security.sasl.AuthenticationException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.filter.GenericFilterBean;

@Slf4j
public class IdentityAuthFilter extends GenericFilterBean {

  public static final String HEADER_X_ROLES = "X-Roles";
  public static final String HEADER_X_IMPERSONATOR_ROLES = "X-Impersonator-Roles";
  public static final String HEADER_TENANT = "Requested-Tenant-Id";

  // some requests don't come through with a `tenantHeader` to validate
  // but do have a list of tenants to log for audit purposes.
  private final static String EXTRA_TENANT_HEADER = "X-Tenant-Id";

  private final ObjectMapper objectMapper;
  private final IdentityTokenValidationService identityTokenValidationService;
  boolean requireTenantId;

  private @Value("${spring.application.name}")
  String appName;

  //  private Tracer tracer;
  private static final String ATTRIBUTE_TRACE_ID = "traceId";
  private static final String ATTRIBUTE_APP = "app";
  private static final String ATTRIBUTE_HOST = "host";
  private static final String ATTRIBUTE_PATH = "path";

  public IdentityAuthFilter(IdentityTokenValidationService identityTokenValidationService,
      ObjectMapper objectMapper, boolean requireTenantId) {
    this.objectMapper = objectMapper;
    this.identityTokenValidationService = identityTokenValidationService;
    this.requireTenantId = requireTenantId;
  }

  @Override
  public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse,
      FilterChain filterChain) throws IOException, ServletException {
    if (servletRequest instanceof HttpServletRequest) {
      final HttpServletRequest request = (HttpServletRequest) servletRequest;
      final HttpServletResponse response = (HttpServletResponse) servletResponse;

      String xAuthToken = request.getHeader("x-auth-token");
      try {
        TokenValidationResponse tokenValidationResponse = identityTokenValidationService.validateToken(xAuthToken);
        if (isTokenValid(tokenValidationResponse)) {
          Map<String, String> headersMap = getIdentityResponseAsMap(request,
              tokenValidationResponse);
          servletRequest.setAttribute("identityHeadersMap", headersMap);
          Optional<PreAuthenticatedToken> token = getToken(headersMap, requireTenantId);
          if (token.isPresent()) {
            final PreAuthenticatedToken auth = token.get();
            SecurityContextHolder.getContext().setAuthentication(auth);
          }
          filterChain.doFilter(servletRequest, response);
        } else {
          throw new RestClientException("Unauthorized Access");
        }
      } catch (RestClientException e) {
        log.error("Spring Security Filter Chain Exception:", e);
        throw new AuthenticationException(e.getLocalizedMessage());
//        prepareResponse(response, e.getLocalizedMessage(), "Bad Token",
//            HttpStatus.INTERNAL_SERVER_ERROR
//                .value());
//        throw e;
      }
    }
  }

  Optional<PreAuthenticatedToken> getToken(Map<String, String> headersMap,
      boolean requireTenantId) {
    log.debug("Getting PreAuthenticatedToken for request");

    final Set<String> rolesSet = new HashSet<>();
    for (String header : headersMap.keySet()) {
      String roleString = headersMap.get(header);
      if (roleString != null) {
        List<String> roleValues = Arrays.asList(roleString.split(","));
        rolesSet.addAll(roleValues);
      }
    }

    final String tenant = headersMap.get(HEADER_TENANT);
    final String tenantList = headersMap.get(EXTRA_TENANT_HEADER);
    log.trace("Found tenant {} with roles {} while authenticating", tenant, rolesSet);

    if (requireTenantId && !StringUtils.hasText(tenant)) {
      log.debug("Failed PreAuthenticatedToken creation due to missing {} header."
          + " {}={}, roles={}", HEADER_TENANT, EXTRA_TENANT_HEADER, tenantList, rolesSet);
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
          .collect(Collectors.toList());

      return Optional.of(new PreAuthenticatedToken(tenant, roles));
    } else {
      log.debug("Failed PreAuthenticatedToken creation due to empty roles list."
          + " tenant={}, roles={}", tenant, rolesSet);
      return Optional.empty();
    }
  }

  private void prepareResponse(HttpServletResponse httpServletResponse, String message,
      String error, int statusCode)
      throws IOException {
    Map<String, Object> response = new HashMap<>();

    response.put("timestamp", LocalDateTime.now().toString());
    response.put("status", statusCode);
    response.put("error", error);
    response.put("message", message);

//    response.put("ATTRIBUTE_PATH")
    response.put(ATTRIBUTE_APP, appName);
    response.put(ATTRIBUTE_HOST, InetAddress.getLocalHost().getHostName());

//    final Span currentSpan = tracer.currentSpan();
//    if (currentSpan != null) {
//      errorAttributes.put(ATTRIBUTE_TRACE_ID, currentSpan.context().traceIdString());
//    }

    httpServletResponse.setContentType("application/json");
    httpServletResponse.setStatus(statusCode);
    PrintWriter out = httpServletResponse.getWriter();
    out.println(objectMapper.writeValueAsString(response));
  }

  private Map<String, String> getIdentityResponseAsMap(HttpServletRequest request,
      TokenValidationResponse tokenValidationResponse) {
    Map<String, String> attributes = new HashMap<>();

    String xRolesValues = tokenValidationResponse.getAccess().getUser().getRoles().stream()
        .map(Role::getName)
        .collect(Collectors.joining(","));

    String extraTenantValues = tokenValidationResponse.getAccess().getUser().getRoles().stream()
        .map(Role::getTenantId)
        .collect(Collectors.joining(","));

    //TODO - for local only - Remove before committing
    String tenantIdFromResponse = String.valueOf(Math.abs(
        Integer.parseInt(tokenValidationResponse.getAccess().getToken().getTenant().getId())));

//    String tenantIdFromResponse = String.valueOf(Math.abs(Integer.parseInt(tokenValidationResponse.getAccess().getToken().getTenant().getId());

    attributes.put(HEADER_X_ROLES, xRolesValues);
    attributes.put(EXTRA_TENANT_HEADER, extraTenantValues);
    attributes.put(HEADER_TENANT, tenantIdFromResponse);
    return attributes;
  }

  private boolean isTokenValid(TokenValidationResponse tokenValidationResponse) {
    if (Instant.now()
        .isAfter(Instant.parse(tokenValidationResponse.getAccess().getToken().getExpires()))) {
      log.info("invalidated token");
      return false;
    } else {
      log.info("validated token");
      return true;
    }
  }
}
