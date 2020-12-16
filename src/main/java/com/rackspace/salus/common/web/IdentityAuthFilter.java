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

import static java.util.stream.Collectors.toList;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rackspace.salus.common.model.TokenValidationResponse;
import com.rackspace.salus.common.model.TokenValidationResponse.Role;
import com.rackspace.salus.common.util.IdentityServiceUtils;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
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

  private @Value("${identity-endpoint}")
  String identityEndpoint = "https://identity-internal.api.rackspacecloud.com/v2.0/tokens";

  private @Autowired
  IdentityServiceUtils identityServiceUtils = new IdentityServiceUtils();

  private RestTemplate restTemplate = new RestTemplate();

  private ObjectMapper objectMapper = new ObjectMapper();

  private @Autowired
  CacheManager cacheManager;

  private final DateTimeFormatter formatter = DateTimeFormatter
      .ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX");

  @Override
  public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse,
      FilterChain filterChain) throws IOException, ServletException {
    if (servletRequest instanceof HttpServletRequest) {
      final HttpServletRequest request = (HttpServletRequest) servletRequest;
      final HttpServletResponse response = (HttpServletResponse) servletResponse;

      String xAuthToken = request.getHeader("x-auth-token");
      String endpointUrl = identityEndpoint + "/" + xAuthToken;

      String adminToken = identityServiceUtils.getAdminToken(true);
      try {
        TokenValidationResponse tokenValidationResponse = callTokenValidationApi(endpointUrl,
            adminToken);
        if (checkTokenExpired(tokenValidationResponse)) {
          addCustomHeaders(servletRequest, tokenValidationResponse);

          CustomHttpServletRequest mutableRequest = addCustomHeaders(servletRequest, tokenValidationResponse);

          final List<SimpleGrantedAuthority> roles = tokenValidationResponse.getAccess().getUser().getRoles().stream()
              .map(role ->
                  role.getName()
                      .replace(':', '_')
                      .replace('-', '_')
                      .toUpperCase()
              )
              .map(a -> new SimpleGrantedAuthority("ROLE_" + a))
              .collect(toList());

          final PreAuthenticatedToken auth = new PreAuthenticatedToken(
              tokenValidationResponse.getAccess().getToken().getTenant().getId(), roles);

          SecurityContextHolder.getContext().setAuthentication(auth);

          filterChain.doFilter(mutableRequest, response);
        } else {
          prepareResponse(response, "Token Expired", "Bad Token", HttpStatus.NOT_FOUND
              .value());
        }
      } catch (RestClientException e) {
        log.error("Spring Security Filter Chain Exception:", e);
        prepareResponse(response, e.getLocalizedMessage(), "Bad Token", HttpStatus.NOT_FOUND
            .value());
      }
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
      return callTokenValidationApi(endpointUrl, identityServiceUtils.getAdminToken(false));
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
    httpServletResponse.setStatus(HttpServletResponse.SC_NOT_FOUND);
    PrintWriter out = httpServletResponse.getWriter();
    out.println(objectMapper.writeValueAsString(response));
  }

  private boolean checkTokenExpired(TokenValidationResponse tokenValidationResponse) {
    if (LocalDateTime.now(ZoneId.of("UTC"))
        .isAfter(LocalDateTime
            .parse(tokenValidationResponse.getAccess().getToken().getExpires(), formatter))) {
      log.info("invalidated token");
      return false;
    } else {
      log.info("validated token");
      return true;
    }
  }

  private CustomHttpServletRequest addCustomHeaders(ServletRequest request, TokenValidationResponse tokenValidationResponse) {
    HttpServletRequest req = (HttpServletRequest) request;
    CustomHttpServletRequest mutableRequest = new CustomHttpServletRequest(req);

    StringBuilder xRolesValues = new StringBuilder("");
    for(Role role : tokenValidationResponse.getAccess().getUser().getRoles()) {
      xRolesValues.append(role.getName()).append(",");
    }

    String xRolesValuesString = xRolesValues.toString();
    xRolesValuesString = xRolesValuesString.substring(0, xRolesValuesString.lastIndexOf(",")-1);

    mutableRequest.putHeader(HEADER_X_ROLES, xRolesValuesString);



    StringBuilder extraTenantValues = new StringBuilder("");
    for(Role role : tokenValidationResponse.getAccess().getUser().getRoles()) {
      extraTenantValues.append(role.getTenantId()).append(",");
    }

    String extraTenantValuesString = extraTenantValues.toString();
    extraTenantValuesString = extraTenantValuesString.substring(0, extraTenantValuesString.lastIndexOf(",")-1);

    mutableRequest.putHeader(EXTRA_TENANT_HEADER, extraTenantValuesString);

    mutableRequest.putHeader(HEADER_TENANT, tokenValidationResponse.getAccess().getToken().getTenant().getId());

//    tokenValidationResponse.getAccess().getUser().getRoles().stream().map(e -> xRolesValues.append(e.getName()).append(","));


    return mutableRequest;
  }
}
