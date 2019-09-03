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
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.ServletWebRequest;

/**
 * This handler can be used as a base class for
 * {@link org.springframework.web.bind.annotation.ControllerAdvice} with
 * {@link org.springframework.web.bind.annotation.ExceptionHandler}'s to generalize the
 * formatting of a JSON error resposne body that aligns with the standard Spring Boot
 * error controller.
 * <p>
 *   The following shows an example of implementing controller advice with this base class:
 * </p>
 * <pre>
 &#64;ControllerAdvice(basePackages = "com.rackspace.salus.monitor_management.web")
 &#64;ResponseBody
 public class RestExceptionHandler extends
   com.rackspace.salus.common.web.AbstractRestExceptionHandler {

   &#64;Autowired
   public RestExceptionHandler(ErrorAttributes errorAttributes) {
     super(errorAttributes);
   }

   &#64;ExceptionHandler({IllegalArgumentException.class})
   public ResponseEntity<?> handleBadRequest(HttpServletRequest request) {
     return respondWith(request, HttpStatus.BAD_REQUEST);
   }
   ...

  * </pre>
 */
public abstract class AbstractRestExceptionHandler {

  private final ErrorAttributes errorAttributes;

  public AbstractRestExceptionHandler(
      ErrorAttributes errorAttributes) {
    this.errorAttributes = errorAttributes;
  }

  protected ResponseEntity<?> respondWith(HttpServletRequest request,
                                          HttpStatus status) {
    return respondWith(request, status, null);
  }

  /**
   * Leverages standard Spring Boot error attributes extraction, but allows for overriding the
   * derived fields.
   * @param request the request being handled, which obtained as a parameter of the {@link org.springframework.web.bind.annotation.ExceptionHandler}
   * @param status the {@link HttpStatus} to report instead of the discovered value
   * @param message if not null, replaces the auto-derived message field
   * @return a {@link ResponseEntity} with a Spring Boot-standard error response body
   */
  protected ResponseEntity<?> respondWith(
      HttpServletRequest request,
      HttpStatus status, @Nullable String message) {
    Map<String, Object> body = getErrorAttributes(request);
    body.put("status", status.value());
    body.put("error", status.getReasonPhrase());
    if (message != null) {
      body.put("message", message);
    }
    return new ResponseEntity<>(body, status);
  }

  private Map<String, Object> getErrorAttributes(HttpServletRequest request) {
    final ServletWebRequest webRequest = new ServletWebRequest(request);
    return errorAttributes.getErrorAttributes(webRequest, false);
  }
}
