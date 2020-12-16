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

package com.rackspace.salus.common.util;

import com.rackspace.salus.common.model.AdminTokenRequest;
import com.rackspace.salus.common.model.AdminTokenRequest.Auth;
import com.rackspace.salus.common.model.AdminTokenRequest.PasswordCredentials;
import com.rackspace.salus.common.model.AdminTokenResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
public class IdentityServiceUtils {

  private @Autowired
  RestTemplate restTemplate = new RestTemplate();

  private @Value("${identity-endpoint}") String identityEndpoint = "https://identity-internal.api.rackspacecloud.com/v2.0/tokens";
  private @Value("${identity-admin-username}") String adminUsername = "cloudMAAS";
  private @Value("${identity-admin-password}") String adminPassword = "dr7jP26iv4BNR9bA";

  @Cacheable(value = "tokens", condition = "#useCache")
  public String getAdminToken(boolean useCache) {
    log.info("getting admin token");
    HttpHeaders headers = new HttpHeaders();
    headers.add("Content-Type", MediaType.APPLICATION_JSON_VALUE);

    AdminTokenRequest adminTokenRequest = new AdminTokenRequest(
        new Auth(new PasswordCredentials(adminUsername, adminPassword)));

    HttpEntity httpEntity = new HttpEntity(adminTokenRequest, headers);

    log.info("hitting "+identityEndpoint);
    ResponseEntity<AdminTokenResponse> responseEntity = restTemplate.exchange(identityEndpoint, HttpMethod.POST, httpEntity, AdminTokenResponse.class);
    if(responseEntity.getStatusCode().is4xxClientError()) {
      throw new RestClientException("Error occurred");
    } else  {
      log.info("Success from admin token api");
      return responseEntity.getBody().getAccess().getToken().getId();
    }
  }
}
