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

import static ca.islandora.alpaca.config.AlpacaConfig.JMS_ENDPOINT_NAME;
import static ca.islandora.alpaca.config.AlpacaConfig.addJmsOptions;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Triplestore indexer configuration class.
 * @author whikloj
 */
@ConfigurationProperties(prefix = "triplestore.indexer")
public class TriplestoreIndexerOptions {
  /**
   * Comes from triplestore.indexer.enabled property
   */
  private boolean enabled;
  /**
   * Comes from triplestore.indexer.index-stream property
   */
  private String indexStream;
  /**
   * Comes from triplestore.indexer.delete-stream property
   */
  private String deleteStream;
  /**
   * Comes from triplestore.indexer.baseUrl property
   */
  private String baseUrl;
  /**
   * Comes from triplestore.indexer.concurrent-consumers property
   */
  private int concurrentConsumers;
  /**
   * Comes from triplestore.indexer.max-concurrent-consumers property
   */
  private int maxConcurrentConsumers;
  /**
   * Comes from triplestore.indexer.async-consumer property
   */
  private boolean asyncConsumer;

  /**
   * Get whether the triplestore indexer is enabled.
   * @return true if enabled, false otherwise.
   */
  public boolean isEnabled() {
    return enabled;
  }

  /**
   * Set whether the triplestore indexer is enabled.
   * @param enabled
   *   true to enable, false to disable.
   */
  public void setEnabled(final boolean enabled) {
    this.enabled = enabled;
  }

  /**
   * Get the index stream name.
   * @return
   *   The index stream name.
   */
  public String getIndexStream() {
    return indexStream;
  }

  /**
   * Set the index stream name.
   * @param indexStream
   *   The index stream name.
   */
  public void setIndexStream(final String indexStream) {
    this.indexStream = indexStream;
  }

  /**
   * Get the delete stream name.
   * @return
   *   The delete stream name.
   */
  public String getDeleteStream() {
    return deleteStream;
  }

  /**
   * Set the delete stream name.
   * @param deleteStream
   *   The delete stream name.
   */
  public void setDeleteStream(final String deleteStream) {
    this.deleteStream = deleteStream;
  }

  /**
   * Get the base URL for the triplestore.
   * @return
   *   The base URL for the triplestore.
   */
  public String getBaseUrl() {
    return baseUrl;
  }

  /**
   * Set the base URL for the triplestore.
   * @param baseUrl
   *   The base URL for the triplestore.
   */
  public void setBaseUrl(final String baseUrl) {
    this.baseUrl = baseUrl;
  }

  /**
   * Get the number of concurrent consumers for the JMS endpoint.
   * @return
   *   The number of concurrent consumers.
   */
  public int getConcurrentConsumers() {
    return concurrentConsumers;
  }

  /**
   * Set the number of concurrent consumers for the JMS endpoint.
   * @param concurrentConsumers
   *   The number of concurrent consumers.
   */
  public void setConcurrentConsumers(final int concurrentConsumers) {
    this.concurrentConsumers = concurrentConsumers;
  }

  /**
   * Get the maximum number of concurrent consumers for the JMS endpoint.
   * @return
   *   The maximum number of concurrent consumers.
   */
  public int getMaxConcurrentConsumers() {
    return maxConcurrentConsumers;
  }

  /**
   * Set the maximum number of concurrent consumers for the JMS endpoint.
   * @param maxConcurrentConsumers
   *   The maximum number of concurrent consumers.
   */
  public void setMaxConcurrentConsumers(final int maxConcurrentConsumers) {
    this.maxConcurrentConsumers = maxConcurrentConsumers;
  }

  /**
   * Get whether the JMS consumers are asynchronous.
   * @return
   *   true if consumers are asynchronous, false otherwise.
   */
  public boolean isAsyncConsumer() {
    return asyncConsumer;
  }

  /**
   * Set whether the JMS consumers are asynchronous.
   * @param asyncConsumer
   *   true if consumers should be asynchronous, false otherwise.
   */
  public void setAsyncConsumer(final boolean asyncConsumer) {
    this.asyncConsumer = asyncConsumer;
  }

  /**
   * @return the jms index stream endpoint.
   */
  public String getJmsIndexStream() {
    // Prepend the current broker name
    return addConcurrent(JMS_ENDPOINT_NAME + ":" + indexStream);
  }

  /**
   * @return the jms delete stream endpoint.
   */
  public String getJmsDeleteStream() {
    // Prepend the current broker name
    return addConcurrent(JMS_ENDPOINT_NAME + ":" + deleteStream);
  }

  /**
   * Utility to avoid passing variables each time.
   * @param queueString
   *   The topic/queue string to alter.
   * @return
   *   The altered topic/queue string.
   */
  private String addConcurrent(final String queueString) {
    return addJmsOptions(queueString, concurrentConsumers, maxConcurrentConsumers, asyncConsumer);
  }
}
