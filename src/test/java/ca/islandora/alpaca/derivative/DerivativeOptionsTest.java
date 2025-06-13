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
import org.apache.camel.Route;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.impl.DefaultCamelContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.util.List;
import java.util.Set;

import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class for DerivativeOptions.
 * This class tests the creation of routes based on the properties defined in the environment.
 * @author whikloj
 */
public class DerivativeOptionsTest {

    private CamelContext context;

    private MockEnvironment env;

    private DerivativeOptions options;

    @BeforeEach
    void setUp() {
        context = new DefaultCamelContext();
        final AlpacaConfig alpacaConfig = new AlpacaConfig();
        alpacaConfig.setConfig("application.properties");
        alpacaConfig.setMaxRedeliveries(0);
        env = new MockEnvironment();
        options = new DerivativeOptions(env, context, alpacaConfig);
    }

    @Test
    void testDerivativeOptionsCreatesRoutes() throws Exception {
        // Set up mock environment with properties
        env.withProperty("derivative.foo.enabled", "true")
                .withProperty("derivative.foo.in-stream", "topic:foo")
                .withProperty("derivative.foo.service-url", "http://example.org/foo")
                .withProperty("derivative.bar.enabled", "true")
                .withProperty("derivative.bar.in-stream", "topic:bar")
                .withProperty("derivative.bar.service-url", "http://example.org/bar");

        options.setSystemsInstalled(List.of("foo", "bar")); // Needed as we aren't using Spring

        // Trigger route creation
        options.processAllServices();

        context.getRoutes();
        AdviceWith.adviceWith(context, "IslandoraConnectorDerivative-foo", a -> {
            a.replaceFromWith("direct:start-foo");
            a.weaveAddLast().to("mock:result");
        });
        AdviceWith.adviceWith(context, "IslandoraConnectorDerivative-bar", a -> {
            a.replaceFromWith("direct:start-bar");
            a.weaveAddLast().to("mock:result");
        });

        // Start context to initialize routes
        context.start();

        // Assert routes exist
        final Set<String> expectedRoutes = Set.of(
                "IslandoraConnectorDerivative-foo",
                "IslandoraConnectorDerivative-bar"
        );
        assertTrue(context.getRoutes().stream()
                .map(Route::getId)
                .collect(toSet())
                .containsAll(expectedRoutes));

        context.stop();
    }

    @Test
    void testDerivativeOptionsMissingInstalled() throws Exception {
        env.withProperty("derivative.foo.enabled", "true")
                .withProperty("derivative.foo.in-stream", "topic:foo")
                .withProperty("derivative.foo.service-url", "http://example.org/foo")
                .withProperty("derivative.bar.enabled", "true")
                .withProperty("derivative.bar.in-stream", "topic:bar")
                .withProperty("derivative.bar.service-url", "http://example.org/bar");

        options.setSystemsInstalled(List.of("foo")); // Only foo is installed

        options.processAllServices();
        context.getRoutes();
        AdviceWith.adviceWith(context, "IslandoraConnectorDerivative-foo", a -> {
            a.replaceFromWith("direct:start-foo");
            a.weaveAddLast().to("mock:result");
        });


        context.start();

        assertTrue(context.getRoutes().stream()
                .map(Route::getId)
                .collect(toSet())
                .contains("IslandoraConnectorDerivative-foo"));
        assertFalse(context.getRoutes().stream()
                .map(Route::getId)
                .collect(toSet())
                .contains("IslandoraConnectorDerivative-bar"));

        context.stop();
    }

    @Test
    void testDerivativeOptionsNotEnabled() throws Exception {
        env.withProperty("derivative.systems.installed", "foo,bar")
                .withProperty("derivative.foo.enabled", "false")
                .withProperty("derivative.foo.in-stream", "topic:foo")
                .withProperty("derivative.foo.service-url", "http://example.org/foo")
                .withProperty("derivative.bar.enabled", "true")
                .withProperty("derivative.bar.in-stream", "topic:bar")
                .withProperty("derivative.bar.service-url", "http://example.org/bar");

        options.setSystemsInstalled(List.of("foo", "bar"));

        options.processAllServices();

        context.getRoutes();
        AdviceWith.adviceWith(context, "IslandoraConnectorDerivative-bar", a -> {
            a.replaceFromWith("direct:start");
            a.weaveAddLast().to("mock:result");
        });

        context.start();

        assertTrue(context.getRoutes().stream()
                .map(Route::getId)
                .collect(toSet())
                .contains("IslandoraConnectorDerivative-bar"));
        assertFalse(context.getRoutes().stream()
                .map(Route::getId)
                .collect(toSet())
                .contains("IslandoraConnectorDerivative-foo"));

        context.stop();
    }
}
