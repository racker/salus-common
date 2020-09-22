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

import com.fasterxml.jackson.databind.JsonMappingException;
import com.rackspace.salus.common.errors.ResponseMessages;
import com.rackspace.salus.common.errors.RuntimeKafkaException;
import java.util.Map;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.context.request.ServletWebRequest;

/**
 * This handler can be used as a base class for
 * {@link org.springframework.web.bind.annotation.ControllerAdvice} with
 * {@link org.springframework.web.bind.annotation.ExceptionHandler}'s to generalize the
 * formatting of a JSON error response body that aligns with the standard Spring Boot
 * error controller.
 * <p>
 *   Handlers are provided for common cases, such as handling {@link IllegalArgumentException}
 *   and {@link IllegalArgumentException}.
 * </p>
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

   &#64;ExceptionHandler({CustomAppException.class})
   public ResponseEntity<?> handleCustomAppException(HttpServletRequest request, Exception e) {
     logRequestFailure(request, e);
     return respondWith(request, HttpStatus.BAD_REQUEST);
   }
   ...

  * </pre>
 */
@Slf4j
public abstract class AbstractRestExceptionHandler {

  private static final String SLEUTH_BRAVE_TRACE_ID_HEADER = "x-b3-traceid";

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
    // extract Spring Cloud Sleuth (aka Brave)'s traceId from incoming request headers to avoid
    // pulling dependency into common module
    final String traceId = request.getHeader(SLEUTH_BRAVE_TRACE_ID_HEADER);
    if (traceId != null) {
      body.put("traceId", traceId);
    }
    body.put("status", status.value());
    body.put("error", status.getReasonPhrase());
    body.remove("errors");
    if (message != null) {
      body.put("message", message);
    }
    return new ResponseEntity<>(body, status);
  }

  /**
   * Provides a uniform logging strategy for exception handlers.
   * @param request the request that was being handled
   * @param e the {@link Exception} that was thrown while handling the request
   */
  protected void logRequestFailure(HttpServletRequest request, Exception e) {
    log.warn("Web request for uri={} failed", request.getRequestURI(), e);
  }

  private Map<String, Object> getErrorAttributes(HttpServletRequest request) {
    final ServletWebRequest webRequest = new ServletWebRequest(request);
    return errorAttributes.getErrorAttributes(webRequest, false);
  }

  @ExceptionHandler({IllegalArgumentException.class})
  public ResponseEntity<?> handleBadRequest(HttpServletRequest request, Exception e) {

    logRequestFailure(request, e);
    return respondWith(request, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler({RemoteServiceCallException.class})
  public ResponseEntity<?> handleRemoteServiceCallException(
      HttpServletRequest request, RemoteServiceCallException e) {

    logRequestFailure(request, e);
    return respondWith(request, e.getStatusCode(), e.getMessage());
  }

  @ExceptionHandler({IllegalStateException.class})
  public ResponseEntity<?> handleBadState(HttpServletRequest request, Exception e) {

    logRequestFailure(request, e);
    return respondWith(request, HttpStatus.INTERNAL_SERVER_ERROR);
  }

  @ExceptionHandler({RestClientException.class})
  public ResponseEntity<?> handleRestClientException(HttpServletRequest request, RestClientException e) {
    logRequestFailure(request, e);

    if (e instanceof HttpStatusCodeException) {
      return respondWith(request, ((HttpStatusCodeException) e).getStatusCode(), "Remote REST exchange failed");
    }
    else {
      return respondWith(request, HttpStatus.BAD_GATEWAY, e.getMessage());
    }
  }

  @ExceptionHandler({HttpMessageNotReadableException.class})
  public ResponseEntity<?> handleHttpMessageNotReadable(HttpServletRequest request,
                                                        HttpMessageNotReadableException e) {
    logRequestFailure(request, e);

    if (e.getCause() instanceof JsonMappingException) {
      final JsonMappingException jsonMappingException = (JsonMappingException) e.getCause();

      return respondWith(request, HttpStatus.BAD_REQUEST,
          String.format("Failed to parse JSON: %s", jsonMappingException.getMessage()));
    }
    else {
      // fallback to default message derivation
      return respondWith(request, HttpStatus.BAD_REQUEST);
    }
  }

  @ExceptionHandler({RuntimeKafkaException.class})
  public ResponseEntity<?> handleKafkaExceptions(HttpServletRequest request, Exception e) {
    logRequestFailure(request, e);
    return respondWith(request, HttpStatus.SERVICE_UNAVAILABLE, ResponseMessages.kafkaExceptionMessage);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<?> handleMethodArgumentNotValidException(HttpServletRequest request, MethodArgumentNotValidException e) {
    logRequestFailure(request, e);
    return respondWith(request, HttpStatus.BAD_REQUEST, e.getMessage());
  }

}
