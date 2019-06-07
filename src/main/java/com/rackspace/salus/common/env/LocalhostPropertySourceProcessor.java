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

package com.rackspace.salus.common.env;

import java.net.InetAddress;
import java.net.UnknownHostException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;

/**
 * Auto-configures a property source for identifying the local node's hostname and IP address.
 * <p>
 * The following properties are provided:
 * <ul>
 *   <li><code>localhost.name</code> : the hostname of the local node</li>
 *   <li><code>localhost.address</code> : the IP address of the local node</li>
 * </ul>
 * </p>
 */
public class LocalhostPropertySourceProcessor implements EnvironmentPostProcessor {

  @Override
  public void postProcessEnvironment(ConfigurableEnvironment environment,
                                     SpringApplication application) {

    final InetAddress localHost;
    try {
      localHost = InetAddress.getLocalHost();
    } catch (UnknownHostException e) {
      throw new IllegalStateException("Unable to locate local host information", e);
    }

    environment.getPropertySources().addLast(
        new PropertySource<String>("localhost") {
          @Override
          public Object getProperty(String s) {
            switch (s) {
              case "localhost.name":
                return localHost.getHostName();
              case "localhost.address":
                return localHost.getHostAddress();
            }
            return null;
          }
        }
    );
  }
}
