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

package ca.islandora.alpaca.indexing.triplestore;

import static org.apache.camel.LoggingLevel.ERROR;
import static org.apache.camel.LoggingLevel.INFO;
import static org.apache.camel.LoggingLevel.TRACE;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_URI;
import static org.slf4j.LoggerFactory.getLogger;

import com.jayway.jsonpath.JsonPathException;

import net.minidev.json.JSONArray;
import org.apache.camel.PropertyInject;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.Exchange;
import org.apache.camel.component.activemq.ActiveMQComponent;
import org.fcrepo.camel.processor.SparqlUpdateProcessor;
import org.fcrepo.camel.processor.SparqlDeleteProcessor;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;

/**
 * @author dhlamb
 */
@SpringBootApplication
@Component
public class TriplestoreIndexer extends RouteBuilder {

    /**
     * The logger.
     */
    private static final Logger LOGGER = getLogger(TriplestoreIndexer.class);

    /**
     * Static application method.
     * @param args Command line arguments.
     */
    public static void main(final String[] args) {
        SpringApplication.run(TriplestoreIndexer.class, args);
    }

    @Autowired
    private ActiveMQComponent component;

    @PropertyInject("broker.name")
    private String brokerName;

    @Override
    public void configure() {
        LOGGER.debug("TriplestoreIndexer routes starting");
        getContext().addComponent(brokerName, component);
        // Global exception handler for the indexer.
        // Just logs after retrying X number of times.
        onException(Exception.class)
            .maximumRedeliveries("{{error.maxRedeliveries}}")
            .log(
                ERROR,
                LOGGER,
                "Error indexing ${exchangeProperty.uri} in triplestore: ${exception.message}\n\n${exception.stacktrace}"
            );

        from("{{index.stream}}")
            .routeId("IslandoraTriplestoreIndexer")
                .log(TRACE, LOGGER, "Received message on IslandoraTriplestoreIndexer")
              .to("direct:parse.url")
              .removeHeaders("*", "Authorization")
              .setHeader(Exchange.HTTP_METHOD, constant("GET"))
              .setBody(simple("${null}"))
              .toD("${exchangeProperty.jsonld_url}&connectionClose=true")
              .setHeader(FCREPO_URI, simple("${exchangeProperty.subject_url}"))
              .process(new SparqlUpdateProcessor())
              .log(INFO, LOGGER, "Indexing ${exchangeProperty.subject_url} in triplestore")
              .to("{{triplestore.baseUrl}}?connectionClose=true");

        from("{{delete.stream}}")
            .routeId("IslandoraTriplestoreIndexerDelete")
              .to("direct:parse.url")
              .setHeader(FCREPO_URI, simple("${exchangeProperty.subject_url}"))
              .process(new SparqlDeleteProcessor())
              .log(INFO, LOGGER, "Deleting ${exchangeProperty.subject_url} in triplestore")
              .to("{{triplestore.baseUrl}}?connectionClose=true");

        // Extracts the JSONLD URL from the event message and stores it on the exchange.
        from("direct:parse.url")
            .routeId("IslandoraTriplestoreIndexerParseUrl")
              // Custom exception handlers.  Don't retry if event is malformed.
              .onException(JsonPathException.class)
                .maximumRedeliveries(0)
                .log(
                   ERROR,
                   LOGGER,
                   "Error extracting properties from event: ${exception.message}\n\n${exception.stacktrace}"
                )
                .end()
              .onException(RuntimeException.class)
                .maximumRedeliveries(0)
                .log(
                   ERROR,
                   LOGGER,
                   "Error extracting properties from event: ${exception.message}\n\n${exception.stacktrace}"
                )
                .end()
              .transform().jsonpath("$.object.url")
              .process(ex -> {
                  // Parse the event message.
                  final JSONArray message = ex.getIn().getBody(JSONArray.class);

                  // Get the JSONLD url.
                  final LinkedHashMap jsonldUrl = message.stream()
                          .map(LinkedHashMap.class::cast)
                          .filter(elem -> "application/ld+json".equals(elem.get("mediaType")))
                          .findFirst()
                          .orElseThrow(() -> new RuntimeException("Cannot find JSONLD URL in event message."));
                  ex.setProperty("jsonld_url", jsonldUrl.get("href"));

                  // Attempt to get the 'describes' url first, but if it fails, fall back to the canonical.
                  try {
                      final LinkedHashMap describesUrl = message.stream()
                              .map(LinkedHashMap.class::cast)
                              .filter(elem -> "describes".equals(elem.get("rel")))
                              .findFirst()
                              .orElseThrow(() -> new RuntimeException("Cannot find describes URL in event message."));
                      ex.setProperty("subject_url", describesUrl.get("href"));
                  } catch (RuntimeException e) {
                      final LinkedHashMap canonicalUrl = message.stream()
                              .map(LinkedHashMap.class::cast)
                              .filter(elem -> "canonical".equals(elem.get("rel")))
                              .findFirst()
                              .orElseThrow(() -> new RuntimeException("Cannot find canonical URL in event message."));
                      ex.setProperty("subject_url", canonicalUrl.get("href"));
                  }
              });
    }
}
