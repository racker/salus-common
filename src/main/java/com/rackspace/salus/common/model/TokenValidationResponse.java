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

package com.rackspace.salus.common.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Data;

@Data
public class TokenValidationResponse {

  @JsonProperty("access")
  private Access access;

  @Data
  public static class Access {

    @JsonProperty("user")
    private User user;
    @JsonProperty("token")
    private Token token;
  }

  @Data
  public static class Token {

    @JsonProperty("expires")
    private String expires;
    @JsonProperty("RAX-AUTH:issued")
    private String rAXAUTHIssued;
    @JsonProperty("RAX-AUTH:authenticatedBy")
    private List<String> rAXAUTHAuthenticatedBy = null;
    @JsonProperty("id")
    private String id;
    @JsonProperty("tenant")
    private Tenant tenant;
  }

  @Data
  public static class Tenant {

    @JsonProperty("name")
    private String name;
    @JsonProperty("id")
    private String id;
  }

  @Data
  public static class User {

    @JsonProperty("RAX-AUTH:defaultRegion")
    private String rAXAUTHDefaultRegion;
    @JsonProperty("roles")
    private List<Role> roles = null;
    @JsonProperty("RAX-AUTH:phonePin")
    private String rAXAUTHPhonePin;
    @JsonProperty("name")
    private String name;
    @JsonProperty("id")
    private String id;
    @JsonProperty("RAX-AUTH:domainId")
    private String rAXAUTHDomainId;
    @JsonProperty("RAX-AUTH:phonePinState")
    private String rAXAUTHPhonePinState;
  }

  @Data
  public static class Role {

    @JsonProperty("name")
    private String name;
    @JsonProperty("tenantId")
    private String tenantId;
    @JsonProperty("description")
    private String description;
    @JsonProperty("id")
    private String id;
    @JsonProperty("serviceId")
    private String serviceId;
  }
}

