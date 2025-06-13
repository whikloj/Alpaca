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

package ca.islandora.alpaca.triplestore;

import static java.net.URLEncoder.encode;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.camel.Exchange.CONTENT_TYPE;
import static org.apache.camel.builder.Builder.exchangeProperty;
import static org.apache.camel.test.junit5.TestSupport.assertIsInstanceOf;
import static org.apache.camel.util.ObjectHelper.loadResourceAsStream;

import java.util.Arrays;
import java.util.List;

import ca.islandora.alpaca.AlpacaCamelTestSupport;
import ca.islandora.alpaca.config.AlpacaConfig;
import org.apache.camel.CamelContext;
import org.apache.camel.CamelExecutionException;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.test.junit5.TestSupport;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ca.islandora.alpaca.exceptions.MissingCanonicalUrlException;
import ca.islandora.alpaca.exceptions.MissingJsonldUrlException;

/**
 * Test class for the {@link TriplestoreIndexer} route.
 * @author dannylamb
 * @author whikloj
 */
public class TriplestoreIndexerTest extends AlpacaCamelTestSupport {

    protected MockEndpoint resultEndpoint;

    protected ProducerTemplate template;

    private static final TriplestoreIndexerOptions options = new TriplestoreIndexerOptions();
    private static final AlpacaConfig alpacaConfig = new AlpacaConfig();

    private CamelContext context;

    @BeforeAll
    public static void setProperties() {
        ALL_ROUTES = List.of(
                "IslandoraTriplestoreIndexer",
                "IslandoraTriplestoreIndexerDelete"
        );
        options.setBaseUrl("http://localhost:8080/bigdata/namespace/islandora/sparql");
        options.setIndexStream("topic:islandora-indexing-triplestore-index");
        options.setDeleteStream("topic:islandora-indexing-triplestore-delete");
        options.setConcurrentConsumers(1);
        options.setMaxConcurrentConsumers(1);
        options.setAsyncConsumer(false);
        alpacaConfig.setMaxRedeliveries(1);
        alpacaConfig.setConfig("test-config");
    }

    @BeforeEach
    public void setUp() throws Exception {
        context = new DefaultCamelContext();
        context.addRoutes(new TriplestoreIndexer(options, alpacaConfig));
        template = context.createProducerTemplate();
        resultEndpoint = TestSupport.getMockEndpoint(context, "mock:result", true);
    }

    @Test
    public void testParseUrl() throws Exception {
        final String route = "IslandoraTriplestoreIndexerParseUrl";

        context.getRoutes();
        AdviceWith.adviceWith(context, route, a -> {
            a.replaceFromWith("direct:start");
            a.weaveAddLast().to(resultEndpoint);
        });
        replaceFromToNoop(context, List.of(route));
        context.start();

        resultEndpoint.expectedMessageCount(1);

        final Exchange exchange = template.send("direct:start", xchange ->
                xchange.getIn().setBody(IOUtils.toString(loadResourceAsStream("triplestore/AS2Event.jsonld"), UTF_8))
        );


        TestSupport.assertPredicate(
                exchangeProperty("jsonld_url").isEqualTo("http://localhost:8000/node/1?_format=jsonld"),
                exchange,
                true
        );
        TestSupport.assertPredicate(
                exchangeProperty("subject_url").isEqualTo("http://localhost:8000/node/1"),
                exchange,
                true
        );
        resultEndpoint.assertIsSatisfied();
    }

    @Test
    public void testParseUrlDiesOnNoJsonld() throws Exception {
        final String route = "IslandoraTriplestoreIndexerParseUrl";

        AdviceWith.adviceWith(context, route, a -> {
            a.replaceFromWith("direct:start");
            a.weaveAddLast().to(resultEndpoint);
        });
        replaceFromToNoop(context, List.of(route));
        context.start();

        resultEndpoint.expectedMessageCount(0);

        // Make sure it dies if you can't extract the jsonld url from the event.
        try {
            template.sendBody("direct:start",
                    IOUtils.toString(loadResourceAsStream("triplestore/AS2EventNoJsonldUrl.jsonld"), UTF_8));
        } catch (final CamelExecutionException e) {
            assertIsInstanceOf(MissingJsonldUrlException.class, e.getCause());
        }

        resultEndpoint.assertIsSatisfied();
    }

