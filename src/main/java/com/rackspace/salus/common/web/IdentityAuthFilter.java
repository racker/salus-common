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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rackspace.salus.common.config.IdentityProperties;
import com.rackspace.salus.common.model.TokenValidationResponse;
import com.rackspace.salus.common.model.TokenValidationResponse.Role;
import com.rackspace.salus.common.services.IdentityAuthenticationService;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.filter.GenericFilterBean;

@Slf4j
public class IdentityAuthFilter extends GenericFilterBean {

  public static final String HEADER_X_ROLES = "X-Roles";
  public static final String HEADER_X_IMPERSONATOR_ROLES = "X-Impersonator-Roles";
  public static final String HEADER_TENANT = "Requested-Tenant-Id";

  // some requests don't come through with a `tenantHeader` to validate
  // but do have a list of tenants to log for audit purposes.
  private final static String EXTRA_TENANT_HEADER = "X-Tenant-Id";

  private final DateTimeFormatter formatter = DateTimeFormatter
      .ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX");

  private RestTemplate restTemplate;
  private ObjectMapper objectMapper;
  private IdentityAuthenticationService identityAuthenticationService;
  private IdentityProperties identityProperties;
  boolean requireTenantId;

  public IdentityAuthFilter(IdentityAuthenticationService identityAuthenticationService,
      RestTemplate restTemplate, ObjectMapper objectMapper,
      IdentityProperties identityProperties, boolean requireTenantId) {
    this.identityAuthenticationService = identityAuthenticationService;
    this.restTemplate = restTemplate;
    this.objectMapper = objectMapper;
    this.identityProperties = identityProperties;
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
        String adminToken = identityAuthenticationService.getAdminToken(true);
        TokenValidationResponse tokenValidationResponse = callTokenValidationApi(identityProperties.getEndpoint() + "/" + xAuthToken,
            adminToken);
        if (isTokenValid(tokenValidationResponse)) {
          Optional<PreAuthenticatedToken> token = getToken(tokenValidationResponse, requireTenantId);
          if (token.isPresent()) {
            final PreAuthenticatedToken auth = token.get();
            SecurityContextHolder.getContext().setAuthentication(auth);
          }

          filterChain.doFilter(servletRequest, response);
        } else {
          prepareResponse(response, "Token Expired", "Bad Token", HttpStatus.UNAUTHORIZED
              .value());
        }
      } catch (RestClientException e) {
        log.error("Spring Security Filter Chain Exception:", e);
        prepareResponse(response, e.getLocalizedMessage(), "Bad Token",
            HttpStatus.INTERNAL_SERVER_ERROR
                .value());
      }
    }
  }

  Optional<PreAuthenticatedToken> getToken(TokenValidationResponse tokenValidationResponse, boolean requireTenantId) {
    log.debug("Getting PreAuthenticatedToken for request");
    final List<SimpleGrantedAuthority> roles = tokenValidationResponse.getAccess().getUser()
        .getRoles().stream()
        .map(role ->
            role.getName()
                .replace(':', '_')
                .replace('-', '_')
                .toUpperCase()
        )
        .map(a -> new SimpleGrantedAuthority("ROLE_" + a))
        .collect(Collectors.toList());

    final String tenant = String.valueOf(Math.abs(Integer.parseInt(tokenValidationResponse.getAccess().getToken().getTenant().getId())));

    StringBuilder extraTenantValues = new StringBuilder("");
    for(Role role : tokenValidationResponse.getAccess().getUser().getRoles()) {
      extraTenantValues.append(role.getTenantId()).append(",");
    }

    String extraTenantValuesString = extraTenantValues.toString();
    final String tenantList = extraTenantValuesString.substring(0, extraTenantValuesString.lastIndexOf(","));

    log.trace("Found tenant {} with roles {} while authenticating", tenant, roles);

    if (requireTenantId && !StringUtils.hasText(tenant)) {
      log.debug("Failed PreAuthenticatedToken creation due to missing {} header."
          + " {}={}, roles={}", HEADER_TENANT, EXTRA_TENANT_HEADER, tenantList, roles);
      return Optional.empty();
    }

    if (!roles.isEmpty()) {
      return Optional.of(new PreAuthenticatedToken(tenant, roles));
    } else {
      log.debug("Failed PreAuthenticatedToken creation due to empty roles list."
          + " tenant={}, roles={}", tenant, roles);
      return Optional.empty();
    }
  }

  private TokenValidationResponse callTokenValidationApi(String endpointUrl, String admintoken) {
    HttpHeaders headers = new HttpHeaders();
    headers.add("X-Auth-Token", admintoken);
    HttpEntity httpEntity = new HttpEntity(headers);
    ResponseEntity<String> responseEntity = restTemplate
        .exchange(endpointUrl, HttpMethod.GET, httpEntity, String.class);

    if (responseEntity.getStatusCodeValue() == 401) {
      log.info("401 error from callTokenValidationApi");
      return callTokenValidationApi(endpointUrl,
          identityAuthenticationService.getAdminToken(false));
    } else if (responseEntity.getStatusCodeValue() == 200) {
      log.info("200 success from callTokenValidationApi " + responseEntity.getBody());
      TokenValidationResponse tokenValidationResponse = null;
      try {
        tokenValidationResponse =
            objectMapper.readValue(responseEntity.getBody(), TokenValidationResponse.class);
        return tokenValidationResponse;
      } catch (JsonProcessingException e) {
        return null;
      }
    } else {
      log.info(responseEntity.getStatusCodeValue() + " error from callTokenValidationApi");
      throw new IllegalArgumentException(responseEntity.getBody());
    }
  }

  private void prepareResponse(HttpServletResponse httpServletResponse, String message,
      String error, int statusCode)
      throws IOException {
    Map<String, Object> response = new HashMap<>();

    response.put("timestamp", LocalDateTime.now().toString());
    response.put("error", error);
    response.put("message", message);
    response.put("status", statusCode);

    httpServletResponse.setContentType("application/json");
    httpServletResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    PrintWriter out = httpServletResponse.getWriter();
    out.println(objectMapper.writeValueAsString(response));
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
