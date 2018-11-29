/*
 *    Copyright 2018 Rackspace US, Inc.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 *
 */

package com.rackspace.salus.common.web;

import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.GenericFilterBean;

/**
 * This Spring Security filter integrates with Repose by consuming the headers populated by the
 * <a href="https://repose.atlassian.net/wiki/x/CAALAg">Repose Keystone v2 filter</a> and translates
 * that into an authenticated {@link ReposeHeaderToken}.
 */
public class ReposeHeaderFilter extends GenericFilterBean {
    public static final String ATTR_REPOSE_HEADERS =
        ReposeHeaderFilter.class.getName() + ".REPOSE_HEADERS";
    public static final String HEADER_X_ROLES = "X-Roles";
    public static final String HEADER_TENANT = "X-Tenant-Id";

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain) throws IOException, ServletException {
        if (servletRequest instanceof HttpServletRequest) {
            final HttpServletRequest req = (HttpServletRequest) servletRequest;

            final String value = req.getHeader(HEADER_X_ROLES);
            final String tenant = req.getHeader(HEADER_TENANT);

            if ((StringUtils.hasText(value)) && (StringUtils.hasText(tenant))) {
                final String[] convertedNames = value.trim().replace(':', '_')
                        .replace('-','_').toUpperCase().split(",");

                final List<SimpleGrantedAuthority> roles = Arrays.stream(convertedNames)
                        .map(a -> new SimpleGrantedAuthority("ROLE_" + a))
                        .collect(toList());

                final ReposeHeaderToken auth = new ReposeHeaderToken(tenant, roles);

                req.setAttribute(ATTR_REPOSE_HEADERS, true);
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }

        chain.doFilter(servletRequest, servletResponse);
    }
}
