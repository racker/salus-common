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

package com.rackspace.salus.common.services;

import com.rackspace.salus.common.config.IdentityProperties;
import com.rackspace.salus.common.model.AdminTokenRequest;
import com.rackspace.salus.common.model.AdminTokenRequest.Auth;
import com.rackspace.salus.common.model.AdminTokenRequest.PasswordCredentials;
import com.rackspace.salus.common.model.AdminTokenResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Service class to get identity admin token using admin credentials from config which will be used
 * to validate x-auth-token in api request header
 */
@Service
@Slf4j
public class IdentityAdminAuthService {

  private RestTemplate restTemplate;
  private final IdentityProperties identityProperties;

  @Autowired
  public IdentityAdminAuthService(RestTemplateBuilder restTemplateBuilder,
                                  IdentityProperties identityProperties) {
    this.restTemplate = restTemplateBuilder
      .rootUri(identityProperties.getEndpoint())
      .build();
    this.identityProperties = identityProperties;
  }

  @Cacheable(cacheNames = "identityTokenCache", condition = "#useCache")
  public String getAdminToken(boolean useCache) {
    log.info("getting admin token");
    HttpHeaders headers = new HttpHeaders();
    headers.add("Content-Type", MediaType.APPLICATION_JSON_VALUE);

    AdminTokenRequest adminTokenRequest = new AdminTokenRequest(
        new Auth().setPasswordCredentials(new PasswordCredentials(
            identityProperties.getAdminUsername(), identityProperties.getAdminPassword())));

    HttpEntity httpEntity = new HttpEntity(adminTokenRequest, headers);

    log.debug("hitting {} ", identityProperties.getEndpoint());
    ResponseEntity<AdminTokenResponse> responseEntity = restTemplate
        .exchange("/v2.0/tokens", HttpMethod.POST, httpEntity,
            AdminTokenResponse.class);
    return responseEntity.getBody().getAccess().getToken().getId();
  }
}
