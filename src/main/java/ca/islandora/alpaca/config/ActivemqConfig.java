/*
 * Licensed to Islandora Foundation under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * The Islandora Foundation licenses this file to you under the MIT License.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://opensource.org/licenses/MIT
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ca.islandora.alpaca.config;

import static org.slf4j.LoggerFactory.getLogger;

import org.slf4j.Logger;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * ActiveMQ configuration class
 *
 * @author whikloj
 */
@ConfigurationProperties(prefix = "jms")
public class ActivemqConfig {

  private static final Logger LOGGER = getLogger(ActivemqConfig.class);

  private String brokerUrl = "";
  private String username = "";
  private String password = "";
  private int connections;

  /**
   * Get the JMS broker URL.
   * @return the url
   */
  public String getBrokerUrl() {
    return brokerUrl;
  }

  /**
   * Set the JMS broker URL.
   * @param brokerUrl
   *   the url to set
   */
  public void setBrokerUrl(final String brokerUrl) {
    this.brokerUrl = brokerUrl;
  }

  /**
   * Get the JMS broker username
   * @return the username or an empty string.
   */
  public String getUsername() {
    return username;
  }

  /**
   * Set the JMS broker username.
   * @param username
   *   the username to set
   */
  public void setUsername(final String username) {
    this.username = username;
  }

  /**
   * Get the JMS broker password
   * @return the password or an empty string.
   */
  public String getPassword() {
    return password;
  }

  /**
   * Set the JMS broker password.
   * @param password
   *   the password to set
   */
  public void setPassword(final String password) {
    this.password = password;
  }

  /**
   * Get the size of the connection pool.
   * @return connection pool size
   */
  public int getConnections() {
    return connections;
  }

  /**
   * Set the size of the connection pool.
   * @param connections
   *   the number of connections to set
   */
  public void setConnections(final int connections) {
    this.connections = connections;
  }

}
