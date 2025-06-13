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

import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSException;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.pool.PooledConnectionFactory;
import org.apache.camel.component.activemq6.ActiveMQComponent;
import org.apache.camel.component.jms.JmsConfiguration;
import org.slf4j.Logger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import static ca.islandora.alpaca.config.AlpacaConfig.JMS_ENDPOINT_NAME;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * ActiveMQ component creator
 *
 * @author whikloj
 */
@Component
@EnableConfigurationProperties(ActivemqConfig.class)
public class ActivemqComponent {
    private static final Logger LOGGER = getLogger(ActivemqComponent.class);
    /**
     * Create a JMS connection factory bean.
     * @return the connection factory bean
     * @throws JMSException on failure to create new connection.
     */
    @Bean
    @ConditionalOnProperty(name = "jms.brokerUrl")
    public ConnectionFactory jmsConnectionFactory(final ActivemqConfig config) throws JMSException {
        if (config.getBrokerUrl().isBlank()) {
            throw new JMSException("JMS broker URL is not set. Please check your configuration.");
        }
        LOGGER.debug("jmsConnectionFactory: brokerUrl is {}", config.getBrokerUrl());
        final ActiveMQConnectionFactory factory;
        if (!config.getUsername().isBlank() && !config.getPassword().isBlank()) {
            LOGGER.debug("Creating ActiveMQConnectionFactory with username {} and password of length {}",
                    config.getUsername(), config.getPassword().length());
            factory = new ActiveMQConnectionFactory(config.getUsername(), config.getPassword(), config.getBrokerUrl());
        } else {
            LOGGER.debug("Creating ActiveMQConnectionFactory without username and password");
            factory = new ActiveMQConnectionFactory(config.getBrokerUrl());
        }
        return factory;
    }

    /**
     * Get a pooled connection factory
     * @param connectionFactory the JMS connection factory.
     * @return A pooled connection factory bean.
     */
    @Bean
    @ConditionalOnProperty(name = "jms.brokerUrl")
    @DependsOn("jmsConnectionFactory")
    public PooledConnectionFactory pooledConnectionFactory(final ConnectionFactory connectionFactory,
                                                           final ActivemqConfig config) {
        final var pooledConnectionFactory = new PooledConnectionFactory();
        pooledConnectionFactory.setMaxConnections(config.getConnections());
        pooledConnectionFactory.setConnectionFactory(connectionFactory);
        return pooledConnectionFactory;
    }

    /**
     * Create a JMS configuration bean.
     * @param connectionFactory the pooled connection factory.
     * @return the JMS configuration
     */
    @Bean
    @ConditionalOnProperty(name = "jms.brokerUrl")
    @DependsOn("pooledConnectionFactory")
    public JmsConfiguration jmsConfiguration(final PooledConnectionFactory connectionFactory) {
        final var configuration = new JmsConfiguration();
        configuration.setConnectionFactory(connectionFactory);
        return configuration;
    }

    /**
     * Create an ActiveMQ endpoint bean.
     * @param jmsConfiguration the JMS configuration
     * @return the ActiveMQ endpoint.
     */
    @Bean(JMS_ENDPOINT_NAME)
    @ConditionalOnProperty(name = "jms.brokerUrl")
    @DependsOn("jmsConfiguration")
    public ActiveMQComponent activeMQComponent(final JmsConfiguration jmsConfiguration) {
        final var component = new ActiveMQComponent();
        component.setConfiguration(jmsConfiguration);
        return component;
    }
}
