/*
 * Copyright 2019 Rackspace US, Inc.
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

import com.rackspace.salus.common.config.IdentityConfig;
import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class IdentityHeaderFilter extends PreAuthenticatedFilter {

    public IdentityHeaderFilter(boolean requireTenantId) {
        super(IdentityConfig.HEADER_TENANT, Arrays.asList(IdentityConfig.HEADER_X_ROLES, IdentityConfig.HEADER_X_IMPERSONATOR_ROLES), requireTenantId);
    }
}
