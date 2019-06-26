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

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rackspace.salus.common.util.ConfigMetadata.Property;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.util.ClassUtils;

/**
 * This utility class activates when the command-line argument <code>--dump-config</code>
 * is present. When present, it will process the
 * <a href="https://docs.spring.io/spring-boot/docs/current/reference/html/configuration-metadata.html">Spring Boot Configuration Metadata</a>
 * to locate all of the known configuration properties and output a markdown-style table containing:
 * <ul>
 *   <li>Property name, as used in properties and yaml files</li>
 *   <li>Data type</li>
 *   <li>Default value</li>
 *   <li>Description</li>
 *   <li>Environment variable name</li>
 * </ul>
 * <p>
 *   The properties can be filtered by passing one or more regular expressions, separated by commas,
 *   as <code>--dump-config=regex,regex,...</code>
 * </p>
 * <p>
 *   The {@link #process(String[])} method will call {@link System#exit(int)} if the command-line
 *   argument was found, so it the invocation can be placed first in the main method, such as
 * </p>
 * <pre>
 public static void main(String[] args) {
   DumpConfigProperties.process(args);
 * </pre>
 * <p>
 *   When activated, the application will exit immediately with an exit code of 0, normally, or
 *   1, when an error occurred during metadata processing.
 * </p>
 */
public class DumpConfigProperties {

  private DumpConfigProperties() {}

  /**
   * Instructs this utility to scan the main command-line arguments for the request
   * to dump configuration. If matched, the process will be exited.
   */
  public static void process(String[] args) {
    for (String arg : args) {
      if (arg.equals("--dump-config") || arg.startsWith("--dump-config=")) {
        final String[] parts = arg.split("=", 2);
        if (parts.length == 1) {
          dumpConfig(Collections.emptyList());
        } else {
          final String[] patterns = parts[1].split("\\s*,\\s*");
          dumpConfig(Arrays.asList(patterns));
        }
      }
    }
  }

  private static void dumpConfig(List<String> patternStrs) {
    final ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    final List<Pattern> patterns = patternStrs.stream()
        .map(Pattern::compile)
        .collect(Collectors.toList());

    final List<Property> properties = new ArrayList<>();

    try {
      final Enumeration<URL> resources = ClassUtils.getDefaultClassLoader()
          .getResources("META-INF/spring-configuration-metadata.json");

      while (resources.hasMoreElements()) {
        final URL resource = resources.nextElement();
        final ConfigMetadata configMetadata = objectMapper
            .readValue(resource, ConfigMetadata.class);

        if (patterns.isEmpty()) {
          properties.addAll(configMetadata.getProperties());
        }
        else {
          configMetadata.getProperties().stream()
              .filter(configMetadataProperty -> patterns.stream()
                  .anyMatch(pattern -> pattern.matcher(configMetadataProperty.getName()).lookingAt()))
              .forEach(properties::add);
        }
      }

      outputConfigProperties(properties);

      System.exit(0);

    } catch (IOException e) {
      System.err.println("Failed to load spring-configuration-metadata: "+e.getMessage());
      System.exit(1);
    }
  }

  private static void outputConfigProperties(List<Property> properties) {
    System.out.println("Name | Type | Default | Description | Environment Variable");
    System.out.println("-----|------|---------|-------------|---------------------");
    properties.stream()
        .sorted(Comparator.comparing(Property::getName))
        .forEach(p -> {
          System.out.printf("%s | %s | %s | %s | %s%n",
              p.getName(),
              simplifyType(p.getType()),
              nullToEmpty(p.getDefaultValue()),
              nullToEmpty(p.getDescription()),
              nameToEnvVar(p.getName())
              );
        });
  }

  /**
   * Simplified version of the conversion described in
   * https://github.com/spring-projects/spring-boot/wiki/Relaxed-Binding-2.0
   */
  private static String nameToEnvVar(String name) {
    return name.toUpperCase()
        // remove any special characters
        .replaceAll("[^A-Za-z0-9.]", "")
        // apply . -> _ binding conversion
        .replace('.', '_');
  }

  /**
   * Removes common package prefixes from the given Java type. Also turns generics like
   * <code>List&lt;String&gt;</code> into <code>List of String</code> to allow proper
   * rendering in markdown.
   */
  private static String simplifyType(String type) {
    if (type == null) {
      return null;
    }

    return type
        .replaceAll("(java\\.lang\\.|java\\.time\\.|java\\.util\\.)", "")
        .replaceAll("<(.*?)>", " of $1");
  }

  private static Object nullToEmpty(Object value) {
    return value != null ? value : "";
  }
}