    @Test
    public void testParseUrlDiesOnNoCanonical() throws Exception {
        final String route = "IslandoraTriplestoreIndexerParseUrl";

        AdviceWith.adviceWith(context, route, a -> {
            a.replaceFromWith("direct:start");
            a.weaveAddLast().to(resultEndpoint);
        });
        replaceFromToNoop(context, List.of(route));
        context.start();

        resultEndpoint.expectedMessageCount(0);

        // Make sure it dies if you can't extract the jsonld url from the event.
        try {
            template.sendBody("direct:start",
                    IOUtils.toString(loadResourceAsStream("triplestore/AS2EventNoCanonicalUrl.jsonld"), UTF_8));
        } catch (final CamelExecutionException e) {
            assertIsInstanceOf(MissingCanonicalUrlException.class, e.getCause());
        }

        resultEndpoint.assertIsSatisfied();
    }

    @Test
    public void testIndex() throws Exception {
        final String route = "IslandoraTriplestoreIndexer";

        context.disableJMX();
        AdviceWith.adviceWith(context, route, a -> {
            a.replaceFromWith("direct:start");

            // Rig Drupal REST endpoint to return canned jsonld
            a.interceptSendToEndpoint("http://localhost:8000/node/1?_format=jsonld&connectionClose=true" +
                            "&disableStreamCache=true")
                    .skipSendToOriginalEndpoint()
                    .process(exchange -> {
                        exchange.getIn().removeHeaders("*");
                        exchange.getIn().setHeader("Content-Type", "application/ld+json");
                        exchange.getIn().setBody(
                                IOUtils.toString(loadResourceAsStream("triplestore/node.jsonld"), UTF_8),
                                String.class);
                    });

            a.mockEndpointsAndSkip(
                "http://localhost:8080/bigdata/namespace/islandora/sparql?connectionClose=true&disableStreamCache=true"
            );
        });
        replaceFromToNoop(context, List.of(route));
        context.start();

        final String subject = "<http://localhost:8000/node/1>";
        final String responsePrefix =
                "DELETE WHERE { " + subject + " ?p ?o };\n" +
                        "INSERT DATA { ";
        final List<String> triples = Arrays.asList(
                subject + " <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://schema.org/Thing> .",
                subject + " <http://schema.org/dateCreated> \"2017-01-30T04:36:07+00:00\" .",
                subject + " <http://schema.org/dateModified> \"2017-01-30T14:35:57+00:00\" ."
        );

        final MockEndpoint endpoint = TestSupport.getMockEndpoint(context,
                "mock:http:localhost:8080/bigdata/namespace/islandora/sparql",
                true
        );

        endpoint.expectedMessageCount(1);
        endpoint.expectedHeaderReceived(Exchange.HTTP_METHOD, "POST");
        endpoint.expectedHeaderReceived(CONTENT_TYPE, "application/x-www-form-urlencoded; charset=utf-8");
        endpoint.allMessages().body().startsWith("update=" + encode(responsePrefix, UTF_8));
        endpoint.allMessages().body().endsWith(encode("\n}", UTF_8));
        for (final String triple : triples) {
            endpoint.expectedBodyReceived().body().contains(encode(triple, UTF_8));
        }

        template.send("direct:start", exchange -> {
                exchange.getIn().setBody(IOUtils.toString(loadResourceAsStream("triplestore/AS2Event.jsonld"), UTF_8));
        });

        endpoint.assertIsSatisfied();
    }

    @Test
    public void testDelete() throws Exception {
        final String route = "IslandoraTriplestoreIndexerDelete";

        context.disableJMX();
        AdviceWith.adviceWith(context, route, a -> {
            a.replaceFromWith("direct:start");
            a.mockEndpointsAndSkip("broker:*");
            a.mockEndpointsAndSkip("http://localhost:8080/bigdata/namespace/islandora/sparql?" +
                    "connectionClose=true&disableStreamCache=true");
        });
        replaceFromToNoop(context, List.of(route));
        context.start();

        final MockEndpoint endpoint = TestSupport.getMockEndpoint(context,
                "mock:http:localhost:8080/bigdata/namespace/islandora/sparql",
                true
        );

        endpoint.expectedMessageCount(1);
        endpoint.expectedHeaderReceived(Exchange.HTTP_METHOD, "POST");
        endpoint.expectedHeaderReceived(CONTENT_TYPE, "application/x-www-form-urlencoded; charset=utf-8");
        endpoint.allMessages().body().startsWith(
                "update=" + encode("DELETE WHERE { <http://localhost:8000/node/1> ?p ?o }", UTF_8)
        );

        template.send("direct:start", exchange -> {
            exchange.getIn().setBody(IOUtils.toString(loadResourceAsStream("triplestore/AS2Event.jsonld"), UTF_8));
        });

        endpoint.assertIsSatisfied();
    }

}
