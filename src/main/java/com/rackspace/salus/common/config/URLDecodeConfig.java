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

package com.rackspace.salus.common.config;

import com.rackspace.salus.common.web.URLDecodeFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class URLDecodeConfig {

  @Bean
  public FilterRegistrationBean<URLDecodeFilter> filterRegistrationBean() {
    FilterRegistrationBean < URLDecodeFilter > URLDecodeRegistrationBean = new FilterRegistrationBean();
    URLDecodeFilter URLDecodeFilter = new URLDecodeFilter();

    URLDecodeRegistrationBean.setFilter(URLDecodeFilter);
    URLDecodeRegistrationBean.setName("URLDecodeFilter");
    return URLDecodeRegistrationBean;
  }
}