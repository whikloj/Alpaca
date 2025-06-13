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

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Abstract class of common properties
 *
 * @author whikloj
 */
@Component
@ConfigurationProperties(prefix = "alpaca")
public class AlpacaConfig {

  private static final Logger LOGGER = getLogger(AlpacaConfig.class);
  /**
   * The property name for the Alpaca configuration file.
   */
  public static final String ALPACA_CONFIG_PROPERTY = "alpaca.config";

  // static endpoint name for activemq connection
  public static final String JMS_ENDPOINT_NAME = "broker";

  // Property alpaca.additional-http-options is a list of additional HTTP options to be used in the endpoint.
  private List<String> additionalHttpOptions = List.of();
  // Property alpaca.config
  private String config = "";
  // Property alpaca.maxRedeliveries
  private int maxRedeliveries = 5;

  public String getConfig() {
    return config;
  }

  public void setConfig(final String config) {
    this.config = config;
  }

  /**
   * Set max redeliveries for error handling.
   * @param maxRedeliveries
   *   The number of maximum redeliveries.
   */
  public void setMaxRedeliveries(final int maxRedeliveries) {
    this.maxRedeliveries = maxRedeliveries;
  }

  /**
   * Get the max redeliveries for error handling.
   * @return the error.maxRedeliveries amount.
   */
  public int getMaxRedeliveries() {
    return maxRedeliveries;
  }

  /**
   * Post construct method to initialize additional HTTP options if using the http.additional-options property.
   */
  public List<String> getAdditionalHttpOptions() {
    return additionalHttpOptions;
  }

  /**
   * Set additional HTTP options.
   * @param options
   *   The list of additional HTTP options to be used in the endpoint.
   */
  public void setAdditionalHttpOptions(final List<String> options) {
    this.additionalHttpOptions = options;
  }

  /**
   * Utility function to append various JMS options like concurrentConsumer variables.
   * @param queueString
   *   The original topic/queue string
   * @param concurrentConsumers
   *   The number of concurrent consumers. -1 means no setting.
   * @param maxConcurrentConsumers
   *   The max number of concurrent consumers. -1 means no setting.
   * @param asyncConsumers
   *   Indicate if the queue should be processed strictly queue-wise (false;
   *   more for dealing with overhead?); otherwise, allow multiple items to be
   *   processed at the same time.
   * @return
   *   The modified topic/queue string.
   */
  public static String addJmsOptions(final String queueString, final int concurrentConsumers,
                              final int maxConcurrentConsumers, final boolean asyncConsumers) {
    final StringBuilder builder = new StringBuilder();
    if (concurrentConsumers > 0) {
      builder.append("concurrentConsumers=");
      builder.append(concurrentConsumers);
    }
    if (maxConcurrentConsumers > 0) {
      if (!builder.isEmpty()) {
        builder.append('&');
      }
      builder.append("maxConcurrentConsumers=");
      builder.append(maxConcurrentConsumers);
    }
    if (asyncConsumers) {
      if (!builder.isEmpty()) {
        builder.append('&');
      }
      builder.append("asyncConsumer=")
        .append(asyncConsumers);
    }
    if (!builder.isEmpty()) {
      LOGGER.trace("addJmsOptions returning builder {}", builder);
      return queueString + (queueString.contains("?") ? '&' : '?') + builder;
    }
    return queueString;
  }

  /**
   * Utility to add common endpoint options to HTTP endpoints.
   * @param httpEndpoint
   *   The http endpoint string.
   * @param forceAmpersand
   *   If you want to use this function with a dynamic endpoint, this is whether to force an ampersand to start.
   * @return
   *   The modified http endpoint string.
   */
  public String addHttpOptions(final String httpEndpoint, final boolean forceAmpersand) {
    // Filter any empty values.
    final List<String> elementSet =
            new ArrayList<>(getAdditionalHttpOptions().stream()
                    .filter(i -> !i.isEmpty()).map(String::trim).toList());
    if (elementSet.stream().noneMatch(t -> t.startsWith("connectionClose="))) {
      // If the user defined connectionClose=anything, we don't add this default, otherwise we do.
      elementSet.add("connectionClose=true");
    }
    if (elementSet.stream().noneMatch(t -> t.startsWith("disableStreamCache="))) {
      // If the user defined disableStreamCache=anything, we don't add this default, otherwise we do.
      elementSet.add("disableStreamCache=true");
    }
    final String commonElements = String.join("&", elementSet);
    final int bestGuessAtFinalLength = httpEndpoint.length() + commonElements.length() + 1;
    final StringBuilder builder = new StringBuilder(bestGuessAtFinalLength);
    builder.append(httpEndpoint);
    // Only append ? or & if there is an endpoint.
    if (!builder.isEmpty()) {
      if (httpEndpoint.contains("?") || forceAmpersand) {
        builder.append('&');
      } else {
        builder.append('?');
      }
    }
    // Append the common elements.
    builder.append(commonElements);
    LOGGER.debug("addHttpOptions returning {}", builder);
    return builder.toString();
  }

  /**
   * Assumes not to forceAmpersand
   * @param httpEndpoint
   *   The http endpoint string.
   * @return
   *   The modified http endpoint string.
   */
  public String addHttpOptions(final String httpEndpoint) {
    return addHttpOptions(httpEndpoint, false);
  }
}
