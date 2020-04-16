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

package com.rackspace.salus.common.config;

import java.util.Map;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("salus.common.roles")
@Data
public class RoleProperties {
  /**
   * A map of authentication roles to json views.
   * Both key and value must be UPPERCASE.
   *
   * The roles (with "ROLE_" prefix) correspond to the roles in the spring security context.
   * Identity roles are translated to this format via {@link com.rackspace.salus.common.web.PreAuthenticatedFilter}.
   *
   * The views correspond to {@link com.rackspace.salus.common.web.View.ViewName}
   *
   * An anonymous role is used for unauthenticated requests.
   * i.e. internal service-to-service requests.
   */
  Map<String, String> roleToView = Map.of("ROLE_IDENTITY_ADMIN", "ADMIN");
}
