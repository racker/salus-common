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
 *
 */

package com.rackspace.salus.common.web;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import org.springframework.web.util.WebUtils;

/**
 * A filter to make sure that our inputs are properly URLdecode'ed
 *
 * To apply this to a project add @EnableURLDecode to the main function that starts spring boot
 * and this Filter will be applied to every URI registered with that project.
 */
public class URLDecodeFilter implements Filter {

  @Override
  public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse,
      FilterChain filterChain) throws IOException, ServletException {

    HttpServletRequest req = (HttpServletRequest) servletRequest;
    req.setAttribute(
        WebUtils.INCLUDE_SERVLET_PATH_ATTRIBUTE, URLDecoder.decode(req.getRequestURI(),
            StandardCharsets.UTF_8));

    filterChain.doFilter(req, servletResponse);
  }
}