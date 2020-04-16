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

import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;

/**
 * This Spring Security filter integrates with Repose by consuming the headers populated by the
 * <a href="https://repose.atlassian.net/wiki/x/CAALAg">Repose Keystone v2 filter</a> and translates
 * that into an authenticated {@link PreAuthenticatedToken}.
 */
@Slf4j
public class ReposeHeaderFilter extends PreAuthenticatedFilter {

    public static final String HEADER_X_ROLES = "X-Roles";
    public static final String HEADER_X_IMPERSONATOR_ROLES = "X-Impersonator-Roles";
    public static final String HEADER_TENANT = "Requested-Tenant-Id";

    public ReposeHeaderFilter(boolean requireTenantId) {
        super(HEADER_TENANT, Arrays.asList(HEADER_X_ROLES, HEADER_X_IMPERSONATOR_ROLES), requireTenantId);
    }
}
