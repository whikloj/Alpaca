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
package ca.islandora.alpaca.fcrepo;

import ca.islandora.alpaca.config.AlpacaConfig;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

/**
 * Component to conditionally start the Fcrepo Indexer.
 * @author whikloj
 */
@Component
@EnableConfigurationProperties({FcrepoIndexerOptions.class, AlpacaConfig.class})
public class FcrepoIndexerComponent {
    /**
     * Creates a Camel route for indexing into Fedora.
     * @return the bean for the FcrepoIndexer route
     */
    @Bean
    @ConditionalOnProperty(name = "fcrepo.indexer.enabled", havingValue = "true", matchIfMissing = false)
    public RouteBuilder fcrepoIndexer(final FcrepoIndexerOptions options, final AlpacaConfig alpacaConfig) {
        return new FcrepoIndexer(options, alpacaConfig);
    }
}
