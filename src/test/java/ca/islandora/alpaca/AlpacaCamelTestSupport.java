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
package ca.islandora.alpaca;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.AdviceWith;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Base class for Alpaca Camel tests, providing support for replacing "from" endpoints with direct:noop endpoints.
 * This allows for testing routes without needing to start the actual endpoints.
 * @author whikloj
 */
public class AlpacaCamelTestSupport  {

    /**
     * List of all routes that are part of the route builder and have an activeMQ "from" endpoint.
     * Used to replace the "from" endpoint with a direct:noop_<routeCounter>
     */
    protected static List<String> ALL_ROUTES = List.of();

    /**
     * Replace the "from" endpoint of all routes except ones in goodRoutes with a direct:noop_<routeCounter>
     * @param context  The Camel context to modify
     * @param goodRoutes List of route IDs that should not be replaced
     */
    protected void replaceFromToNoop(final CamelContext context, final List<String> goodRoutes) {
        final AtomicInteger routeCounter = new AtomicInteger(1);
        ALL_ROUTES.stream().filter(a -> !goodRoutes.contains(a)).forEach(route -> {
            try {
                AdviceWith.adviceWith(context, route, a -> {
                    a.replaceFromWith("direct:noop_" + routeCounter);
                });
                routeCounter.addAndGet(1);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }
}
