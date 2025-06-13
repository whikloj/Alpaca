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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.camel.Exchange.CONTENT_TYPE;
import static org.apache.camel.util.ObjectHelper.loadResourceAsStream;
import static org.slf4j.LoggerFactory.getLogger;

import ca.islandora.alpaca.config.AlpacaConfig;
import ca.islandora.alpaca.config.HttpConfigurerContext;
import org.apache.camel.CamelContext;
import org.apache.camel.Configuration;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.test.junit5.TestSupport;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.TestPropertySource;

/**
 * Test overriding one of the default options.
 * @author whikloj
 * @since 2.2.0
 */
@CamelSpringBootTest
@SpringBootTest(classes = {
        DerivativeConnectorTest.TestConfig.class
})
@TestPropertySource(properties = {
        "derivative.systems-installed=testRoutesWithOverride",
        "derivative.testRoutesWithOverride.enabled=true",
        "derivative.testRoutesWithOverride.in-stream=topic:input",
        "derivative.testRoutesWithOverride.service-url=http://example.org/derivative/other",
        "alpaca.maxRedeliveries=0",
        "alpaca.additional-http-options=specialProp=true,disableStreamCache=false"
})
public class OverrideHttpOptionsTest  {

    private static final Logger LOGGER = getLogger(OverrideHttpOptionsTest.class);

    @Autowired
    CamelContext context;

    @Configuration
    @EnableConfigurationProperties({DerivativeOptions.class, AlpacaConfig.class, HttpConfigurerContext.class})
    static class TestConfig {

        @Bean
        public CamelContext camelContext() {
            return new DefaultCamelContext();
        }

    }

    @Test
    public void testDerivativeConnectorWithOverride() throws Exception {
        final String route = "IslandoraConnectorDerivative-testRoutesWithOverride";

        AdviceWith.adviceWith(context, route, a -> {
            a.replaceFromWith("direct:start");

            // Rig Drupal REST endpoint to return canned jsonld
            a.interceptSendToEndpoint("http://example.org/derivative/other?specialProp=true&" +
                            "disableStreamCache=false&connectionClose=true")
                    .skipSendToOriginalEndpoint()
                    .process(exchange -> {
                        exchange.getIn().removeHeaders("*", "Authorization");
                        exchange.getIn().setHeader("Content-Type", "image/jpeg");
                        exchange.getIn().setBody("SOME DERIVATIVE", String.class);
                    });

            a.mockEndpointsAndSkip("http://localhost:8000/node/2/media/image/3?specialProp=true&" +
                    "disableStreamCache=false&connectionClose=true");
        });
        context.start();

        final ProducerTemplate template = context.createProducerTemplate();

        final MockEndpoint endpoint = TestSupport.getMockEndpoint(context,
                "mock:http:localhost:8000/node/2/media/image/3",
                true);

        endpoint.expectedMessageCount(1);
        endpoint.expectedHeaderReceived(Exchange.HTTP_METHOD, "PUT");
        endpoint.expectedHeaderReceived(CONTENT_TYPE, "image/jpeg");
        endpoint.expectedHeaderReceived("Content-Location", "public://2018-08/2-Service File.jpg");
        endpoint.expectedHeaderReceived("Authorization", "Bearer islandora");

        template.send("direct:start", exchange -> {
            exchange.getIn().setBody(
                    IOUtils.toString(loadResourceAsStream("derivative/AS2Event.jsonld"), UTF_8)
            );
            exchange.getIn().setHeader("Authorization", "Bearer islandora");
        });

        endpoint.assertIsSatisfied();
    }
}
