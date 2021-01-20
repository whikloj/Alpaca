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

package ca.islandora.alpaca.indexing.fcrepo;

import static org.apache.camel.util.ObjectHelper.loadResourceAsStream;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.reifier.RouteReifier;
import org.apache.camel.test.spring.CamelSpringBootRunner;
import org.apache.camel.test.spring.UseAdviceWith;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author dannylamb
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@UseAdviceWith
@ContextConfiguration(locations = {"/context-test.xml"})
@RunWith(CamelSpringBootRunner.class)
public class FcrepoIndexerTest {

    @Produce("direct:start")
    protected ProducerTemplate template;

    @Autowired
    CamelContext context;

    @Test
    public void testNode() throws Exception {
        final String route = "FcrepoIndexerNode";

        final ModelCamelContext mcc = context.adapt(ModelCamelContext.class);
        RouteReifier.adviceWith(mcc.getRouteDefinition(route), mcc, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                replaceFromWith("direct:start");
                mockEndpointsAndSkip(
                        "http://localhost:8000/milliner/node/72358916-51e9-4712-b756-4b0404c91b1d?connectionClose=true"
                );
            }
        });
        context.start();

        // Assert we POST to milliner with creds.
        final MockEndpoint milliner = (MockEndpoint) context.getEndpoint(
            "mock:http:localhost:8000/milliner/node/72358916-51e9-4712-b756-4b0404c91b1d"
        );
        milliner.expectedMessageCount(1);
        milliner.expectedHeaderReceived("Authorization", "Bearer islandora");
        milliner.expectedHeaderReceived("Content-Location", "http://localhost:8000/node/2?_format=jsonld");
        milliner.expectedHeaderReceived(Exchange.HTTP_METHOD, "POST");
        milliner.expectedHeaderReceived(FcrepoIndexer.FEDORA_HEADER, "http://localhost:8080/fcrepo/rest/node");

        // Send an event.
        template.send(exchange -> {
            exchange.getIn().setHeader("Authorization", "Bearer islandora");
            exchange.getIn().setBody(
                    IOUtils.toString(loadResourceAsStream("NodeAS2Event.jsonld"), "UTF-8"),
                    String.class
            );
        });

        milliner.assertIsSatisfied();
    }

    @Test
    public void testNodeVersion() throws Exception {
        final String route = "FcrepoIndexerNode";
        final ModelCamelContext mcc = context.adapt(ModelCamelContext.class);
        RouteReifier.adviceWith(mcc.getRouteDefinition(route), mcc, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                replaceFromWith("direct:start");
                mockEndpointsAndSkip(
                    "http://localhost:8000/milliner/node/72358916-51e9-"
                    + "4712-b756-4b0404c91b/version?connectionClose=true"
                );
            }
        });
        context.start();

        // Assert we POST to milliner with creds.
        final MockEndpoint milliner = (MockEndpoint) context.getEndpoint(
                "mock:http:localhost:8000/milliner/"
                + "node/72358916-51e9-4712-b756-4b0404c91b/version"
        );
        milliner.expectedMessageCount(1);
        milliner.expectedHeaderReceived("Authorization", "Bearer islandora");
        milliner.expectedHeaderReceived("Content-Location", "http://localhost:8000/node/2?_format=jsonld");
        milliner.expectedHeaderReceived(Exchange.HTTP_METHOD, "POST");

        // Send an event.
        template.send(exchange -> {
            exchange.getIn().setHeader("Authorization", "Bearer islandora");
            exchange.getIn().setBody(
                IOUtils.toString(loadResourceAsStream("VersionAS2Event.jsonld"), "UTF-8"),
                    String.class);
        });

        milliner.assertIsSatisfied();
    }

    @Test
    public void testNodeDelete() throws Exception {
        final String route = "FcrepoIndexerDeleteNode";
        final ModelCamelContext mcc = context.adapt(ModelCamelContext.class);
        RouteReifier.adviceWith(mcc.getRouteDefinition(route), mcc, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                replaceFromWith("direct:start");
                mockEndpointsAndSkip(
                    "http://localhost:8000/milliner/node/72358916-51e9-4712-b756-4b0404c91b1d?connectionClose=true"
                );
            }
        });
        context.start();

        // Assert we DELETE to milliner with creds.
        final MockEndpoint milliner = (MockEndpoint) context.getEndpoint(
            "mock:http:localhost:8000/milliner/node/72358916-51e9-4712-b756-4b0404c91b1d"
        );
        milliner.expectedMessageCount(1);
        milliner.expectedHeaderReceived("Authorization", "Bearer islandora");
        milliner.expectedHeaderReceived(Exchange.HTTP_METHOD, "DELETE");
        milliner.expectedHeaderReceived(FcrepoIndexer.FEDORA_HEADER, "http://localhost:8080/fcrepo/rest/node");

        // Send an event.
        template.send(exchange -> {
            exchange.getIn().setHeader("Authorization", "Bearer islandora");
            exchange.getIn().setBody(
                    IOUtils.toString(loadResourceAsStream("NodeAS2Event.jsonld"), "UTF-8"),
                    String.class
            );
        });

        milliner.assertIsSatisfied();
    }

    @Test
    public void testFile() throws Exception {
        final String route = "FcrepoIndexerFile";
        final ModelCamelContext mcc = context.adapt(ModelCamelContext.class);
        RouteReifier.adviceWith(mcc.getRouteDefinition(route), mcc, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                replaceFromWith("direct:start");
                mockEndpointsAndSkip(
                    "http://localhost:8000/gemini/148dfe8f-9711-4263-97e7-3ef3fb15864f?connectionClose=true"
                );
            }
        });
        context.start();

        // Assert we PUT to gemini with creds.
        final MockEndpoint gemini = (MockEndpoint) context.getEndpoint(
            "mock:http:localhost:8000/gemini/148dfe8f-9711-4263-97e7-3ef3fb15864f"
        );
        gemini.expectedMessageCount(1);
        gemini.expectedHeaderReceived("Authorization", "Bearer islandora");
        gemini.expectedHeaderReceived(Exchange.CONTENT_TYPE, "application/json");
        gemini.expectedHeaderReceived(Exchange.HTTP_METHOD, "PUT");
        gemini.expectedHeaderReceived(FcrepoIndexer.FEDORA_HEADER, "http://localhost:8080/fcrepo/rest/file");
        gemini.allMessages().body().startsWith(
            "{\"drupal\": \"http://localhost:8000/_flysystem/fedora/2018-08/Voltaire-Records1.jpg\", \"fedora\": " +
            "\"http://localhost:8080/fcrepo/rest/2018-08/Voltaire-Records1.jpg\"}"
        );

        // Send an event.
        template.send(exchange -> {
            exchange.getIn().setHeader("Authorization", "Bearer islandora");
            exchange.getIn().setBody(
                    IOUtils.toString(loadResourceAsStream("FileAS2Event.jsonld"), "UTF-8"),
                    String.class
            );
        });

        gemini.assertIsSatisfied();
    }

    @Test
    public void testExternalFile() throws Exception {
        final String route = "FcrepoIndexerExternalFile";
        final ModelCamelContext mcc = context.adapt(ModelCamelContext.class);
        RouteReifier.adviceWith(mcc.getRouteDefinition(route), mcc, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                replaceFromWith("direct:start");
                mockEndpointsAndSkip(
                    "http://localhost:8000/milliner/external/148dfe8f-9711-4263-97e7-3ef3fb15864f?connectionClose=true"
                );
            }
        });
        context.start();

        // Assert we POST to Milliner with creds.
        final MockEndpoint milliner = (MockEndpoint) context.getEndpoint(
            "mock:http:localhost:8000/milliner/external/148dfe8f-9711-4263-97e7-3ef3fb15864f"
        );
        milliner.expectedMessageCount(1);
        milliner.expectedHeaderReceived("Authorization", "Bearer islandora");
        milliner.expectedHeaderReceived(
            "Content-Location",
            "http://localhost:8000/sites/default/files/2018-08/Voltaire-Records1.jpg"
        );
        milliner.expectedHeaderReceived(Exchange.HTTP_METHOD, "POST");
        milliner.expectedHeaderReceived(FcrepoIndexer.FEDORA_HEADER, "http://localhost:8080/fcrepo/rest/externalFile");

        // Send an event.
        template.send(exchange -> {
            exchange.getIn().setHeader("Authorization", "Bearer islandora");
            exchange.getIn().setBody(
                    IOUtils.toString(loadResourceAsStream("ExternalFileAS2Event.jsonld"), "UTF-8"),
                    String.class
            );
        });

        milliner.assertIsSatisfied();
    }

    @Test
    public void testMedia() throws Exception {
        final String route = "FcrepoIndexerMedia";
        final ModelCamelContext mcc = context.adapt(ModelCamelContext.class);
        RouteReifier.adviceWith(mcc.getRouteDefinition(route), mcc, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                replaceFromWith("direct:start");
                mockEndpointsAndSkip("http://localhost:8000/milliner/media/field_media_image?connectionClose=true");
            }
        });
        context.start();

        // Assert we POST the event to milliner with creds.
        final MockEndpoint milliner = (MockEndpoint) context
                .getEndpoint("mock:http:localhost:8000/milliner/media/field_media_image");
        milliner.expectedMessageCount(1);
        milliner.expectedHeaderReceived("Authorization", "Bearer islandora");
        milliner.expectedHeaderReceived("Content-Location", "http://localhost:8000/media/6?_format=json");
        milliner.expectedHeaderReceived(Exchange.HTTP_METHOD, "POST");
        milliner.expectedHeaderReceived(FcrepoIndexer.FEDORA_HEADER, "http://localhost:8080/fcrepo/rest/media");

        // Send an event.
        template.send(exchange -> {
            exchange.getIn().setHeader("Authorization", "Bearer islandora");
            exchange.getIn().setBody(
                    IOUtils.toString(loadResourceAsStream("MediaAS2Event.jsonld"), "UTF-8"),
                    String.class
            );
        });

        milliner.assertIsSatisfied();
    }

    @Test
    public void testVersionMedia() throws Exception {
        final String route = "FcrepoIndexerMedia";
        final ModelCamelContext mcc = context.adapt(ModelCamelContext.class);
        RouteReifier.adviceWith(mcc.getRouteDefinition(route), mcc, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                replaceFromWith("direct:start");
                mockEndpointsAndSkip("http://localhost:8000/milliner/media/field_media_image/"
                + "version?connectionClose=true");
            }
        });
        context.start();

        // Assert we POST the event to milliner with creds.
        final MockEndpoint milliner = (MockEndpoint) context.getEndpoint("mock:http:localhost:8000/milliner/media/"
        + "field_media_image/version");
        milliner.expectedHeaderReceived("Authorization", "Bearer islandora");
        milliner.expectedHeaderReceived("Content-Location", "http://localhost:8000/media/7?_format=json");
        milliner.expectedHeaderReceived(Exchange.HTTP_METHOD, "POST");

        // Send an event.
        template.send(exchange -> {
            exchange.getIn().setHeader("Authorization", "Bearer islandora");
            exchange.getIn().setBody(IOUtils.toString(loadResourceAsStream("MediaVersionAS2Event.jsonld"), "UTF-8"),
                    String.class);
        });

        milliner.assertIsSatisfied();
    }

}
