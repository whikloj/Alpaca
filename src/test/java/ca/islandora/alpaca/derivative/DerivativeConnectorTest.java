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

import ca.islandora.alpaca.config.AlpacaConfig;
import org.apache.camel.CamelContext;
import org.apache.camel.Configuration;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.test.junit5.TestSupport;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.Exchange;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.camel.Exchange.CONTENT_TYPE;
import static org.apache.camel.language.constant.ConstantLanguage.constant;
import static org.apache.camel.util.ObjectHelper.loadResourceAsStream;

/**
 * Test class for the Derivative Connector.
 * This class tests the configuration and functionality of the Derivative Connector.
 * @author whikloj
 */
@CamelSpringBootTest
@SpringBootTest(classes = {
        DerivativeConnectorTest.TestConfig.class
})
@TestPropertySource(properties = {
        "derivative.systems-installed=testRoutes",
        "derivative.testRoutes.enabled=true",
        "derivative.testRoutes.in-stream=topic:input",
        "derivative.testRoutes.service-url=http://example.org/derivative/convert",
        "alpaca.maxRedeliveries=0",
})
public class DerivativeConnectorTest {

    @Autowired
    CamelContext camelContext;

    @Configuration
    @ComponentScan(basePackages = {
            "ca.islandora.alpaca"
    })
    @EnableConfigurationProperties({DerivativeOptions.class, AlpacaConfig.class})
    static class TestConfig {

        @Bean
        public CamelContext camelContext() {
            return new DefaultCamelContext();
        }

    }

    @Test
    public void testDerivativeConnector() throws Exception {
        final String route = "IslandoraConnectorDerivative-testRoutes";
        camelContext.getRoutes();

        AdviceWith.adviceWith(camelContext, route, a -> {
            a.replaceFromWith("direct:start");
            a.interceptSendToEndpoint(
                    "http://example.org/derivative/convert?connectionClose=true&disableStreamCache=true"
                    )
                    .skipSendToOriginalEndpoint()
                    .setHeader(CONTENT_TYPE, constant("image/jpeg"))
                    .setBody(constant("SOME DERIVATIVE"));
            a.mockEndpointsAndSkip(
                    "http://localhost:8000/node/2/media/image/3?connectionClose=true&disableStreamCache=true"
            );
        });

        camelContext.start();
        final ProducerTemplate template = camelContext.createProducerTemplate();

        final MockEndpoint endpoint = TestSupport.getMockEndpoint(camelContext,
                "mock:http:localhost:8000/node/2/media/image/3",
                true);
        endpoint.expectedMessageCount(1);
        endpoint.expectedHeaderReceived(Exchange.HTTP_METHOD, "PUT");
        endpoint.expectedHeaderReceived(CONTENT_TYPE, "image/jpeg");
        endpoint.expectedHeaderReceived("Content-Location", "public://2018-08/2-Service File.jpg");
        endpoint.expectedHeaderReceived("Authorization", "Bearer islandora");

        template.send("direct:start", exchange -> {
            exchange.getIn().setBody(IOUtils.toString(loadResourceAsStream("derivative/AS2Event.jsonld"), UTF_8));
            exchange.getIn().setHeader("Authorization", "Bearer islandora");
        });

        endpoint.assertIsSatisfied();
    }
}
