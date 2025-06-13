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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.camel.util.ObjectHelper.loadResourceAsStream;

import ca.islandora.alpaca.AlpacaCamelTestSupport;
import ca.islandora.alpaca.config.AlpacaConfig;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.model.ToDynamicDefinition;
import org.apache.camel.test.junit5.TestSupport;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * Tests for the {@link FcrepoIndexer} route.
 * @author dannylamb
 * @author whikloj
 */
public class FcrepoIndexerTest extends AlpacaCamelTestSupport {

    protected ProducerTemplate template;

    private static final FcrepoIndexerOptions fcrepoIndexerOptions = new FcrepoIndexerOptions();
    private static final AlpacaConfig alpacaConfig = new AlpacaConfig();

    @BeforeAll
    public static void beforeAll() {
        ALL_ROUTES = List.of("FcrepoIndexerNode", "FcrepoIndexerDeleteNode", "FcrepoIndexerMedia",
                "FcrepoIndexerExternalFile");
        fcrepoIndexerOptions.setEnabled(true);
        fcrepoIndexerOptions.setNode("topic:islandora-indexing-fcrepo-content");
        fcrepoIndexerOptions.setDelete("topic:islandora-indexing-fcrepo-delete");
        fcrepoIndexerOptions.setExternal("topic:islandora-indexing-fcrepo-file-external");
        fcrepoIndexerOptions.setMedia("topic:islandora-indexing-fcrepo-media");
        fcrepoIndexerOptions.setMillinerBaseUrl("http://localhost:8000/milliner/");
        fcrepoIndexerOptions.setFedoraHeader("X-ISLANDORA-FEDORA-HEADER");
        alpacaConfig.setMaxRedeliveries(1);
    }

    private CamelContext context;

    @BeforeEach
    public void setUp() throws Exception {
        context = new DefaultCamelContext();
        context.addRoutes(new FcrepoIndexer(fcrepoIndexerOptions, alpacaConfig));
        template = context.createProducerTemplate();
    }

    @Test
    public void testNode() throws Exception {
        final String route = "FcrepoIndexerNode";
        final String nodeSubRoute = "FcrepoIndexerNodeIndex";

        context.disableJMX();
        AdviceWith.adviceWith(context, route, a ->
            a.replaceFromWith("direct:start")
        );
        AdviceWith.adviceWith(context, nodeSubRoute, a ->
            a.weaveByType(ToDynamicDefinition.class).selectIndex(0).replace().toD("mock:localhost:8000" +
                    "/milliner/node/${exchangeProperty.uuid}")
        );
        replaceFromToNoop(context, List.of(route, nodeSubRoute));
        context.start();

        // Assert we POST to milliner with creds.
        final MockEndpoint milliner = TestSupport.getMockEndpoint(context,
            "mock:localhost:8000/milliner/node/72358916-51e9-4712-b756-4b0404c91b1d",
            true
        );
        milliner.expectedMessageCount(1);
        milliner.expectedHeaderReceived("Authorization", "Bearer islandora");
        milliner.expectedHeaderReceived("Content-Location", "http://localhost:8000/node/2?_format=jsonld");
        milliner.expectedHeaderReceived(Exchange.HTTP_METHOD, "POST");
        milliner.expectedHeaderReceived("X-ISLANDORA-FEDORA-HEADER", "http://localhost:8080/fcrepo/rest/node");

        // Send an event.
        template.send("direct:start", exchange -> {
            exchange.getIn().setHeader("Authorization", "Bearer islandora");
            exchange.getIn().setBody(
                    IOUtils.toString(loadResourceAsStream("fcrepo/NodeAS2Event.jsonld"), UTF_8),
                    String.class
            );
        });

        milliner.assertIsSatisfied();
    }

