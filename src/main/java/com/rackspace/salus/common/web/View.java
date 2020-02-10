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

import java.util.HashMap;
import java.util.Map;

public class View {
  public interface Public {}
  public interface Internal extends Public {}
  public interface Admin extends Internal {}

  public enum ViewName {
    PUBLIC,
    INTERNAL,
    ADMIN
  }

  private static final Map<ViewName, Class> viewNameToClass = new HashMap<>();
  static {
    viewNameToClass.put(ViewName.PUBLIC, Public.class);
    viewNameToClass.put(ViewName.INTERNAL, Internal.class);
    viewNameToClass.put(ViewName.ADMIN, Admin.class);
  }

  public static Class getView(String view) {
    try {
      ViewName v = ViewName.valueOf(view);
      return viewNameToClass.get(v);
    } catch (IllegalArgumentException | NullPointerException e) {
      return null;
    }
  }
}