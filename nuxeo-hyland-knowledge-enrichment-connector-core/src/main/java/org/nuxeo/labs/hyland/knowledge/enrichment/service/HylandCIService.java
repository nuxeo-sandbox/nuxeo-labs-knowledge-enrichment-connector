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

public interface HylandCIService {

    // Kind of Low-level calls
    public String invokeEnrichment(String httpMethod, String endpoint, String jsonPayload);

    public String invokeEnrichment(String httpMethod, String endpoint, String jsonPayload, boolean useCache);

    // High level, does all the call chain for you (get token, get presigned URL, etc. etc.)
    public String enrich(Blob blob, List<String> actions, List<String> classes, List<String> similarMetadata)
            throws IOException;

    public String enrich(File file, String mimeType, List<String> actions, List<String> classes, List<String> similarMetadata)
            throws IOException;


    // ====================================================================================================
    /*
     * Used when CIC provided APIs for quick demos, showing work in progress
     * Not to be used, these APIs and the server will be removed/shutdown at some point.
     */
    public static final String CONTENT_INTELL_URL_PARAM = "nuxeo.hyland.content.intelligence.baseUrl";

    public static final String CONTENT_INTELL_HEADER_NAME_PARAM = "nuxeo.hyland.content.intelligence.authenticationHeaderName";

    public static final String CONTENT_INTELL_HEADER_VALUE_PARAM = "nuxeo.hyland.content.intelligence.authenticationHeaderValue";

    public String invokeObsoleteQuickDemo(String endpoint, String jsonPayload);

    public String invokeObsoleteQuickDemo(String endpoint, String jsonPayload, boolean useCache);
    // ====================================================================================================
}
