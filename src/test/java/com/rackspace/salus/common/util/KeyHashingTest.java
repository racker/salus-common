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

package com.rackspace.salus.common.util;

import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

public class KeyHashingTest {

    @Test
    public void singleValue() {
        KeyHashing hashing = new KeyHashing();
        String value = "thisisatest";
        String hashedValue = hashing.hash(value);
        assertThat(hashedValue, notNullValue());
    }

    @Test
    public void multipleValues() {
        String[] values = new String[3];
        values[0] = "one";
        values[1] = "two";
        values[2] = "three";

        String value = String.format("%s:%s:%s", values[0], values[1], values[2]);

        KeyHashing hashing = new KeyHashing();

        String hashedValues = hashing.hash(values);
        String hashedValue = hashing.hash(value);

        assertThat(hashedValue, notNullValue());
        assertThat(hashedValues, notNullValue());
        assertThat(hashedValues, equalTo(hashedValue));
    }
}