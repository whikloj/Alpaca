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
package ca.islandora.alpaca.config;

import ca.islandora.alpaca.AlpacaDriver;
import org.apache.camel.CamelContext;
import org.apache.camel.component.http.HttpComponent;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test of the RequestConfigurerConfig class
 *
 * @author whikloj
 */
@CamelSpringBootTest
@SpringBootTest(classes = {AlpacaDriver.class, HttpConfigurerContext.class},
        properties = {
                "request.configurer.enabled=true",
                "request.configurer.request-timeout=1234",
                "request.configurer.connection-timeout=2345",
                "request.configurer.socket-timeout=3456"
        }
        )
public class RequestConfigurerTest {

    @Autowired
    CamelContext camelContext;

    @Test
    void testHttpConfigurerApplied() {
        final var http = camelContext.getComponent("http", HttpComponent.class);
        assertEquals(1234, http.getConnectionRequestTimeout().toMilliseconds());
        assertEquals(2345, http.getConnectTimeout().toMilliseconds());
        assertEquals(3456, http.getSoTimeout().toMilliseconds());
    }
}
