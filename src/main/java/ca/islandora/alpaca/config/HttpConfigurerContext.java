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

import org.apache.camel.CamelContext;
import org.apache.camel.component.http.HttpComponent;
import org.apache.camel.spring.boot.CamelContextConfiguration;
import org.apache.hc.core5.util.Timeout;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Class to apply HTTP client configuration settings to Camel's HTTP component.
 * @author whikloj
 */
@Configuration
@EnableConfigurationProperties(RequestConfigurerConfig.class)
public class HttpConfigurerContext {

    @Bean
    public CamelContextConfiguration contextConfigurer(final RequestConfigurerConfig config) {
        return new CamelContextConfiguration() {
            @Override
            public void beforeApplicationStart(final CamelContext context) {
                if (config.isEnabled()) {
                    final var http = context.getComponent("http", HttpComponent.class);
                    http.setConnectionRequestTimeout(Timeout.ofMilliseconds(config.getRequestTimeout()));
                    http.setConnectTimeout(Timeout.ofMilliseconds(config.getConnectionTimeout()));
                    http.setSoTimeout(Timeout.ofMilliseconds(config.getSocketTimeout()));
                }
            }

            @Override
            public void afterApplicationStart(final CamelContext context) {
                // Optional, usually not needed unless you're setting something post-startup
            }
        };
    }
}
