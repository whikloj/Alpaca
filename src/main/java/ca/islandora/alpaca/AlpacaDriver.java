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

import static ca.islandora.alpaca.config.AlpacaConfig.ALPACA_CONFIG_PROPERTY;
import static org.slf4j.LoggerFactory.getLogger;

import java.nio.file.Path;
import java.util.concurrent.Callable;

import ca.islandora.alpaca.driver.VersionProvider;
import org.slf4j.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.context.annotation.ComponentScan;
import picocli.CommandLine;

/**
 * The Command line SpringBoot application class
 * @author whikloj
 */
@CommandLine.Command(name = "alpaca", mixinStandardHelpOptions = true, sortOptions = false,
        versionProvider = VersionProvider.class)
@SpringBootApplication
@ComponentScan(basePackages = "ca.islandora.alpaca")
public class AlpacaDriver implements Callable<Integer> {

    /**
     * Logger instance.
     */
    private static final Logger LOGGER = getLogger(AlpacaDriver.class);

    /**
     * Configuration file.
     */
    @CommandLine.Option(names = {"--config", "-c"}, required = false, order = 1,
            description = "The path to the configuration file")
    private Path configurationFilePath;

    @Override
    public Integer call() throws Exception {
        final SpringApplication app = new SpringApplication(AlpacaDriver.class);
        if (configurationFilePath != null) {
            System.setProperty(ALPACA_CONFIG_PROPERTY, configurationFilePath.toFile().getAbsolutePath());
        }

        final var context = app.run();
        // Block until the application is stopped
        Thread.currentThread().join();
        return 0;
    }

    /**
     * @param args Command line arguments
     */
    public static void main(final String[] args) {
        final int exitCode = new CommandLine(new AlpacaDriver()).execute(args);
        System.exit(exitCode);
    }

}
