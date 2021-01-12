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

import brave.Tracer;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.mvc.method.annotation.ExceptionHandlerExceptionResolver;

@Slf4j
@Import({ExceptionHandlerExceptionResolver.class})
@Component
public class ExceptionHandlerFilter extends OncePerRequestFilter {

  private ObjectMapper objectMapper;

  public ExceptionHandlerFilter(ObjectMapper objectMapper)  {
    this.objectMapper = objectMapper;
  }

  private Tracer tracer;
  private static final String ATTRIBUTE_TRACE_ID = "traceId";
  private static final String ATTRIBUTE_APP = "app";
  private static final String ATTRIBUTE_HOST = "host";
  private static final String ATTRIBUTE_PATH = "path";

  @Autowired
  private ExceptionHandlerExceptionResolver resolver;

  @Override
  public void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {
    try {
      filterChain.doFilter(request, response);
    } catch (RuntimeException e) {
      log.error("Spring Security Filter Chain Exception:", e);
      resolver.resolveException(request, response, null, e);

      Map<String, Object> responseMap = new HashMap<>();

      responseMap.put("timestamp", LocalDateTime.now().toString());
      responseMap.put("status", 401);
      responseMap.put("error", e);
      responseMap.put("message", e.getLocalizedMessage());

      responseMap.put(ATTRIBUTE_PATH, request.getContextPath());
      responseMap.put(ATTRIBUTE_APP, "appName");
      responseMap.put(ATTRIBUTE_HOST, InetAddress.getLocalHost().getHostName());

//      final Span currentSpan = tracer.currentSpan();
//      if (currentSpan != null) {
//        responseMap.put(ATTRIBUTE_TRACE_ID, currentSpan.context().traceIdString());
//      }

      response.setContentType("application/json");
      response.setStatus(HttpStatus.UNAUTHORIZED.value());
      log.info("response set");
      response.getOutputStream().write(objectMapper.writeValueAsBytes(responseMap));
////      response.getWriter().write(objectMapper.writeValueAsString(responseMap));
//      return;
    }
  }
}
