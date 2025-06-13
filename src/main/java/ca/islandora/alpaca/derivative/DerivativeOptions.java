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
package ca.islandora.alpaca.derivative;

import static ca.islandora.alpaca.config.AlpacaConfig.JMS_ENDPOINT_NAME;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.annotation.PostConstruct;

import org.apache.camel.CamelContext;

import org.slf4j.Logger;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.env.Environment;

import ca.islandora.alpaca.config.AlpacaConfig;
import org.springframework.stereotype.Component;

/**
 * Base derivative configuration class.
 * @author whikloj
 */
@Component
@ConfigurationProperties(prefix = "derivative")
public class DerivativeOptions {

  private static final Logger LOGGER = getLogger(DerivativeOptions.class);

  private static final String DERIVATIVE_PREFIX = "derivative";
  private static final String DERIVATIVE_ENABLED_PROPERTY = "enabled";
  private static final String DERIVATIVE_INPUT_PROPERTY = "in-stream";
  private static final String DERIVATIVE_OUTPUT_PROPERTY = "service-url";
  private static final String DERIVATIVE_CONCURRENT_PROPERTY = "concurrent-consumers";
  private static final String DERIVATIVE_MAX_CONCURRENT_PROPERTY = "max-concurrent-consumers";
  private static final String DERIVATIVE_ASYNC_CONSUMER = "async-consumer";

  private final Environment environment;

  private final CamelContext camelContext;

  private final AlpacaConfig alpacaConfig;
  /**
   * Comes from the derivative.systems-installed property
   */
  private Set<String> systemsInstalled = Collections.emptySet();

  /**
   * Constructor for DerivativeOptions.
   *
   * @param environment
   *   The Spring Environment to read properties from.
   * @param camelContext
   *   The CamelContext to add routes to.
   * @param alpacaConfig
   *   The AlpacaConfig to use for HTTP options.
   */
  public DerivativeOptions(final Environment environment, final CamelContext camelContext,
                           final AlpacaConfig alpacaConfig) {
    this.environment = environment;
    this.camelContext = camelContext;
    this.alpacaConfig = alpacaConfig;
  }

  /**
   * Set the systems installed.
   * @param systemsInstalled
   *   The set of systems installed
   */
  public void setSystemsInstalled(final List<String> systemsInstalled) {
    this.systemsInstalled = new HashSet<>(systemsInstalled);
  }

  /**
   * Get the derivative.systems-installed property
   */
  public Set<String> getSystemsInstalled() {
    return this.systemsInstalled;
  }

  /**
   * Register additional beans for derivative routes.
   *
   * @throws Exception
   *   When unable to add routes to the camel context.
   */
  @PostConstruct
  void processAllServices() throws Exception {
    if (getSystemsInstalled() != null && !getSystemsInstalled().isEmpty()) {
      for (final var system : getSystemsInstalled()) {
        final var enabled =
                environment.getProperty(enabledProperty(system), Boolean.class, false);
        if (enabled) {
          startDerivativeService(system);
        } else {
          LOGGER.debug("Derivative connector (" + system + ") is disabled, skipping");
        }
      }
    }
  }

  /**
   * Attempt to start the routes and add them to the camel context.
   *
   * @param serviceName
   *   The derivative service name.
   * @throws Exception
   *   When unable to add routes to the camel context.
   */
  private void startDerivativeService(final String serviceName) throws Exception {
      final var input = environment.getProperty(inputProperty(serviceName), "");
      final var output = environment.getProperty(outputProperty(serviceName), "");
      if (!input.isBlank() && !output.isBlank()) {
        final int concurrentConsumers = environment.getProperty(concurrentConsumerProperty(serviceName),
                Integer.class, -1);
        final int maxConcurrentConsumers = environment.getProperty(maxConcurrentConsumerProperty(serviceName),
                Integer.class, -1);
        final boolean asyncConsumer = environment.getProperty(asyncConsumerProperty(serviceName),
                Boolean.class, false);
        // Add concurrent/max-concurrent
        final String finalInput = AlpacaConfig.addJmsOptions(addBrokerName(input), concurrentConsumers,
                maxConcurrentConsumers, asyncConsumer);
        // Add connectionClose and other http options.
        final String finalOutput = alpacaConfig.addHttpOptions(output);
        camelContext.addRoutes(new DerivativeConnector(serviceName, finalInput, finalOutput, alpacaConfig));
      } else {
        final StringBuilder message = new StringBuilder();
        if (input.isBlank()) {
          message.append(inputProperty(serviceName)).append(" is blank");
        }
        if (output.isBlank()) {
          if (!message.isEmpty()) {
            message.append(" and ");
          }
          message.append(outputProperty(serviceName)).append(" is blank");
        }
        message.append(", skipping");
        LOGGER.debug(message.toString());
      }

  }

  /**
   * Just adds the JMS broker name to the provided queue/topic.
   * @param queueName the provided queue/topic.
   * @return the full endpoint including the broker name.
   */
  private String addBrokerName(final String queueName) {
    return JMS_ENDPOINT_NAME + ":" + queueName;
  }

  /**
   * Return the expected enabled property
   * @param systemName the derivative system name
   * @return the property
   */
  private String enabledProperty(final String systemName) {
    return DERIVATIVE_PREFIX + "." + systemName + "." + DERIVATIVE_ENABLED_PROPERTY;
  }

  /**
   * Return the expected input topic/queue property
   * @param systemName the derivative system name
   * @return the property
   */
  private String inputProperty(final String systemName) {
    return DERIVATIVE_PREFIX + "." + systemName + "." + DERIVATIVE_INPUT_PROPERTY;
  }

  /**
   * Return the expected output service url property
   * @param systemName the derivative system name
   * @return the property
   */
  private String outputProperty(final String systemName) {
    return DERIVATIVE_PREFIX + "." + systemName + "." + DERIVATIVE_OUTPUT_PROPERTY;
  }

  /**
   * Return the expected concurrent consumers property.
   * @param systemName the derivative system name
   * @return the property
   */
  private String concurrentConsumerProperty(final String systemName) {
    return DERIVATIVE_PREFIX + "." + systemName + "." + DERIVATIVE_CONCURRENT_PROPERTY;
  }

  /**
   * Return the expected max-concurrent consumers property.
   * @param systemName the derivative system name
   * @return the property
   */
  private String maxConcurrentConsumerProperty(final String systemName) {
    return DERIVATIVE_PREFIX + "." + systemName + "." + DERIVATIVE_MAX_CONCURRENT_PROPERTY;
  }

  /**
   * Return the expected async-consumer property.
   * @param systemName the derivative system name
   * @return the property
   */
  private String asyncConsumerProperty(final String systemName) {
      return DERIVATIVE_PREFIX + "." + systemName + "." + DERIVATIVE_ASYNC_CONSUMER;
  }

}
