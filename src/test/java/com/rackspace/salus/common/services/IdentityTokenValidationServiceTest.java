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

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestToUriTemplate;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rackspace.salus.common.config.IdentityConfig;
import com.rackspace.salus.common.config.IdentityProperties;
import com.rackspace.salus.common.model.TokenValidationResponse;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureMockRestServiceServer;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.client.MockRestServiceServer;
import uk.co.jemos.podam.api.PodamFactory;
import uk.co.jemos.podam.api.PodamFactoryImpl;

@RunWith(SpringRunner.class)
@Import({IdentityProperties.class, ObjectMapper.class})
@TestPropertySource(properties = {
    "salus.identity.endpoint=http://this-is-a-non-null-value",
    "salus.identity.admin-username=testUser",
    "salus.identity.admin-password=testPass"
})
@EnableAutoConfiguration
@AutoConfigureMockRestServiceServer
@ComponentScan("com.rackspace.salus.common.services")
public class IdentityTokenValidationServiceTest {

  @Autowired
  MockRestServiceServer mockServer;

  @Autowired
  private ObjectMapper objectMapper;

  @MockBean
  IdentityAdminAuthService identityAdminAuthService;

  @Autowired
  private IdentityTokenValidationService identityTokenValidationService;

  private PodamFactory podamFactory = new PodamFactoryImpl();

  @Test
  public void testValidateToken() throws Exception {
    String xAuthToken = "abc";
    when(identityAdminAuthService.getAdminToken(true)).thenReturn("xyz");
    TokenValidationResponse tokenValidationResponseExpected = podamFactory.manufacturePojo(TokenValidationResponse.class);

    String url = String.format("http://this-is-a-non-null-value/v2.0/tokens/%s", xAuthToken);

    mockServer.expect(requestToUriTemplate(url))
        .andExpect(header(IdentityConfig.IDENTITY_API_X_AUTH_HEADER, "xyz"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(
            withSuccess(objectMapper.writeValueAsString(tokenValidationResponseExpected),
                MediaType.APPLICATION_JSON));

    TokenValidationResponse tokenValidationResponseActual = identityTokenValidationService.validateToken(xAuthToken);

    verify(identityAdminAuthService).getAdminToken(true);
  }
}