    @Test
    public void testNodeVersion() throws Exception {
        final String route = "FcrepoIndexerNode";
        final String versionSubRoute = "FcrepoIndexerNodeVersion";

        context.disableJMX();
        AdviceWith.adviceWith(context, route, a ->
            a.replaceFromWith("direct:start")
        );
        AdviceWith.adviceWith(context, versionSubRoute, a ->
            a.weaveByType(ToDynamicDefinition.class).selectIndex(0).replace().toD("mock:localhost:8000" +
                    "/milliner/node/${exchangeProperty.uuid}/version")
        );
        replaceFromToNoop(context, List.of(route,versionSubRoute));
        context.start();

        // Assert we POST to milliner with creds.
        final MockEndpoint milliner = TestSupport.getMockEndpoint(context,
                "mock:localhost:8000/milliner/node/72358916-51e9-4712-b756-4b0404c91b/version",
                true
        );
        milliner.expectedMessageCount(1);
        milliner.expectedHeaderReceived("Authorization", "Bearer islandora");
        milliner.expectedHeaderReceived("Content-Location", "http://localhost:8000/node/2?_format=jsonld");
        milliner.expectedHeaderReceived(Exchange.HTTP_METHOD, "POST");

        // Send an event.
        template.send("direct:start", exchange -> {
            exchange.getIn().setHeader("Authorization", "Bearer islandora");
            exchange.getIn().setBody(
                IOUtils.toString(loadResourceAsStream("fcrepo/VersionAS2Event.jsonld"), UTF_8),
                    String.class);
        });

        milliner.assertIsSatisfied();
    }

    @Test
    public void testNodeDelete() throws Exception {
        final String route = "FcrepoIndexerDeleteNode";

        context.disableJMX();
        AdviceWith.adviceWith(context, route, a -> {
            a.replaceFromWith("direct:start");
            a.weaveByType(ToDynamicDefinition.class).selectIndex(0).replace().toD("mock:localhost:8000/milliner/node" +
                    "/${exchangeProperty.uuid}");
        });
        replaceFromToNoop(context, List.of(route));
        context.start();

        // Assert we DELETE to milliner with creds.
        final MockEndpoint milliner = TestSupport.getMockEndpoint(context,
            "mock:localhost:8000/milliner/node/72358916-51e9-4712-b756-4b0404c91b1d",
                true
        );
        milliner.expectedMessageCount(1);
        milliner.expectedHeaderReceived("Authorization", "Bearer islandora");
        milliner.expectedHeaderReceived(Exchange.HTTP_METHOD, "DELETE");
        milliner.expectedHeaderReceived("X-ISLANDORA-FEDORA-HEADER", "http://localhost:8080/fcrepo/rest/node");

        // Send an event.
        template.send("direct:start", exchange -> {
            exchange.getIn().setHeader("Authorization", "Bearer islandora");
            exchange.getIn().setBody(
                    IOUtils.toString(loadResourceAsStream("fcrepo/NodeAS2Event.jsonld"), "UTF-8"),
                    String.class
            );
        });

        milliner.assertIsSatisfied();
    }

    @Test
    public void testExternalFile() throws Exception {
        final String route = "FcrepoIndexerExternalFile";

        context.disableJMX();
        AdviceWith.adviceWith(context, route, a -> {
            a.replaceFromWith("direct:start");
            a.weaveByType(ToDynamicDefinition.class).selectIndex(0).replace().toD("mock:localhost:8000/milliner/" +
                    "external/${exchangeProperty.uuid}");
        });
        replaceFromToNoop(context, List.of(route));
        context.start();

        // Assert we POST to Milliner with creds.
        final MockEndpoint milliner = TestSupport.getMockEndpoint(context,
            "mock:localhost:8000/milliner/external/148dfe8f-9711-4263-97e7-3ef3fb15864f",
                true
        );
        milliner.expectedMessageCount(1);
        milliner.expectedHeaderReceived("Authorization", "Bearer islandora");
        milliner.expectedHeaderReceived(
            "Content-Location",
            "http://localhost:8000/sites/default/files/2018-08/Voltaire-Records1.jpg"
        );
        milliner.expectedHeaderReceived(Exchange.HTTP_METHOD, "POST");
        milliner.expectedHeaderReceived("X-ISLANDORA-FEDORA-HEADER", "http://localhost:8080/fcrepo/rest/externalFile");

        // Send an event.
        template.send("direct:start", exchange -> {
            exchange.getIn().setHeader("Authorization", "Bearer islandora");
            exchange.getIn().setBody(
                    IOUtils.toString(loadResourceAsStream("fcrepo/ExternalFileAS2Event.jsonld"), UTF_8),
                    String.class
            );
        });

        milliner.assertIsSatisfied();
    }

