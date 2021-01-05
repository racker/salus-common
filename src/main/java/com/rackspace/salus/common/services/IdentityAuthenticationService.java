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
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
public class IdentityAuthenticationService {

  private RestTemplate restTemplate;
  private final IdentityProperties identityProperties;

  @Autowired
  public IdentityAuthenticationService(RestTemplate restTemplate, IdentityProperties identityProperties) {
    this.restTemplate = restTemplate;
    this.identityProperties = identityProperties;
  }

  @Cacheable(value = "identity_admin_tokens", condition = "#useCache")
  public String getAdminToken(boolean useCache) {
    log.info("getting admin token");
    HttpHeaders headers = new HttpHeaders();
    headers.add("Content-Type", MediaType.APPLICATION_JSON_VALUE);

    AdminTokenRequest adminTokenRequest = new AdminTokenRequest(
        new Auth(new PasswordCredentials(
            identityProperties.getAdminUsername(), identityProperties.getAdminPassword(), null)));

    HttpEntity httpEntity = new HttpEntity(adminTokenRequest, headers);

    log.info("hitting {} with body {}",identityProperties.getEndpoint(), adminTokenRequest);
    ResponseEntity<AdminTokenResponse> responseEntity = restTemplate
        .exchange(identityProperties.getEndpoint(), HttpMethod.POST, httpEntity, AdminTokenResponse.class);
    if(responseEntity.getStatusCode().is2xxSuccessful()) {
      log.info("Success from admin token api");
      return responseEntity.getBody().getAccess().getToken().getId();
    } else  {
      throw new RestClientException("Error occurred");
    }
  }
}
