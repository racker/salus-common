/*
 * Copyright 2021 Rackspace US, Inc.
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
import com.rackspace.salus.common.model.TokenValidationResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
public class IdentityTokenValidationService {

  private final IdentityAdminAuthService identityAdminAuthService;
  private final RestTemplate restTemplate;
  private final IdentityProperties identityProperties;

  @Autowired
  public IdentityTokenValidationService(IdentityAdminAuthService identityAdminAuthService,
      RestTemplate restTemplate, IdentityProperties identityProperties) {
    this.identityAdminAuthService = identityAdminAuthService;
    this.restTemplate = restTemplate;
    this.identityProperties = identityProperties;
  }

  public TokenValidationResponse validateToken(String xAuthToken) {
    String endpointUrl = identityProperties.getEndpoint()+ "/" + xAuthToken;
    String adminToken = "";
    try {
      adminToken = identityAdminAuthService.getAdminToken(true);
      return callTokenValidationApi(endpointUrl, adminToken);
    } catch (RestClientResponseException e) {
      if (e.getRawStatusCode() == 401) {
        adminToken = identityAdminAuthService.getAdminToken(false);
        return callTokenValidationApi(endpointUrl, adminToken);
      }
    }
    return null;
  }


  public TokenValidationResponse callTokenValidationApi(String endpointUrl, String adminToken) {
    HttpHeaders headers = new HttpHeaders();
    headers.add("X-Auth-Token", adminToken);
    HttpEntity httpEntity = new HttpEntity(headers);
    ResponseEntity<TokenValidationResponse> responseEntity = restTemplate
        .exchange(endpointUrl, HttpMethod.GET, httpEntity, TokenValidationResponse.class);
    return responseEntity.getBody();
  }
}
