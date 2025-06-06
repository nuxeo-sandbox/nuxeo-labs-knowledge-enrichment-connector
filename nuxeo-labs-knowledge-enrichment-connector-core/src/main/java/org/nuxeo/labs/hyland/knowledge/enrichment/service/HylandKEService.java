/*
 * (C) Copyright 2025 Hyland (http://hyland.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Thibaud Arguillere
 */
package org.nuxeo.labs.hyland.knowledge.enrichment.service;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.labs.knowledge.enrichment.http.ServiceCallResult;

@SuppressWarnings("rawtypes")
public interface HylandKEService {

    /**
     * Send the blob for enrichment. In the response, and if succesful, there will be the job ID to use with
     * getJobIdResult(). Also, <copde>sourceid</sourceId> is optional. I is returned in the result (see 
     * ServiceCallResult#objectKeysMapping) and let the caller bind the job ID with the input blob.
     * If not passed, a random UUID is generated.
     * <br>
     * For the values to pass in <code>actions</code>, <code>classes</code> and <code>similarmetadata</code>, see
     * the service documentation at
     * {@link https://hyland.github.io/ContentIntelligence-Docs/KnowledgeEnrichment/ContextEnrichmentAPI}
     * 
     * @param blob
     * @param sourceId
     * @param actions
     * @param classes
     * @param similarMetadataJsonArrayStr
     * @param extraJsonPayloadStr
     * @return
     * @throws IOException
     * @since TODO
     */
    public ServiceCallResult sendForEnrichment(Blob blob, String sourceId, List<String> actions, List<String> classes,
            String similarMetadataJsonArrayStr, String extraJsonPayloadStr) throws IOException;

    /**
     * Send the file for enrichment. In the response, and if succesful, there will be the job ID to use with
     * getJobIdResult(). Also, <copde>sourceid</sourceId> is optional. I is returned in the result (see 
     * ServiceCallResult#objectKeysMapping) and let the caller bind the job ID with the input blob.
     * If not passed, a random UUID is generated.
     * <br>
     * For the values to pass in <code>actions</code>, <code>classes</code> and <code>similarmetadata</code>, see
     * the service documentation at
     * {@link https://hyland.github.io/ContentIntelligence-Docs/KnowledgeEnrichment/ContextEnrichmentAPI}
     * 
     * @param file
     * @param sourceId
     * @param mimeType. If null or "", it will be calculated (can take time)
     * @param actions
     * @param classes
     * @param similarMetadataJsonArrayStr
     * @param extraJsonPayloadStr
     * @return
     * @throws IOException
     * @since TODO
     */
    public ServiceCallResult sendForEnrichment(File file, String sourceId, String mimeType, List<String> actions,
            List<String> classes, String similarMetadataJsonArrayStr, String extraJsonPayloadStr) throws IOException;

    /**
     * Send a list of blobs for enrichment. In the response, and if succesful, there will be the job ID to use with
     * getJobIdResult()
     * 
     * @param contentObjects
     * @param actions
     * @param classes
     * @param similarMetadataJsonArrayStr
     * @param extraJsonPayloadStr
     * @return
     * @throws IOException
     * @since TODO
     */
    public ServiceCallResult sendForEnrichment(List<ContentToProcess> contentObjects, List<String> actions,
            List<String> classes, String similarMetadataJsonArrayStr, String extraJsonPayloadStr) throws IOException;

    /**
     * After calling one of the sendForEnrichment() method, pull the results with getJobIdResult().
     * The HTTP response mayb not be 200. it could be for example 202, "accepted"
     * 
     * @param jobId
     * @return
     * @throws IOException
     * @since TODO
     */
    public ServiceCallResult getJobIdResult(String jobId) throws IOException;

    /**
     * High level call performing all the different serial requests to the service (authenticate, then ask for presigned
     * url, then send the file, etc.)
     * <br>
     * For the values to pass in <code>actions</code>, <code>classes</code> and <code>similarmetadata</code>, see
     * the service documentation at
     * {@link https://hyland.github.io/ContentIntelligence-Docs/KnowledgeEnrichment/ContextEnrichmentAPI}
     * 
     * @param blob
     * @param actions
     * @param classes
     * @param similarMetadata
     * @return a ServiceCallResult
     * @throws IOException
     * @since 2023
     */
    public ServiceCallResult enrich(Blob blob, List<String> actions, List<String> classes,
            String similarMetadataJsonArrayStr, String extraJsonPayloadStr) throws IOException;

