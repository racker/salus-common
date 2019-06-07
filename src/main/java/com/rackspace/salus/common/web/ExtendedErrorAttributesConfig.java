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

import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.WebRequest;

/**
 * When <code>&#64;Import</code>ed, this will register an {@link ErrorAttributes} bean
 * that augments the standard response content with:
 * <ul>
 *   <li><code>app</code> : the value of the <code>spring.application.name</code> property</li>
 *   <li><code>host</code> : the value of the <code>localhost.name</code> property</li>
 * </ul>
 * <p>
 *   <em>NOTE:</em> this requires <code>spring.application.name</code> to be configured in
 *   <code>application.yml</code> (or similar), which is why this configuration bean needs
 *   to be opted in with an <code>&#64;Import</code>.
 * </p>
 * @see com.rackspace.salus.common.env.LocalhostPropertySourceProcessor
 */
@Configuration
@EnableConfigurationProperties({ServerProperties.class})
public class ExtendedErrorAttributesConfig {

  @Bean
  public ErrorAttributes errorAttributes(ServerProperties serverProperties,
                                         @Value("${spring.application.name}") String appName,
                                         @Value("${localhost.name}") String ourHost) {
    return new DefaultErrorAttributes(serverProperties.getError().isIncludeException()) {
      @Override
      public Map<String, Object> getErrorAttributes(WebRequest webRequest,
                                                    boolean includeStackTrace) {
        final Map<String, Object> errorAttributes = super
            .getErrorAttributes(webRequest, includeStackTrace);

        errorAttributes.put("app", appName);
        errorAttributes.put("host", ourHost);

        return errorAttributes;
      }
    };
  }
}
