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

package com.rackspace.salus.common.config;

public interface IdentityConfig {

  /**
   * Request Attribute name to store Identity headers Map
   */
  String ATTRIBUTE_NAME = "identityHeadersMap";

  String HEADER_X_ROLES = "X-Roles";
  String HEADER_X_IMPERSONATOR_ROLES = "X-Impersonator-Roles";
  String HEADER_TENANT = "Requested-Tenant-Id";

  // some requests don't come through with a `tenantHeader` to validate
  // but do have a list of tenants to log for audit purposes.
   String EXTRA_TENANT_HEADER = "X-Tenant-Id";

  String X_AUTH_HEADER = "x-auth-token";
}
