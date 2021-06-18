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

package com.rackspace.salus.common.web;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rackspace.salus.common.config.IdentityConfig;
import com.rackspace.salus.common.services.IdentityTokenValidationService;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.hamcrest.core.StringStartsWith;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import uk.co.jemos.podam.api.PodamFactory;
import uk.co.jemos.podam.api.PodamFactoryImpl;

@RunWith(MockitoJUnitRunner.class)
public class IdentityAuthFilterTest {

  @Mock
  IdentityTokenValidationService identityTokenValidationService;

  @Autowired
  ObjectMapper objectMapper;

  private PodamFactory podamFactory = new PodamFactoryImpl();

  @Test
  public void testGetToken() {
    IdentityAuthFilter identityAuthFilter = new IdentityAuthFilter(
        identityTokenValidationService, objectMapper, true);

    Map<String, String> attributes = new HashMap<>();
    attributes.put(IdentityConfig.HEADER_X_ROLES, "monitoring:admin,dedicated:default,ticketing:admin,identity:user-admin");
    attributes.put(IdentityConfig.EXTRA_TENANT_HEADER, "12345");
    attributes.put(IdentityConfig.HEADER_TENANT, "12345");

    Optional<PreAuthenticatedToken> token = identityAuthFilter.getToken(attributes, true);

    assertTrue(token.isPresent());
    assertThat(token.get().getAuthorities(), hasSize(4));
    for (GrantedAuthority authority : token.get().getAuthorities()) {
      assertThat(authority.getAuthority(), StringStartsWith.startsWith("ROLE_"));
    }
  }

  @Test
  public void testGetTokenNoRoles() {
    IdentityAuthFilter identityAuthFilter = new IdentityAuthFilter(
        identityTokenValidationService, objectMapper, true);

    Map<String, String> attributes = new HashMap<>();
    attributes.put(IdentityConfig.EXTRA_TENANT_HEADER, "12345");
    attributes.put(IdentityConfig.HEADER_TENANT, "12345");

    Optional<PreAuthenticatedToken> token = identityAuthFilter.getToken(attributes, true);

    assertFalse(token.isPresent());
  }

  @Test
  public void testGetTokenNoTenant() {
    IdentityAuthFilter identityAuthFilter = new IdentityAuthFilter(
        identityTokenValidationService, objectMapper, true);

    Map<String, String> attributes = new HashMap<>();
    attributes.put(IdentityConfig.HEADER_X_ROLES, "monitoring:admin,dedicated:default,ticketing:admin,identity:user-admin");

    Optional<PreAuthenticatedToken> token = identityAuthFilter.getToken(attributes, true);

    assertFalse(token.isPresent());
  }
}
