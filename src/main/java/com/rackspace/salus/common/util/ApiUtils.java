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

package com.rackspace.salus.common.util;

import java.util.List;
import org.springframework.cloud.gateway.mvc.ProxyExchange;
import org.springframework.http.HttpHeaders;

public class ApiUtils {

  static final List<String> requiredHeaders = List.of(
      "X-Tenant-Id",
      "X-Roles",
      "X-Impersonation-Roles"
  );

  public static void applyRequiredHeaders(ProxyExchange proxyExchange, HttpHeaders headers) {
    HttpHeaders newHeaders = new HttpHeaders();
    for (String header : requiredHeaders) {
      List<String> value = headers.get(header);
      if (value != null) {
        newHeaders.put(header, value);
      }
    }
    proxyExchange.headers(newHeaders);
  }

}
