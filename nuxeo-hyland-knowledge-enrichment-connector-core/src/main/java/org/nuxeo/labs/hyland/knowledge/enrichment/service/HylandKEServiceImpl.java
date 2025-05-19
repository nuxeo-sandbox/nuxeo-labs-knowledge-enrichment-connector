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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CloseableFile;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeRegistry;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.DefaultComponent;

/*
 * A reminder of the functionning of Knowledge Enrichment (https://hyland.github.io/ContentIntelligence-Docs/KnowledgeEnrichment/ContextEnrichmentAPI/gettingstarted)
 *   1. Get Auth token
 *   2. Get presigned URL
 *   3. Uplaod file to this URL
 *   4. Get available actions
 *   5. Process
 *   6. Get results (loop to check when done)
 * 
 */
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

    public static final String PULL_RESULTS_SLEEP_INTERVALL_PARAM = "nuxeo.hyland.cic.pullResultsSleepIntervall";

    public static final int PULL_RESULTS_SLEEP_INTERVALL_DEFAULT = 3000;

    public static final String DATA_CURATION_PRESIGN_DEFAULT_OPTIONS = "{\"normalization\": {\"quotations\": true},\"chunking\": true,\"embedding\": true}";

    public static String enrichmentClientId = null;

    public static String enrichmentClientSecret = null;

    public static String dataCurationClientId = null;

    public static String dataCurationClientSecret = null;

    public static String authEndPoint = null;

    public static String contextEnrichmentEndPoint = null;

    public static String dataCurationEndPoint = null;

    public static final String CONTENT_INTELL_CACHE = "content_intelligence_cache";

    protected static String enrichmentAuthToken = null;

    protected static Instant enrichmentTokenExpiration = null;

    protected static String dataCurationAuthToken = null;

    protected static Instant dataCurationTokenExpiration = null;

    protected static int pullResultsMaxTries;

    protected static int pullResultsSleepIntervall;

    public enum CICService {
        ENRICHMENT, DATA_CURATION
    }

    public HylandKEServiceImpl() {
        initialize();
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
        String param = Framework.getProperty(PULL_RESULTS_MAX_TRIES_PARAM);
        if (StringUtils.isBlank(param)) {
            param = "" + PULL_RESULTS_MAX_TRIES_DEFAULT;
        }
        pullResultsMaxTries = Integer.parseInt(param);

        param = Framework.getProperty(PULL_RESULTS_SLEEP_INTERVALL_PARAM);
        if (StringUtils.isBlank(param)) {
            param = "" + PULL_RESULTS_SLEEP_INTERVALL_DEFAULT;
        }
        pullResultsSleepIntervall = Integer.parseInt(param);

    }

    protected String fetchAuthTokenIfNeeded(CICService service) {

        // TODO
        // Use a synchronize to make sure 2 simultaneous calls stay OK

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

        HttpURLConnection connection = null;
        try {
            URL url = new URL(targetUrl);

            connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("POST");
            connection.setRequestProperty("Accept", "*/*");
            connection.setRequestProperty("Accept-Encoding", "gzip, deflate, br");
            // Not JSON...
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setDoOutput(true);

            // Write request body
            String postData = "client_id=" + URLEncoder.encode(clientId, "UTF-8") + "&client_secret="
                    + URLEncoder.encode(clientSecret, "UTF-8") + "&grant_type=client_credentials"
                    + "&scope=environment_authorization";

            try (OutputStream os = connection.getOutputStream()) {
                os.write(postData.getBytes("UTF-8"));
            }

            // Get response code
            int status = connection.getResponseCode();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    (status >= 200 && status < 300) ? connection.getInputStream() : connection.getErrorStream(),
                    StandardCharsets.UTF_8))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                JSONObject responseJson = new JSONObject(response.toString());
                // {"error":"invalid_grant","error_description":"Caller not authorized for requested resource"}
                if (responseJson.has("error")) {
                    String msg = "Getting a token failed with error " + responseJson.getString("error") + ".";
                    if (responseJson.has("error_description")) {
                        msg += " " + responseJson.getString("error_description");
                    }
                    log.error(msg);
                } else {
                    int expiresIn = responseJson.getInt("expires_in");
                    switch (service) {
                    case ENRICHMENT:
                        enrichmentAuthToken = responseJson.getString("access_token");
                        enrichmentTokenExpiration = Instant.now().plusSeconds(expiresIn - 15);
                        break;

                    case DATA_CURATION:
                        dataCurationAuthToken = responseJson.getString("access_token");
                        dataCurationTokenExpiration = Instant.now().plusSeconds(expiresIn - 15);
                        break;
                    }
                    // should we get "expires_in"?
                }
            }

        } catch (IOException e) {
            throw new NuxeoException(e);
        } finally {
            if (connection != null) {
                connection.disconnect();
                connection = null;
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

    /*
     * 1. Get Auth token
     * 2. Get presigned URL
     * 3. Upload file to this URL
     * 4. OPT: Get available actions
     * 5. Process
     * 6. Pull results
     */
    public String enrich(Blob blob, List<String> actions, List<String> classes, List<String> similarMetadata)
            throws IOException {

        String mimeType = blob.getMimeType();
        if (StringUtils.isBlank(mimeType)) {
            // This can happen when Blob is built manually
            MimetypeRegistry registry = Framework.getService(MimetypeRegistry.class);
            mimeType = registry.getMimetypeFromBlob(blob);
        }

        try (CloseableFile closFile = blob.getCloseableFile()) {
            return enrich(closFile.getFile(), mimeType, actions, classes, similarMetadata);
        }

    }

    public String enrich(File file, String mimeType, List<String> actions, List<String> classes,
            List<String> similarMetadata) throws IOException {

        String result;
        JSONObject resultJson;
        JSONObject anObject;
        int responseCode;

        if (StringUtils.isBlank(mimeType)) {
            MimetypeRegistry registry = Framework.getService(MimetypeRegistry.class);
            mimeType = registry.getMimetypeFromFile(file);
        }

        // (1. Token will be handled at first call)

        // 2. Get presigned URL
        result = invokeEnrichment("GET", "/api/files/upload/presigned-url?contentType=" + mimeType.replace("/", "%2F"),
                null);
        resultJson = new JSONObject(result);
        responseCode = resultJson.getInt("responseCode");
        if (responseCode != 200) {
            return result;
        }

        anObject = resultJson.getJSONObject("response");
        String presignedUrl = anObject.getString("presignedUrl");
        String objectKey = anObject.getString("objectKey");

        // 3. Upload file to this URL
        responseCode = uploadFileWithPut(file, presignedUrl, mimeType); // uploadFile(file, presignedUrl, mimeType);
        if (responseCode != 200) {
            return buildJsonResponseString("{}", responseCode, "");
        }

        // 4. Get available actions
        // NOt checked here

        // 5. Process
        JSONObject payload = new JSONObject();
        payload.put("objectKeys", new JSONArray("[\"" + objectKey + "\"]"));
        payload.put("actions", new JSONArray(actions));
        if (similarMetadata == null) {
            payload.put("kSimilarMetadata", new JSONArray());
        } else {
            payload.put("classes", new JSONArray(similarMetadata));
        }
        if (classes == null) {
            payload.put("classes", new JSONArray());
        } else {
            payload.put("classes", new JSONArray(classes));
        }

        result = invokeEnrichment("POST", "/api/content/process", payload.toString());
        resultJson = new JSONObject(result);
        responseCode = resultJson.getInt("responseCode");
        if (responseCode != 200) {
            return result;
        }
        String resultId = resultJson.getString("response");

        // 6. Get results (loop to check when done)
        result = pullEnrichmentResults(resultId);

        return result;
    }

    /*
     * 1. Get Auth token
     * 2. Get presigned URLs (put and get)
     * 3. Upload file to this URL
     * 4. Pull results
     * jsonOptions => see API documentation. As of May 2025:
     * {
     * "normalization": {
     * "quotations": true
     * },
     * "chunking": true,
     * "embedding": true
     * }
     */
    public String curate(Blob blob, String jsonOptions) throws IOException {

        try (CloseableFile closFile = blob.getCloseableFile()) {
            return curate(closFile.getFile(), jsonOptions);
        }
    }

    public String curate(File file, String jsonOptions) throws IOException {

        String result;
        JSONObject jsonPresign;
        String jobId = null;
        String getUrl = null;
        String putUrl = null;
        int responseCode;

        // ====================> 1. Get auth token
        String bearer = fetchAuthTokenIfNeeded(CICService.DATA_CURATION);
        if (StringUtils.isBlank(bearer)) {
            throw new NuxeoException("No authentication info for calling the Data Curation service.");
        }

        // ====================> 2. Get presigned stuff
        String targetUrl = dataCurationEndPoint;
        targetUrl += "/api/presign";

        // Let's use the good old HttpURLConnection.
        HttpURLConnection conn = null;
        try {
            // Create the URL object
            URL url = new URL(targetUrl);
            conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("POST");
            conn.setRequestProperty("Accept", "*/*");
            conn.setRequestProperty("Authorization", "Bearer " + bearer);
            // conn.setRequestProperty("Content-Type", "application/json");

            conn.setDoOutput(true);
            if (StringUtils.isBlank(jsonOptions)) {
                jsonOptions = DATA_CURATION_PRESIGN_DEFAULT_OPTIONS;
            }
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonOptions.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            // Get response code
            responseCode = conn.getResponseCode();
            if (responseCode >= 200 && responseCode < 300) {
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    StringBuilder theResponse = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        theResponse.append(line.trim());
                    }
                    jsonPresign = new JSONObject(theResponse.toString());
                    jobId = jsonPresign.getString("job_id");
                    putUrl = jsonPresign.getString("put_url");
                    getUrl = jsonPresign.getString("get_url");
                }
            } else {
                result = buildJsonResponseString("{}", responseCode, conn.getResponseMessage());
                return result;
            }

        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        } finally {
            if (conn != null) {
                conn.disconnect();
                conn = null;
            }
        }

        // ====================> 3. Upload with PUT
        responseCode = uploadFileWithPut(file, putUrl, "application/octet-stream");
        if (responseCode != 200) {
            return buildJsonResponseString("{}", responseCode, "");
        }

        // ====================> 4. Pull results
        result = pullDataCurationResults(jobId, getUrl);

        return result;

    }

    protected String pullEnrichmentResults(String resultId) {

        String result;
        JSONObject resultJson;
        int responseCode;
        int count = 1;

        do {
            if (count > 1) {
                try {
                    Thread.sleep(pullResultsSleepIntervall);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if (count > 5) {
                log.warn("Pulling Enrichment results is taking time. This is the call #" + count + " (max calls: "
                        + pullResultsMaxTries + ")");
            }
            result = invokeEnrichment("GET", "/api/content/process/" + resultId + "/results", null);
            resultJson = new JSONObject(result);
            responseCode = resultJson.getInt("responseCode");
            count += 1;
        } while (responseCode != 200 || count >= pullResultsMaxTries);

        return result;
    }

    /*
     * Pull to dataCurationEndPoint/status/job_id until getting it "Done"
     * Once "Done", just GET at the getUrl (presigned)
     */
    protected String pullDataCurationResults(String jobId, String getUrl) {

        String result = null;
        String status;
        int count = 1;
        JSONObject resultJson;

        if (StringUtils.isBlank(jobId) || StringUtils.isBlank(getUrl)) {
            throw new IllegalArgumentException("jobId and/or getUrl - presigned - is/are null");
        }

        int responseCode = 0;
        boolean gotIt = false;
        do {
            if (count > 1) {
                try {
                    Thread.sleep(pullResultsSleepIntervall);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if (count > 5) {
                log.warn("Pulling Data Curation results is taking time. This is the call #" + count + " (max calls: "
                        + pullResultsMaxTries + ")");
            }

            String bearer = fetchAuthTokenIfNeeded(CICService.DATA_CURATION);
            if (StringUtils.isBlank(bearer)) {
                throw new NuxeoException("No authentication info for calling the Data Curation service.");
            }

            String targetUrl = dataCurationEndPoint + "/api/status/" + jobId;
            HttpURLConnection conn = null;
            try {
                URL url = new URL(targetUrl);
                conn = (HttpURLConnection) url.openConnection();

                conn.setRequestMethod("GET");
                // conn.setRequestProperty("Accept", "*/*");
                conn.setRequestProperty("Authorization", "Bearer " + bearer);

                // Get response code
                responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    try (BufferedReader br = new BufferedReader(
                            new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                        StringBuilder theResponse = new StringBuilder();
                        String line;
                        while ((line = br.readLine()) != null) {
                            theResponse.append(line.trim());
                        }

                        resultJson = new JSONObject(theResponse.toString());

                        // error-proof/error-check that the job ID is the correct one
                        String responseJobId = resultJson.getString("jobId");
                        if (!responseJobId.equals(jobId)) {
                            log.warn("received OK for a different jobID... Pulling again");
                            responseCode = 0;
                        } else {
                            status = resultJson.getString("status");
                            
                            System.out.println("************ " + status);
                            
                            if(status.toLowerCase().equals("done")) {
                                // Just GET at the presigned URL
                                String sampleGetResponse = sampleGET(getUrl);
                                JSONObject responseObj = new JSONObject(sampleGetResponse);
                                if(responseObj.getInt("responseCode") == 200) {
                                    result = sampleGetResponse;
                                    gotIt = true;
                                }
                            }
                        }
                    }
                } else if (count >= pullResultsMaxTries) {
                    result = buildJsonResponseString("{}", responseCode, conn.getResponseMessage());
                    return result;
                }
                
            } catch (IOException e) {
                System.err.println("Error: " + e.getMessage());
            } finally {
                if (conn != null) {
                    conn.disconnect();
                    conn = null;
                }
            }

            count += 1;

        } while (!gotIt || count >= pullResultsMaxTries);

        return result;

    }

    public String invokeEnrichment(String httpMethod, String endpoint, String jsonPayload) {

        String response = null;

        httpMethod = httpMethod.toUpperCase();
        // Sanitycheck
        switch (httpMethod) {
        case "GET":
        case "POST":
        case "PUT":
            // OK;
            break;

        default:
            throw new NuxeoException("Only GET, POST and PU are supported.");
        }

        // Get auth token
        String bearer = fetchAuthTokenIfNeeded(CICService.ENRICHMENT);
        if (StringUtils.isBlank(bearer)) {
            throw new NuxeoException("No authentication info for calling the Enrichment service.");
        }

        // Get config parameter values for URL to call, authentication, etc.
        String targetUrl = contextEnrichmentEndPoint;

        if (!endpoint.startsWith("/")) {
            targetUrl += "/";
        }
        targetUrl += endpoint;

        // Let's use the good old HttpURLConnection.
        HttpURLConnection conn = null;
        try {
            // Create the URL object
            URL url = new URL(targetUrl);
            conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod(httpMethod);

            // Headers
            // conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "*/*");
            conn.setRequestProperty("Authorization", "Bearer " + bearer);
            if (endpoint.startsWith("/api/content/process")) {
                conn.setRequestProperty("Content-Type", "application/json");
            }

            // Body, if any.
            if ("POST".equals(httpMethod) || "PUT".equals(httpMethod)) {
                conn.setDoOutput(true);
                // Write JSON data to request body
                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }
            }

            // Get response code
            int responseCode = conn.getResponseCode();

            if (responseCode >= 200 && responseCode < 300) {
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    StringBuilder finalResponse = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        finalResponse.append(line.trim());
                    }
                    response = buildJsonResponseString(finalResponse.toString(), responseCode,
                            conn.getResponseMessage());
                }
            } else {
                response = buildJsonResponseString("{}", responseCode, conn.getResponseMessage());
            }

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

    protected String buildJsonResponseString(String response, int responseCode, String responseMessage) {
        String jsonResponseStr = "{";
        jsonResponseStr += "\"response\": " + response + ",";
        jsonResponseStr += "\"responseCode\": " + responseCode + ",";
        jsonResponseStr += "\"responseMessage\": \"" + (responseMessage == null ? "" : responseMessage) + "\"";
        jsonResponseStr += "}";

        return jsonResponseStr;
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

    /**
     * To upload the file to the presigned URL, we don't need the bearer token of course, and it
     * is a simple upload.
     * 
     * @param file
     * @param targetUrl
     * @throws IOException
     * @since TODO
     */
    public static int uploadFile(File file, String targetUrl, String contentType) throws IOException {
        int responseCode;

        HttpURLConnection conn = (HttpURLConnection) new URL(targetUrl).openConnection();
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", contentType);
        conn.setFixedLengthStreamingMode(file.length());

        try (OutputStream out = conn.getOutputStream(); InputStream in = new FileInputStream(file)) {

            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }

        responseCode = conn.getResponseCode();
        // System.out.println("Response Code: " + responseCode);

        // Documentaitons tates the call returns nothing.
        return responseCode;
        /*
         * try (BufferedReader br = new BufferedReader(new InputStreamReader(
         * responseCode >= 200 && responseCode < 300 ? conn.getInputStream() : conn.getErrorStream()))) {
         * String line;
         * while ((line = br.readLine()) != null) {
         * System.out.println(line);
         * }
         * }
         */
    }

    public static int uploadFileWithPut(File file, String targetUrl, String contentType) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(targetUrl).openConnection();
        conn.setDoOutput(true);
        conn.setRequestMethod("PUT");
        conn.setRequestProperty("Content-Type", contentType);
        conn.setFixedLengthStreamingMode(file.length());

        try (OutputStream out = conn.getOutputStream(); InputStream in = new FileInputStream(file)) {

            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }

        int responseCode = conn.getResponseCode();
        return responseCode;
    }
    
    public String sampleGET(String url) throws IOException {
        
        String result = null;
        int responseCode;
        HttpURLConnection conn = null;
        
        try {
            // Create the URL object
            URL theUrl = new URL(url);
            conn = (HttpURLConnection) theUrl.openConnection();

            conn.setRequestMethod("GET");
            // No headers required
            
            responseCode = conn.getResponseCode();
            if (responseCode >= 200 && responseCode < 300) {
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    StringBuilder finalResponse = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        finalResponse.append(line.trim());
                    }
                    result = buildJsonResponseString(finalResponse.toString(), responseCode,
                            conn.getResponseMessage());
                }
            } else {
                result = buildJsonResponseString("{}", responseCode, conn.getResponseMessage());
            }
            
            
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        } finally {
            if (conn != null) {
                conn.disconnect();
                conn = null;
            }
        }
        
        return result;
        
    }

}
