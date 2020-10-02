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

package com.rackspace.salus.common.config;

public interface MetricTags {
  String SERVICE_METRIC_TAG = "service";
  String OPERATION_METRIC_TAG = "operation";
  String OBJECT_TYPE_METRIC_TAG = "objectType";
  String EXCEPTION_METRIC_TAG = "exception";
  String URI_METRIC_TAG = "uri";
  String REASON = "reason";
}
