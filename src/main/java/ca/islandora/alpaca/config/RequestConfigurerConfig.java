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
 * Class to enable the HTTP client configurer.
 *
 * @author whikloj
 */
@ConfigurationProperties(prefix = "request.configurer")
public class RequestConfigurerConfig {

    private static final Logger LOGGER = getLogger(RequestConfigurerConfig.class);

    private boolean enabled;
    private int requestTimeout;
    private int connectionTimeout;
    private int socketTimeout;

    /**
     * Set whether the http configurer is enabled or not.
     * @param enabled
     *   true if enabled, false otherwise
     */
    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Get whether the http configurer is enabled or not.
     * @return true if enabled, false otherwise
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Set the request timeout.
     * @param requestTimeout
     *   The request timeout in milliseconds.
     */
    public void setRequestTimeout(final int requestTimeout) {
        this.requestTimeout = requestTimeout;
    }

    /**
     * Get the request timeout.
     * @return the request timeout in milliseconds
     */
    public int getRequestTimeout() {
        return requestTimeout;
    }

    /**
     * Set the connection timeout.
     * @param connectionTimeout
     *   The connection timeout in milliseconds.
     */
    public void setConnectionTimeout(final int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    /**
     * Get the connect timeout.
     * @return the connect timeout in milliseconds
     */
    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    /**
     * Set the socket timeout.
     * @param socketTimeout
     *   The socket timeout in milliseconds.
     */
    public void setSocketTimeout(final int socketTimeout) {
        this.socketTimeout = socketTimeout;
    }

    /**
     * Get the socket timeout.
     * @return the socket timeout in milliseconds
     */
    public int getSocketTimeout() {
        return socketTimeout;
    }
}
