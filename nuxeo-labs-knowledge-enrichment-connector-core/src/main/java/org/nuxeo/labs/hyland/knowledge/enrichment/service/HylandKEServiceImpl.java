/*
i * (C) Copyright 2025 Hyland (http://hyland.com/) and others.
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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CloseableFile;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.labs.knowledge.enrichment.http.ServiceCall;
import org.nuxeo.labs.knowledge.enrichment.http.ServiceCallResult;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.DefaultComponent;

public class HylandKEServiceImpl extends DefaultComponent implements HylandKEService {

    private static final Logger log = LogManager.getLogger(HylandKEServiceImpl.class);

    public static final String ENRICHMENT_CLIENT_ID_PARAM = "nuxeo.hyland.cic.enrichment.clientId";

    public static final String ENRICHMENT_CLIENT_SECRET_PARAM = "nuxeo.hyland.cic.enrichment.clientSecret";

    public static final String DATA_CURATION_CLIENT_ID_PARAM = "nuxeo.hyland.cic.datacuration.clientId";

    public static final String DATA_CURATION_CLIENT_SECRET_PARAM = "nuxeo.hyland.cic.datacuration.clientSecret";

    // Will add "/connect/token" to this endpoint.
    public static final String ENDPOINT_AUTH_PARAM = "nuxeo.hyland.cic.endpoint.auth";

    public static final String ENDPOINT_CONTEXT_ENRICHMENT_PARAM = "nuxeo.hyland.cic.endpoint.contextEnrichment";

    public static final String ENDPOINT_DATA_CURATION_PARAM = "nuxeo.hyland.cic.endpoint.dataCuration";

    public static final String PULL_RESULTS_MAX_TRIES_PARAM = "nuxeo.hyland.cic.pullResultsMaxTries";

    public static final int PULL_RESULTS_MAX_TRIES_DEFAULT = 10;

    public static final String PULL_RESULTS_SLEEP_INTERVAL_PARAM = "nuxeo.hyland.cic.pullResultsSleepInterval";

    public static final int PULL_RESULTS_SLEEP_INTERVAL_DEFAULT = 3000;

    public static final String DATA_CURATION_PRESIGN_DEFAULT_OPTIONS = "{\"normalization\": {\"quotations\": true},\"chunking\": true,\"embedding\": true,\"json_schema\": \"PIPELINE\"}";

    protected static String enrichmentClientId = null;

    protected static String enrichmentClientSecret = null;

    protected static String dataCurationClientId = null;

    protected static String dataCurationClientSecret = null;

    protected static String authEndPoint = null;

    protected static String contextEnrichmentEndPoint = null;

    protected static String dataCurationEndPoint = null;

    public static final String CONTENT_INTELL_CACHE = "content_intelligence_cache";

    protected static String enrichmentAuthToken = null;

    protected static Instant enrichmentTokenExpiration = null;

    protected static String dataCurationAuthToken = null;

    protected static Instant dataCurationTokenExpiration = null;

    protected static int pullResultsMaxTries;

    protected static int pullResultsSleepIntervalMS;
    
    public static final String CUSTOM_ID_PREFIX = "CUSTOM_ID-";

    protected static ServiceCall serviceCall = new ServiceCall();

    public enum CICService {
        ENRICHMENT, DATA_CURATION
    }

    public HylandKEServiceImpl() {
        initialize();
    }

    public void setPullResultsSettings(int maxTries, int sleepIntervalMS) {

        switch (maxTries) {
        case 0:
            // Revert to config or default
            pullResultsMaxTries = configParamToInt(PULL_RESULTS_MAX_TRIES_PARAM, PULL_RESULTS_MAX_TRIES_DEFAULT);
            break;

        case -1:
            // No change
            break;

        default:
            pullResultsMaxTries = maxTries;
            break;
        }

        switch (sleepIntervalMS) {
        case 0:
            pullResultsSleepIntervalMS = configParamToInt(PULL_RESULTS_SLEEP_INTERVAL_PARAM,
                    PULL_RESULTS_SLEEP_INTERVAL_DEFAULT);
            break;

        case -1:
            // No change
            break;

        default:
            pullResultsSleepIntervalMS = sleepIntervalMS;
            break;
        }
    }

    public int getPullResultsMaxTries() {
        return pullResultsMaxTries;
    }

    public int getPullResultsSleepIntervalMS() {
        return pullResultsSleepIntervalMS;
    }
    
    protected String getCustomUUID() {
        String uuid = UUID.randomUUID().toString();
        return CUSTOM_ID_PREFIX + uuid.substring(CUSTOM_ID_PREFIX.length());
    }
    

    protected void initialize() {

        // ==========> Auth
        authEndPoint = Framework.getProperty(ENDPOINT_AUTH_PARAM);

        // ==========> EndPoints
        contextEnrichmentEndPoint = Framework.getProperty(ENDPOINT_CONTEXT_ENRICHMENT_PARAM);
        dataCurationEndPoint = Framework.getProperty(ENDPOINT_DATA_CURATION_PARAM);

        // ==========> Clients
        enrichmentClientId = Framework.getProperty(ENRICHMENT_CLIENT_ID_PARAM);
        enrichmentClientSecret = Framework.getProperty(ENRICHMENT_CLIENT_SECRET_PARAM);
        dataCurationClientId = Framework.getProperty(DATA_CURATION_CLIENT_ID_PARAM);
        dataCurationClientSecret = Framework.getProperty(DATA_CURATION_CLIENT_SECRET_PARAM);

        // ==========> SanityCheck
        if (StringUtils.isBlank(authEndPoint)) {
            log.warn("No CIC Authentication endpoint provided (" + ENDPOINT_AUTH_PARAM
                    + "), calls to the service will fail.");
        }

        if (StringUtils.isBlank(contextEnrichmentEndPoint)) {
            log.warn("No CIC Context Enrichment endpoint provided (" + ENDPOINT_CONTEXT_ENRICHMENT_PARAM
                    + "), calls to the service will fail.");
        }

        if (StringUtils.isBlank(dataCurationEndPoint)) {
            log.warn("No CIC Data Curation endpoint provided (" + ENDPOINT_DATA_CURATION_PARAM
                    + "), calls to the service will fail.");
        }

        if (StringUtils.isBlank(enrichmentClientId)) {
            log.warn("No CIC Enrichment ClientId provided (" + ENRICHMENT_CLIENT_ID_PARAM
                    + "), calls to the service will fail.");
        }

        if (StringUtils.isBlank(enrichmentClientSecret)) {
            log.warn("No CIC Enrichment ClientSecret provided (" + ENRICHMENT_CLIENT_SECRET_PARAM
                    + "), calls to the service will fail.");
        }

        if (StringUtils.isBlank(dataCurationClientId)) {
            log.warn("No CIC Data Curation ClientId provided (" + DATA_CURATION_CLIENT_ID_PARAM
                    + "), calls to the service will fail.");
        }

        if (StringUtils.isBlank(dataCurationClientSecret)) {
            log.warn("No CIC Data Curation ClientSecret provided (" + DATA_CURATION_CLIENT_SECRET_PARAM
                    + "), calls to the service will fail.");
        }

        // ==========> Other params
        pullResultsMaxTries = configParamToInt(PULL_RESULTS_MAX_TRIES_PARAM, PULL_RESULTS_MAX_TRIES_DEFAULT);
        pullResultsSleepIntervalMS = configParamToInt(PULL_RESULTS_SLEEP_INTERVAL_PARAM,
                PULL_RESULTS_SLEEP_INTERVAL_DEFAULT);
    }

    protected int configParamToInt(String param, int defaultValue) {

        int value;

        String paramValue = Framework.getProperty(param, "" + defaultValue);
        try {
            value = Integer.parseInt(paramValue);
        } catch (NumberFormatException e) {
            log.error(param + " is not a valid integer. Using default value");
            value = PULL_RESULTS_SLEEP_INTERVAL_DEFAULT;
        }

        return value;
    }

    protected String fetchAuthTokenIfNeeded(CICService service) {

        // TODO
        // Use a synchronize to make sure 2 simultaneous calls stay OK?

        String clientId, clientSecret;

        switch (service) {
        case ENRICHMENT:
            if (StringUtils.isNotBlank(enrichmentAuthToken) && !Instant.now().isAfter(enrichmentTokenExpiration)) {
                return enrichmentAuthToken;
            }
            clientId = enrichmentClientId;
            clientSecret = enrichmentClientSecret;
            break;

        case DATA_CURATION:
            if (StringUtils.isNotBlank(dataCurationAuthToken) && !Instant.now().isAfter(dataCurationTokenExpiration)) {
                return dataCurationAuthToken;
            }
            clientId = dataCurationClientId;
            clientSecret = dataCurationClientSecret;
            break;

        default:
            throw new IllegalArgumentException("Unknown service: " + service);
        }
        String targetUrl = authEndPoint + "/connect/token";

        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Accept", "*/*");
        headers.put("Accept-Encoding", "gzip, deflate, br");
        // Not JSON...
        headers.put("Content-Type", "application/x-www-form-urlencoded");

        // Request body
        String postData;
        try {
            postData = "client_id=" + URLEncoder.encode(clientId, "UTF-8") + "&client_secret="
                    + URLEncoder.encode(clientSecret, "UTF-8") + "&grant_type=client_credentials"
                    + "&scope=environment_authorization";
        } catch (UnsupportedEncodingException e) {
            throw new NuxeoException("Failed to encode the request", e);
        }

        ServiceCallResult result = serviceCall.post(targetUrl, headers, postData);

        if (result.callWasSuccesful()) {
            JSONObject serviceResponse = result.getResponseAsJSONObject();
            // {"error":"invalid_grant","error_description":"Caller not authorized for requested resource"}
            if (serviceResponse.has("error")) {
                String msg = "Getting a token failed with error " + serviceResponse.getString("error") + ".";
                if (serviceResponse.has("error_description")) {
                    msg += " " + serviceResponse.getString("error_description");
                }
                log.error(msg);
            } else {
                int expiresIn = serviceResponse.getInt("expires_in");
                String token = serviceResponse.getString("access_token");
                switch (service) {
                case ENRICHMENT:
                    enrichmentAuthToken = token;
                    enrichmentTokenExpiration = Instant.now().plusSeconds(expiresIn - 15);
                    break;

                case DATA_CURATION:
                    dataCurationAuthToken = token;
                    dataCurationTokenExpiration = Instant.now().plusSeconds(expiresIn - 15);
                    break;
                }
            }
        } else {
            log.error("Error getting an auth token:\n" + result.toJsonString(2));
            switch (service) {
            case ENRICHMENT:
                enrichmentAuthToken = null;

            case DATA_CURATION:
                dataCurationAuthToken = null;
            }
        }

        switch (service) {
        case ENRICHMENT:
            return enrichmentAuthToken;

        case DATA_CURATION:
            return dataCurationAuthToken;
        }

        return null;
    }

    @Override
    public ServiceCallResult getJobIdResult(String jobId) {

        ServiceCallResult result = null;

        result = invokeEnrichment("GET", "/api/content/process/" + jobId + "/results", null);

        return result;
    }

    @Override
    public ServiceCallResult sendForEnrichment(Blob blob, String sourceId, List<String> actions, List<String> classes,
            String similarMetadataJsonArrayStr, String extraJsonPayloadStr) throws IOException {
        
        if(StringUtils.isBlank(sourceId)) {
            sourceId = getCustomUUID();
        }

        @SuppressWarnings("rawtypes")
        List<ContentToProcess> contentToProcess = new ArrayList<ContentToProcess>();
        ContentToProcess<Blob> oneContent = new ContentToProcess<Blob>(sourceId, blob);
        contentToProcess.add(oneContent);

        ServiceCallResult result = sendForEnrichment(contentToProcess, actions, classes, similarMetadataJsonArrayStr,
                extraJsonPayloadStr);

        return result;

    }

    @Override
    public ServiceCallResult sendForEnrichment(File file, String sourceId, String mimeType, List<String> actions, List<String> classes,
            String similarMetadataJsonArrayStr, String extraJsonPayloadStr) throws IOException {
        
        if(StringUtils.isBlank(sourceId)) {
            sourceId = getCustomUUID();
        }

        @SuppressWarnings("rawtypes")
        List<ContentToProcess> contentToProcess = new ArrayList<ContentToProcess>();
        ContentToProcess<File> oneContent = new ContentToProcess<File>(sourceId, file);
        contentToProcess.add(oneContent);

        ServiceCallResult result = sendForEnrichment(contentToProcess, actions, classes, similarMetadataJsonArrayStr,
                extraJsonPayloadStr);

        return result;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public ServiceCallResult sendForEnrichment(List<ContentToProcess> contentObjects, List<String> actions,
            List<String> classes, String similarMetadataJsonArrayStr, String extraJsonPayloadStr) throws IOException {

        ServiceCallResult result = null;
        JSONObject serviceResponse;

        // (1. Token will be handled at first call)

        // 2. Get presigned URL for every file
        String errMsg;
        for (ContentToProcess content : contentObjects) {

            result = invokeEnrichment("GET",
                    "/api/files/upload/presigned-url?contentType=" + content.getMimeType().replace("/", "%2F"), null);
            if (result.callFailed()) {
                // return result;

                errMsg = "Failed getting a presigned URL for content ID <" + content.getSourceId() + ">, File name <"
                        + content.getFile().getName() + ">.";
                log.error(errMsg);
                content.setErrorMessage(errMsg);
                content.setProcessingSuccess(false);
                continue;
            }

            serviceResponse = result.getResponseAsJSONObject();
            String presignedUrl = serviceResponse.getString("presignedUrl");
            String objectKey = serviceResponse.getString("objectKey");
            content.setObjectKey(objectKey);

            // 3. Upload file to this URL
            result = serviceCall.uploadFileWithPut(content.getFile(), presignedUrl, content.getMimeType());
            if (result.callFailed()) {
                // return result;

                errMsg = "Failed uploading content ID <" + content.getSourceId() + ">, File name <\"\n"
                        + content.getFile().getName() + ">.";
                log.error(errMsg);
                content.setErrorMessage(errMsg);
                content.setProcessingSuccess(false);
                continue;
            }

            content.setProcessingSuccess(true);

        }
        
        // We need to cleanup and close() any potential CloseableFile fetched during the loop
        for (ContentToProcess content : contentObjects) {
            content.close();
        }

        // 4. Get available actions
        // (Not needed here)

        // 5. Process
        List<String> objectKeys = contentObjects.stream()
                                                .filter(ContentToProcess::isProcessingSuccess)
                                                .map(ContentToProcess::getObjectKey)
                                                .collect(Collectors.toList());

        JSONObject payload = buildProcessActionPayload(objectKeys, actions, classes, similarMetadataJsonArrayStr,
                extraJsonPayloadStr);
        result = invokeEnrichment("POST", "/api/content/process", payload.toString());

        return result;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public ServiceCallResult enrich(List<ContentToProcess> contentObjects, List<String> actions, List<String> classes,
            String similarMetadataJsonArrayStr, String extraJsonPayloadStr) throws IOException {

        ServiceCallResult result = null;
        JSONObject serviceResponse;

        result = sendForEnrichment(contentObjects, actions, classes, similarMetadataJsonArrayStr, extraJsonPayloadStr);
        if (result.callFailed()) {
            return result;
        }
        serviceResponse = result.getResponseAsJSONObject();
        String resultId = serviceResponse.getString("processingId");

        result = pullEnrichmentResults(resultId);

        // Add the info so that caller can map objectKey and their blob/file
        if (result.callWasSuccesful()) {
            JSONObject response = result.getResponseAsJSONObject();
            JSONArray results = response.getJSONArray("results");
            JSONArray mapping = new JSONArray();
            results.forEach(oneResult -> {
                String objectKey = ((JSONObject) oneResult).getString("objectKey");
                ContentToProcess found = contentObjects.stream()
                                                       .filter(content -> objectKey.equals(content.getObjectKey()))
                                                       .findFirst()
                                                       .orElse(null);
                if (found != null) {
                    JSONObject obj = new JSONObject();
                    obj.put("sourceId", found.getSourceId());
                    obj.put("objectKey", objectKey);
                    mapping.put(obj);
                }
            });

            result.setObjectKeysMapping(mapping);
        }

        return result;
    }

    @Override
    public ServiceCallResult enrich(Blob blob, List<String> actions, List<String> classes,
            String similarMetadataJsonArrayStr, String extraJsonPayloadStr) throws IOException {
        
        String sourceId = getCustomUUID();

        @SuppressWarnings("rawtypes")
        List<ContentToProcess> contentToProcess = new ArrayList<ContentToProcess>();
        ContentToProcess<Blob> oneContent = new ContentToProcess<Blob>(sourceId, blob);
        contentToProcess.add(oneContent);

        ServiceCallResult result = enrich(contentToProcess, actions, classes, similarMetadataJsonArrayStr,
                extraJsonPayloadStr);

        return result;
    }

    protected JSONObject buildProcessActionPayload(List<String> objectKeys, List<String> actions, List<String> classes,
            String similarMetadataJsonArrayStr, String extraJsonPayloadStr) {

        JSONObject payload = new JSONObject();
        payload.put("objectKeys", new JSONArray(objectKeys));
        payload.put("actions", new JSONArray(actions));
        if (similarMetadataJsonArrayStr == null) {
            payload.put("kSimilarMetadata", new JSONArray());
        } else {
            payload.put("kSimilarMetadata", new JSONArray(similarMetadataJsonArrayStr));
        }
        if (classes == null) {
            payload.put("classes", new JSONArray());
        } else {
            payload.put("classes", new JSONArray(classes));
        }

        if (StringUtils.isNoneBlank(extraJsonPayloadStr)) {
            JSONObject extraJsonPayload = new JSONObject(extraJsonPayloadStr);
            for (String key : JSONObject.getNames(extraJsonPayload)) {
                payload.put(key, extraJsonPayload.get(key));
            }
        }

        return payload;
    }

    @Override
    public ServiceCallResult enrich(File file, String mimeType, List<String> actions, List<String> classes,
            String similarMetadataJsonArrayStr, String extraJsonPayloadStr) throws IOException {
        
        String sourceId = getCustomUUID();

        @SuppressWarnings("rawtypes")
        List<ContentToProcess> contentToProcess = new ArrayList<ContentToProcess>();
        ContentToProcess<File> oneContent = new ContentToProcess<File>(sourceId, file);
        contentToProcess.add(oneContent);

        ServiceCallResult result = enrich(contentToProcess, actions, classes, similarMetadataJsonArrayStr,
                extraJsonPayloadStr);

        return result;
    }

    @Override
    public ServiceCallResult curate(Blob blob, String jsonOptions) throws IOException {

        try (CloseableFile closFile = blob.getCloseableFile()) {
            return curate(closFile.getFile(), jsonOptions);
        }
    }

    @Override
    public ServiceCallResult curate(File file, String jsonOptions) throws IOException {

        ServiceCallResult result;
        JSONObject jsonPresign;
        String jobId = null;
        String getUrl = null;
        String putUrl = null;

        // ====================> 1. Get auth token
        String bearer = fetchAuthTokenIfNeeded(CICService.DATA_CURATION);
        if (StringUtils.isBlank(bearer)) {
            throw new NuxeoException("No authentication info for calling the Data Curation service.");
        }

        // ====================> 2. Get presigned stuff
        String targetUrl = dataCurationEndPoint;
        targetUrl += "/api/presign";

        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Accept", "*/*");
        headers.put("Authorization", "Bearer " + bearer);
        // headers.put("Content-Type", "application/json");

        if (StringUtils.isBlank(jsonOptions)) {
            jsonOptions = DATA_CURATION_PRESIGN_DEFAULT_OPTIONS;
        }

        result = serviceCall.post(targetUrl, headers, jsonOptions);
        if (result.callFailed()) {
            return result;
        }
        jsonPresign = result.getResponseAsJSONObject();
        jobId = jsonPresign.getString("job_id");
        putUrl = jsonPresign.getString("put_url");
        getUrl = jsonPresign.getString("get_url");

        // ====================> 3. Upload with PUT
        result = serviceCall.uploadFileWithPut(file, putUrl, "application/octet-stream");
        if (result.callFailed()) {
            return result;
        }

        // ====================> 4. Pull results
        result = pullDataCurationResults(jobId, getUrl);

        return result;

    }

    protected ServiceCallResult pullEnrichmentResults(String resultId) {

        ServiceCallResult result;
        int count = 1;

        do {
            if (count > 1) {
                try {
                    Thread.sleep(pullResultsSleepIntervalMS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            if (count > (pullResultsMaxTries / 2)) {
                log.warn("Pulling Enrichment results is taking time. This is the call #" + count + " (max calls: "
                        + pullResultsMaxTries + ")");
            }

            result = getJobIdResult(resultId);
            count += 1;

            // We must get an OK. A 202 "Accepted" for example does not have the full response.
        } while (!result.callResponseOK() && count <= pullResultsMaxTries);

        return result;
    }

    /*
     * Pull to dataCurationEndPoint/status/job_id until getting it "Done"
     * Once "Done", just GET at the getUrl (presigned)
     */
    protected ServiceCallResult pullDataCurationResults(String jobId, String getUrl) {

        ServiceCallResult result = null;
        int count = 1;

        if (StringUtils.isBlank(jobId) || StringUtils.isBlank(getUrl)) {
            throw new IllegalArgumentException("jobId and/or getUrl - presigned - is/are null");
        }

        String targetUrl = dataCurationEndPoint + "/api/status/" + jobId;
        boolean gotIt = false;
        do {
            if (count > 1) {
                try {
                    Thread.sleep(pullResultsSleepIntervalMS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if (count > (pullResultsMaxTries / 2)) {
                log.warn("Pulling Data Curation results is taking time. This is the call #" + count + " (max calls: "
                        + pullResultsMaxTries + ")");
            }

            String bearer = fetchAuthTokenIfNeeded(CICService.DATA_CURATION);
            if (StringUtils.isBlank(bearer)) {
                throw new NuxeoException("No authentication info for calling the Data Curation service.");
            }

            Map<String, String> headers = new HashMap<String, String>();
            headers.put("Authorization", "Bearer " + bearer);

            result = serviceCall.get(targetUrl, headers);
            if (result.callWasSuccesful()) {
                JSONObject resultJson = result.getResponseAsJSONObject();
                String responseJobId = resultJson.getString("jobId");
                if (!responseJobId.equals(jobId)) {
                    String msg = "Received OK for a different jobID. Exoected jobId: " + jobId + ", received: "
                            + responseJobId;
                    log.warn(msg);
                    // Not really a HTTP status, right?
                    // Nut this is needed woth the while(...) part.
                    result = new ServiceCallResult("{}", -2, msg);
                } else {
                    String status = resultJson.getString("status");
                    if (status.toLowerCase().equals("done")) {
                        // Just GET at the presigned URL, no headers required
                        result = serviceCall.get(getUrl, null);
                        if (result.callWasSuccesful()) {
                            gotIt = true;
                        }
                    } else {
                        log.info("Pulling Data Curation status for job " + jobId + ", status: " + status);
                    }
                }
            }

            count += 1;

        } while (!gotIt && count <= pullResultsMaxTries);

        return result;

    }

    public ServiceCallResult invokeEnrichment(String httpMethod, String endpoint, String jsonPayload) {

        ServiceCallResult result = null;

        // Get auth token
        String bearer = fetchAuthTokenIfNeeded(CICService.ENRICHMENT);
        if (StringUtils.isBlank(bearer)) {
            throw new NuxeoException("No authentication info for calling the Enrichment service.");
        }

        // URL/endpoint
        String targetUrl = contextEnrichmentEndPoint;
        if (!endpoint.startsWith("/")) {
            targetUrl += "/";
        }
        targetUrl += endpoint;

        // Headers
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Accept", "*/*");
        headers.put("Authorization", "Bearer " + bearer);
        if (endpoint.startsWith("/api/content/process")) {
            headers.put("Content-Type", "application/json");
        }

        // Run
        httpMethod = httpMethod.toUpperCase();
        switch (httpMethod) {
        case "GET":
            result = serviceCall.get(targetUrl, headers);
            break;

        case "POST":
            result = serviceCall.post(targetUrl, headers, jsonPayload);
            break;

        case "PUT":
            result = serviceCall.put(targetUrl, headers, jsonPayload);
            break;

        default:
            throw new NuxeoException("Only GET, POST and PUT are supported.");
        }

        return result;

    }

    // ================================================================================
    // ================================================================================
    // ================================================================================
    /*
     * Used when CIC provided APIs for quick demos, showing work in progress
     * Not to be used, these APIs and the server will be removed/shutdown at some point.
     */
    public String invokeObsoleteQuickDemo(String endpoint, String jsonPayload) {

        String response = null;

        // Get config parameter values for URL to call, authentication, etc.
        String targetUrl = Framework.getProperty(HylandKEService.CONTENT_INTELL_URL_PARAM);
        String authenticationHeaderName = Framework.getProperty(HylandKEService.CONTENT_INTELL_HEADER_NAME_PARAM);
        String authenticationHeaderValue = Framework.getProperty(HylandKEService.CONTENT_INTELL_HEADER_VALUE_PARAM);

        if (!endpoint.startsWith("/")) {
            targetUrl += "/";
        }
        targetUrl += endpoint;

        // For whatever reason I have don't time to explore, using the more modern java.net.http.HttpClient;
        // fails, the authentication header is not corrcetly received...
        // So, let's go back to good old HttpURLConnection.
        HttpURLConnection conn = null;
        try {
            // Create the URL object
            URL url = new URL(targetUrl);
            conn = (HttpURLConnection) url.openConnection();

            // Set request method to POST
            conn.setRequestMethod("POST");
            conn.setDoOutput(true); // Allows sending body content

            // Set headers
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty(authenticationHeaderName, authenticationHeaderValue); // Custom Auth Header

            // Write JSON data to request body
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            // Get response code
            int responseCode = conn.getResponseCode();

            // Read response
            try (BufferedReader br = new BufferedReader(new InputStreamReader(
                    responseCode >= 200 && responseCode < 300 ? conn.getInputStream() : conn.getErrorStream(),
                    StandardCharsets.UTF_8))) {

                StringBuilder finalResponse = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    finalResponse.append(line.trim());
                }

                response = finalResponse.toString();
                // System.out.println(response);

                try {
                    JSONObject responseJson = new JSONObject(response);
                    responseJson.put("responseCode", responseCode);
                    responseJson.put("responseMessage", conn.getResponseMessage());
                    response = responseJson.toString();
                } catch (JSONException e) {
                    // Ouch. This is not JSON, let it as it is
                }
            }

            // Disconnect the connection
            conn.disconnect();

        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        } finally {
            if (conn != null) {
                conn.disconnect();
                conn = null;
            }
        }

        return response;
    }

}
