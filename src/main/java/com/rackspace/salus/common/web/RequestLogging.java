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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

/**
 * An interceptor that can be registered by a {@link WebMvcConfigurer} config bean via
 * {@link WebMvcConfigurer#addInterceptors(InterceptorRegistry)}. It will log a summary of
 * a request and its response to allow for easily locating within logs along with any trace IDs.
 */
@Slf4j
public class RequestLogging extends HandlerInterceptorAdapter {

  @Override
  public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                              Object handler, Exception ex) throws Exception {
    log.info("Request completed: method={} path={}{} parameters={} status={}",
        request.getMethod(), request.getContextPath(), request.getServletPath(), request.getParameterMap(),
        response.getStatus());
  }

}
