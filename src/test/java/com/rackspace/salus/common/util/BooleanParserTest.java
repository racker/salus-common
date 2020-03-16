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

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import com.rackspace.salus.common.util.BooleanParser.BooleanFormatException;
import org.junit.Test;

public class BooleanParserTest {
  @Test
  public void parseTrue() throws BooleanFormatException {
    assertThat(BooleanParser.parseBoolean("True"), equalTo(true));
  }

  @Test
  public void parseFalse() throws BooleanFormatException {
    assertThat(BooleanParser.parseBoolean("FaLse"), equalTo(false));
  }

  @Test(expected = BooleanFormatException.class)
  public void parseFails() throws BooleanFormatException {
    BooleanParser.parseBoolean("not a boolean value");
  }

}