    @Test
    public void testMedia() throws Exception {
        final String route = "FcrepoIndexerMedia";
        final String mediaSubRoute = "FcrepoIndexerMediaIndex";

        context.disableJMX();
        AdviceWith.adviceWith(context, route, a ->
            a.replaceFromWith("direct:start")
        );
        AdviceWith.adviceWith(context, mediaSubRoute, a->
            a.weaveByType(ToDynamicDefinition.class).selectIndex(0).replace().toD("mock:localhost:8000/milliner/" +
                    "media/${exchangeProperty.sourceField}")
        );
        replaceFromToNoop(context, List.of(route, mediaSubRoute));
        context.start();

        // Assert we POST the event to milliner with creds.
        final MockEndpoint milliner = TestSupport.getMockEndpoint(context,
                "mock:localhost:8000/milliner/media/field_media_image",
                true
        );
        milliner.expectedMessageCount(1);
        milliner.expectedHeaderReceived("Authorization", "Bearer islandora");
        milliner.expectedHeaderReceived("Content-Location", "http://localhost:8000/media/6?_format=json");
        milliner.expectedHeaderReceived(Exchange.HTTP_METHOD, "POST");
        milliner.expectedHeaderReceived("X-ISLANDORA-FEDORA-HEADER", "http://localhost:8080/fcrepo/rest/media");

        // Send an event.
        template.send("direct:start", exchange -> {
            exchange.getIn().setHeader("Authorization", "Bearer islandora");
            exchange.getIn().setBody(
                    IOUtils.toString(loadResourceAsStream("fcrepo/MediaAS2Event.jsonld"), UTF_8),
                    String.class
            );
        });

        milliner.assertIsSatisfied();
    }

    @Test
    public void testVersionMedia() throws Exception {
        final String route = "FcrepoIndexerMedia";
        final String versionSubRoute = "FcrepoIndexerMediaIndexVersion";

        context.disableJMX();
        AdviceWith.adviceWith(context, route, a -> a.replaceFromWith("direct:start"));
        AdviceWith.adviceWith(context, versionSubRoute, a ->
            a.weaveByType(ToDynamicDefinition.class).selectIndex(0).replace().toD("mock:localhost:8000/milliner/" +
                    "media/${exchangeProperty.sourceField}/version")
        );
        replaceFromToNoop(context, List.of(route, versionSubRoute));
        context.start();

        // Assert we POST the event to milliner with creds.
        final MockEndpoint milliner = TestSupport.getMockEndpoint(context,
                "mock:localhost:8000/milliner/media/field_media_image/version",
                true
        );
        milliner.expectedHeaderReceived("Authorization", "Bearer islandora");
        milliner.expectedHeaderReceived("Content-Location", "http://localhost:8000/media/7?_format=json");
        milliner.expectedHeaderReceived(Exchange.HTTP_METHOD, "POST");

        // Send an event.
        template.send("direct:start", exchange -> {
            exchange.getIn().setHeader("Authorization", "Bearer islandora");
            exchange.getIn().setBody(
                    IOUtils.toString(loadResourceAsStream("fcrepo/MediaVersionAS2Event.jsonld"), UTF_8),
                    String.class);
        });

        milliner.assertIsSatisfied();
    }

}
