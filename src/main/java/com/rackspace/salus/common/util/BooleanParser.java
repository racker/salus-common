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

package com.rackspace.salus.common.util;

import com.rackspace.salus.common.errors.BooleanFormatException;

public class BooleanParser {

  public static boolean parseBoolean(String boolValue) throws BooleanFormatException {
    if("true".compareToIgnoreCase(boolValue) == 0) {
      return true;
    }else if ("false".compareToIgnoreCase(boolValue) == 0) {
      return false;
    }
    throw new BooleanFormatException(String.format("%s is not a valid boolean value", boolValue));
  }


}
