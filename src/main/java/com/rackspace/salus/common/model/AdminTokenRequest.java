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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;
import lombok.experimental.Accessors;

@Data
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AdminTokenRequest {

  @JsonProperty("auth")
  private Auth auth;

  @Data
  @Accessors(chain = true)
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class Auth {

    @JsonProperty("passwordCredentials")
    private PasswordCredentials passwordCredentials;

    @JsonProperty("RAX-KSKEY:apiKeyCredentials")
    private APIKeyCredentials apiKeyCredentials;
  }

  @Data
  @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class PasswordCredentials {

    @NonNull
    @JsonProperty("username")
    private String username;

    @JsonProperty("password")
    private String password;
  }

  @Data
  @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class APIKeyCredentials {

    @NonNull
    @JsonProperty("username")
    private String username;

    @JsonProperty("apiKey")
    private String apiKey;
  }

}
