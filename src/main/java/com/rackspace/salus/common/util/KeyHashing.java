/*
 *    Copyright 2018 Rackspace US, Inc.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 *
 */

package com.rackspace.salus.common.util;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

@Component
public class KeyHashing {

    private final String DELIMITER = ":";

    public HashFunction hashFunction() {
        return Hashing.sha256();
    }

    /**
     * Performs a hashing function on the provided string and returns the output.
     *
     * @param values The string(s) to be hashed.  Colon separated if multiple are provided.
     * @return The hashed result.
     */
    public String hash(String... values) {
        String value = String.join(DELIMITER, values);
        return hashFunction().hashString(value, StandardCharsets.UTF_8).toString();
    }

    /**
     * @return the bit size of the current hash algorithm
     */
    public int bits() {
        return hashFunction().bits();
    }
}
