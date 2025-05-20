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
 *     Michael Vachette
 *     Thibaud Arguillere
 */
package org.nuxeo.labs.hyland.knowledge.enrichment.service;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.labs.knowledge.enrichment.http.ServiceCallResult;

public interface HylandKEService {

    /**
     * High level call performing all the different serial requests to the service (authenticate, then ask for presigned
     * url, then send the file, etc.)
     * <br>
     * For the values for to pass to <code>actions</code>, <code>classes</code> and <code>similarmetadata</code>, see
     * the service documentation at
     * {@link https://hyland.github.io/ContentIntelligence-Docs/KnowledgeEnrichment/ContextEnrichmentAPI}
     * <br>
     * The returned String is JSON String with 3 fields:
     * {
     * "responseCode": The HTTP response of the service when performoing the call. Should be 200
     * "responseMessage": The HTTP response message (like "OK")
     * "response": The JSON response from the service, unchanged
     * }
     * 
     * @param blob
     * @param actions
     * @param classes
     * @param similarMetadata
     * @return
     * @throws IOException
     * @since TODO
     */
    public ServiceCallResult enrich(Blob blob, List<String> actions, List<String> classes, List<String> similarMetadata)
            throws IOException;

    /**
     * See method
     * <code>enrich(Blob blob, List<String> actions, List<String> classes, List<String> similarMetadata)</code>
     * 
     * @param file
     * @param mimeType
     * @param actions
     * @param classes
     * @param similarMetadata
     * @return
     * @throws IOException
     * @since TODO
     */
    public ServiceCallResult enrich(File file, String mimeType, List<String> actions, List<String> classes,
            List<String> similarMetadata) throws IOException;

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
     * @return
     * @since 2023
     */
    public ServiceCallResult invokeEnrichment(String httpMethod, String endpoint, String jsonPayload);

    /**
     * Call the DataCuraiton APi and returns
     * Implementation should provide default values is jsonOptions is null or "".
     * For possible values, please see the service documentation at
     * {@link https://hyland.github.io/ContentIntelligence-Docs/KnowledgeEnrichment/DataCurationAPI}
     * * <br>
     * The returned String is JSON String with 3 fields:
     * {
     * "responseCode": The HTTP response of the service when performoing the call. Should be 200
     * "responseMessage": The HTTP response message (like "OK")
     * "response": The JSON response from the service, unchanged
     * }
     * 
     * @param blob
     * @param jsonOptions
     * @return
     * @throws IOException
     * @since TODO
     */
    public ServiceCallResult curate(Blob blob, String jsonOptions) throws IOException;

    /**
     * (see <code>invokeEnrichment(String httpMethod, String endpoint, String jsonPayload)</code>
     * 
     * @param file
     * @param jsonOptions
     * @return
     * @throws IOException
     * @since TODO
     */
    public ServiceCallResult curate(File file, String jsonOptions) throws IOException;

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
