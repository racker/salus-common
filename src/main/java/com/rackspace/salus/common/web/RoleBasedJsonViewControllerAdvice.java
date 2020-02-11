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

package com.rackspace.salus.common.web;

import com.rackspace.salus.common.config.RoleProperties;
import java.util.Collection;
import java.util.Comparator;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.commons.lang3.ClassUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJacksonValue;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.AbstractMappingJacksonResponseBodyAdvice;


/**
 * Uses the roles populated in the spring security context by {@link BackendServicesWebSecurityConfig}
 * to determine what json view should be used when serializing the api response object.
 */
@RestControllerAdvice
@Import(RoleProperties.class)
public class RoleBasedJsonViewControllerAdvice extends AbstractMappingJacksonResponseBodyAdvice {

  private RoleProperties roleProperties;

  @Autowired
  public RoleBasedJsonViewControllerAdvice(RoleProperties roleProperties) {
    super();
    this.roleProperties = roleProperties;

  }

  /**
   * Intercepts all outgoing api responses and sets jackson to use the correct json view
   * to serialize the response based on the roles provided in the requests header.
   */
  @SuppressWarnings({"NullableProblems", "rawtypes"})
  @Override
  protected void beforeBodyWriteInternal(MappingJacksonValue mappingJacksonValue,
      MediaType mediaType, MethodParameter methodParameter, ServerHttpRequest serverHttpRequest,
      ServerHttpResponse serverHttpResponse) {

    if (SecurityContextHolder.getContext().getAuthentication() != null) {
      Collection<? extends GrantedAuthority> authorities = SecurityContextHolder.getContext().getAuthentication().getAuthorities();
      Class jsonView = authorities.stream()
          .map(GrantedAuthority::getAuthority)
          // get the json view class for that enum
          .map(role -> roleProperties.getRoleToView().get(role))
          .map(View::getView)
          .filter(Objects::nonNull)
          // get the json view class which extends the most interfaces
          // i.e. the one with the greater access permissions
          .max(Comparator.comparing(view -> ClassUtils.getAllInterfaces(view).size()))
          .orElseThrow(() ->
              // this can only occur if an x-tenant-id header is provided and the given roles
              // are not valid.
              // this should not happen since one of those roles is required to pass repose's validation.
              new IllegalArgumentException(String.format("No authorized roles found %s",
                  authorities.stream()
                      .map(GrantedAuthority::getAuthority)
                      .collect(Collectors.joining(",")))));

      mappingJacksonValue.setSerializationView(jsonView);
    }
  }
}
