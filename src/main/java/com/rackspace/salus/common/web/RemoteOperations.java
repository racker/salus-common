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

import java.util.function.Supplier;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Provides utilities for remote service call operations.
 */
public class RemoteOperations {

  /**
   * Wraps a use of {@link RestTemplate} with standardized exception handling of
   * {@link IllegalArgumentException} and {@link RestClientException}.
   * <p>
   *   The following shows an example of wrapping a typical use of RestTemplate:
   *   <pre>
   return mapRestClientExceptions(
     "policy-management",
     () ->
       restTemplate.exchange(
         uri,
         HttpMethod.GET,
         null,
         MAP_OF_MONITOR_POLICY
       ).getBody()
   );
   *   </pre>
   * </p>
   * @param remoteServiceName the name of the remote service being called
   * @param wrapped the code making use of {@link RestTemplate}
   * @param <R> the return type of the wrapped supplier
   * @return the value returned by the wrapped supplier
   */
  public static <R> R mapRestClientExceptions(String remoteServiceName, Supplier<R> wrapped) {
    try {

      return wrapped.get();

    } catch (IllegalArgumentException e) {
      throw new IllegalStateException(
          String.format("REST exchange with %s was malformed", remoteServiceName), e);
    } catch (RestClientException e) {
      throw new RemoteServiceCallException(remoteServiceName, e);
    }
  }

}
