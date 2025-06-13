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
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;


/**
 * Test of connector with the additional http options added.
 * @author whikloj
 * @since 2.2.0
 */
@CamelSpringBootTest
@SpringBootTest(classes = {
        DerivativeConnectorTest.TestConfig.class
})
@TestPropertySource(properties = {
        "derivative.systems-installed=testRoutesWithMultipleOptions",
        "derivative.testRoutesWithMultipleOptions.enabled=true",
        "derivative.testRoutesWithMultipleOptions.in-stream=topic:input",
        "derivative.testRoutesWithMultipleOptions.service-url=http://example.org/derivative/other",
        "alpaca.maxRedeliveries=0",
        "alpaca.additional-http-options=specialProp=true,otherProp=91"
})
public class ConnectorWithHttpOptions2Test {

    private static final Logger LOGGER = getLogger(ConnectorWithHttpOptions2Test.class);

    @Autowired
    CamelContext context;

    @Configuration
    @ComponentScan(basePackages = {
            "ca.islandora.alpaca"
    })
    @EnableConfigurationProperties({AlpacaConfig.class, DerivativeOptions.class})
    static class TestConfig {

        @Bean
        public CamelContext camelContext() {
            return new DefaultCamelContext();
        }

    }

    @Test
    public void testDerivativeConnectorWithMultipleOptions() throws Exception {
        final String route = "IslandoraConnectorDerivative-testRoutesWithMultipleOptions";

        AdviceWith.adviceWith(context, route, a -> {
            a.replaceFromWith("direct:start");

            // Rig Drupal REST endpoint to return canned jsonld
            a.interceptSendToEndpoint("http://example.org/derivative/other?specialProp=true&otherProp=91" +
                            "&connectionClose=true&disableStreamCache=true")
                    .skipSendToOriginalEndpoint()
                    .process(exchange -> {
                        exchange.getIn().removeHeaders("*", "Authorization");
                        exchange.getIn().setHeader("Content-Type", "image/jpeg");
                        exchange.getIn().setBody("SOME DERIVATIVE", String.class);
                    });

            a.mockEndpoints("http://localhost:8000/node/2/media/image/3?specialProp=true&otherProp=91" +
                    "&connectionClose=true&disableStreamCache=true");
        });

        context.start();

        final MockEndpoint endpoint = TestSupport.getMockEndpoint(context,
                "mock:http:localhost:8000/node/2/media/image/3",
                true);
        final ProducerTemplate template = context.createProducerTemplate();

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