    /**
     * See method
     * <code>enrich(Blob blob, List<String> actions, List<String> classes, List<String> similarMetadata)</code>
     * 
     * @param file
     * @param mimeType. If null or "", it will be calculated (can take time)
     * @param actions
     * @param classes
     * @param similarMetadata
     * @return a ServiceCallResult
     * @throws IOException
     * @since 2023
     */
    public ServiceCallResult enrich(File file, String mimeType, List<String> actions, List<String> classes,
            String similarMetadataJsonArrayStr, String extraJsonPayloadStr) throws IOException;

    /**
     * Enrich a list of blobs
     * 
     * @param blobs
     * @param actions
     * @param classes
     * @param similarMetadataJsonArrayStr
     * @return
     * @throws IOException
     * @since TODO
     */
    public ServiceCallResult enrich(List<ContentToProcess> contentObjects, List<String> actions, List<String> classes,
            String similarMetadataJsonArrayStr, String extraJsonPayloadStr) throws IOException;

    /**
     * Enrich a list of File
     * TO BE IMPROVED: The caller has no way to link the results to the input files
     * 
     * @param blobs
     * @param actions
     * @param classes
     * @param similarMetadataJsonArrayStr
     * @return
     * @throws IOException
     * @since TODO
     */
    /*
     * public ServiceCallResult enrich(List<ContentToProcess> files, List<String> actions, List<String> classes,
     * String similarMetadataJsonArrayStr) throws IOException;
     */
    /**
     * Call the KE service, using the configuration parameters (clientId, clientSecret, endpoints, …). This is a kind of
     * "low-level" call to the service.
     * <br>
     * The method handles the authentication token and its expiration time.
     * <br>
     * <code>jsonPayload</code> may be null, and its content depends on the <code>endpoint</code> used.
     * <br>
     * <code>endpoint</code> are documented here:
     * {@link https://hyland.github.io/ContentIntelligence-Docs/KnowledgeEnrichment/ContextEnrichmentAPI}
     * 
     * @param httpMethod
     * @param endpoint
     * @param jsonPayload
     * @return a ServiceCallResult
     * @since 2023
     */
    public ServiceCallResult invokeEnrichment(String httpMethod, String endpoint, String jsonPayload);

    /**
     * Call the DataCuraiton APi and returns
     * Implementation should provide default values if jsonOptions is null or "".
     * For possible values, please see the service documentation at
     * {@link https://hyland.github.io/ContentIntelligence-Docs/KnowledgeEnrichment/DataCurationAPI}
     * * <br>
     * The returned String is JSON String with 3 fields:
     * {
     * "responseCode": The HTTP response of the service when performoing the call. Should be a succesful range (200-299)
     * "responseMessage": The HTTP response message (like "OK")
     * "response": The response (as JSON string from the service
     * }
     * 
     * @param blob
     * @param jsonOptions
     * @return a ServiceCallResult
     * @throws IOException
     * @since 2023
     */
    public ServiceCallResult curate(Blob blob, String jsonOptions) throws IOException;

    /**
     * (see <code>curate(Blob blob, String jsonOptions)</code>
     * 
     * @param file
     * @param jsonOptions
     * @return
     * @throws IOException
     * @since 2023
     */
    public ServiceCallResult curate(File file, String jsonOptions) throws IOException;

    /**
     * maxRetries and sleepinterval are configuration parametere, they can be tuned with this method.
     * <br>
     * The value is used for all and every calls until they change again.
     * <br>
     * Special values:
     * <ul>
     * <li>0: Revert to configuration parameter values. If not set, revert to default value.</li>
     * <li>-1: Do not change the value</li>
     * </ul>
     * 
     * @param maxTries
     * @param sleepIntervalMS
     * @since 2023
     */
    public void setPullResultsSettings(int maxTries, int sleepIntervalMS);

    // ====================================================================================================
    /*
     * Used when CIC provided APIs for quick demos, showing work in progress
     * Not to be used, these APIs and the server will be removed/shutdown at some point.
     */
    public static final String CONTENT_INTELL_URL_PARAM = "nuxeo.hyland.content.intelligence.baseUrl";

    public static final String CONTENT_INTELL_HEADER_NAME_PARAM = "nuxeo.hyland.content.intelligence.authenticationHeaderName";

    public static final String CONTENT_INTELL_HEADER_VALUE_PARAM = "nuxeo.hyland.content.intelligence.authenticationHeaderValue";

    public String invokeObsoleteQuickDemo(String endpoint, String jsonPayload);
    // ====================================================================================================
}
