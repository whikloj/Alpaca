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

import org.springframework.boot.context.properties.ConfigurationProperties;

import static ca.islandora.alpaca.config.AlpacaConfig.JMS_ENDPOINT_NAME;
import static ca.islandora.alpaca.config.AlpacaConfig.addJmsOptions;

/**
 * Fcrepo indexer configuration class.
 * @author whikloj
 */
@ConfigurationProperties(prefix = "fcrepo.indexer")
public class FcrepoIndexerOptions {
  /**
   * Comes from fcrepo.indexer.enabled property
   */
  private boolean enabled = false;
  /**
   * Comes from fcrepo.indexer.node property
   */
  private String node = "";
  /**
   * Comes from fcrepo.indexer.delete property
   */
  private String delete = "";
  /**
   * Comes from fcrepo.indexer.external property
   */
  private String external = "";
  /**
   * Comes from fcrepo.indexer.media property
   */
  private String media = "";
  /**
   * Comes from fcrepo.indexer.milliner-baseUrl property
   */
  private String millinerBaseUrl = "";
  /**
   * Comes from fcrepo.indexer.fedoraHeader property
   */
  private String fedoraHeader = "X-Islandora-Fedora-Endpoint";
  /**
   * Comes from fcrepo.indexer.concurrent-consumers property
   */
  private int concurrentConsumers = -1;
  /**
   * Comes from fcrepo.indexer.max-concurrent-consumers property
   */
  private int maxConcurrentConsumers = -1;
  /**
   * Comes from fcrepo.indexer.async-consumer property
   */
  private boolean asyncConsumer = false;

  /**
   * Enable or disable the Fcrepo indexer
   * @param value
   *  true if enabled, false otherwise
   */
  public void setEnabled(final boolean value) {
    this.enabled = value;
  }

  /**
   * Check if the Fcrepo indexer is enabled.
   * @return
   *  true if enabled, false otherwise
   */
  public boolean isEnabled() {
    return enabled;
  }

  /**
   * Set the node index endpoint.
   * @param value
   *  The node index endpoint.
   */
  public void setNode(final String value) {
    this.node = value;
  }

  /**
   * Set the node delete endpoint.
   * @param value
   *  The node delete endpoint.
   */
  public void setDelete(final String value) {
    this.delete = value;
  }

  /**
   * Set the external content index endpoint.
   * @param value
   *  The external content index endpoint.
   */
  public void setExternal(final String value) {
    this.external = value;
  }

  /**
   * Set the media index endpoint.
   * @param value
   *  The media index endpoint.
   */
  public void setMedia(final String value) {
    this.media = value;
  }

  /**
   * Set the milliner base URL.
   * @param value
   *  The milliner base URL.
   */
  public void setMillinerBaseUrl(final String value) {
    this.millinerBaseUrl = value;
  }

  /**
   * Set the header containing the fedora base uri.
   * @param value
   *  The header name.
   */
  public void setFedoraHeader(final String value) {
    this.fedoraHeader = value;
  }

  /**
   * Set the number of concurrent consumers.
   * @param value
   *  The number of concurrent consumers.
   */
  public void setConcurrentConsumers(final int value) {
    this.concurrentConsumers = value;
  }

  /**
   * Set the maximum number of concurrent consumers.
   * @param value
   *  The maximum number of concurrent consumers.
   */
  public void setMaxConcurrentConsumers(final int value) {
    this.maxConcurrentConsumers = value;
  }

  /**
   * Set whether to use async consumers.
   * @param value
   *  true if async consumers should be used, false otherwise.
   */
  public void setAsyncConsumer(final boolean value) {
    this.asyncConsumer = value;
  }

  /**
   * Get the Node index endpoint.
   * @return the endpoint.
   */
  public String getNodeIndex() {
    return addConcurrent(JMS_ENDPOINT_NAME + ":" + node);
  }

  /**
   * Get the Node delete endpoint.
   * @return the endpoint.
   */
  public String getNodeDelete() {
    return addConcurrent(JMS_ENDPOINT_NAME + ":" + delete);
  }

  /**
   * Get the media index endpoint.
   * @return the endpoint.
   */
  public String getMediaIndex() {
    return addConcurrent(JMS_ENDPOINT_NAME + ":" + media);
  }

  /**
   * Get the external content index endpoint.
   * @return the endpoint.
   */
  public String getExternalIndex() {
    return addConcurrent(JMS_ENDPOINT_NAME + ":" + external);
  }

  /**
   * Get the number of concurrent consumers.
   * @return
   *  The number of concurrent consumers.
   */
  public int getConcurrentConsumers() {
    return concurrentConsumers;
  }

  /**
   * Get the maximum number of concurrent consumers.
   * @return
   *  The maximum number of concurrent consumers.
   */
  public int getMaxConcurrentConsumers() {
    return maxConcurrentConsumers;
  }

  /**
   * Check if async consumers are enabled.
   * @return
   *  true if async consumers are enabled, false otherwise.
   */
  public boolean isAsyncConsumers() {
    return asyncConsumer;
  }

  /**
   * Utility to avoid passing variables each time.
   * @param queueString
   *   The topic/queue string to alter.
   * @return
   *   The altered topic/queue string.
   */
  private String addConcurrent(final String queueString) {
    return addJmsOptions(queueString, getConcurrentConsumers(), getMaxConcurrentConsumers(), isAsyncConsumers());
  }

  /**
   * Get the Milliner base url
   * @return the url.
   */
  public String getMillinerBaseUrl() {
    return millinerBaseUrl;
  }

  /**
   * Get the header which contains the fedora base uri.
   * @return the header
   */
  public String getFedoraUriHeader() {
    return fedoraHeader;
  }

}
