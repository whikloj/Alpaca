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

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.support.ResourcePropertySource;


/**
 * EnvironmentPostProcessor to load the Alpaca configuration file specified by the
 * system property {@code alpaca.config}.
 * This processor adds the Alpaca configuration as a property source at the highest precedence.
 *
 * @author whikloj
 */
public class PropertyPostProcessor implements EnvironmentPostProcessor, Ordered {

    @Override
    public void postProcessEnvironment(final ConfigurableEnvironment environment, final SpringApplication application) {
        final String configPath = System.getProperty(AlpacaConfig.ALPACA_CONFIG_PROPERTY);
        if (configPath != null) {
            try {
                final PropertySource<?> alpacaSource =
                        new ResourcePropertySource(new FileSystemResource(configPath));

                environment.getPropertySources().addFirst(alpacaSource);
            } catch (final Exception e) {
                throw new RuntimeException("Failed to load alpaca config: " + configPath, e);
            }
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
