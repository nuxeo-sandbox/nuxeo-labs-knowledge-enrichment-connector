/*
 * (C) Copyright 2025 Hyland (http://hyland.com/)  and others.
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
package org.nuxeo.labs.hyland.knowledge.enrichment.http;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Utility class, centralizing the HTTP calls and returning a <code>ServiceCallResult</code>
 * 
 * @since 2023
 */
public class ServiceCall {

    private static final Logger log = LogManager.getLogger(ServiceCall.class);

    /**
     * Query params, if any, must be handled but the caller (and appended to the url, with the correct encoding)
     * 
     * @param url
     * @param headers. Can be null.
     * @return a ServiceCallResult
     * @since 2023
     */
    public ServiceCallResult get(String url, Map<String, String> headers) {

        ServiceCallResult result = null;

        HttpURLConnection connection = null;
        try {
            // Create the URL object
            URL theUrl = new URL(url);
            connection = (HttpURLConnection) theUrl.openConnection();
            connection.setRequestMethod("GET");

            if (headers != null) {
                headers.forEach(connection::setRequestProperty);
            }

            result = readResponse(connection);

        } catch (IOException e) {
            log.error("Error: " + e.getMessage());
            result = new ServiceCallResult("{}", -1, "IOException: " + e.getMessage());
        } finally {
            if (connection != null) {
                connection.disconnect();
                connection = null;
            }
        }

        return result;
    }

    /*
     * Just to centralize the calls. For now, they are the same
     * (may change in the future, depending on the change sin the service API)
     */
    protected ServiceCallResult postOrPut(String httpMethod, String url, Map<String, String> headers, String body) {

        ServiceCallResult result = null;

        HttpURLConnection connection = null;
        try {
            // Create the URL object
            URL theUrl = new URL(url);
            connection = (HttpURLConnection) theUrl.openConnection();
            // POST or PUT
            connection.setRequestMethod(httpMethod);

            if (headers != null) {
                headers.forEach(connection::setRequestProperty);
            }

            connection.setDoOutput(true);
            if (body != null) {
                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = body.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }
            }

            result = readResponse(connection);

        } catch (IOException e) {
            log.error("Error: " + e.getMessage());
            result = new ServiceCallResult("{}", -1, "IOException: " + e.getMessage());
        } finally {
            if (connection != null) {
                connection.disconnect();
                connection = null;
            }
        }

        return result;
    }

    public ServiceCallResult post(String url, Map<String, String> headers, String body) {

        ServiceCallResult result = postOrPut("POST", url, headers, body);

        return result;
    }

    public ServiceCallResult put(String url, Map<String, String> headers, String body) {

        ServiceCallResult result = postOrPut("PUT", url, headers, body);

        return result;
    }

    /**
     * The "response" field of <code>ServiceCallResult</code> is always an empty JSON object, "{}".
     * 
     * @param file
     * @param targetUrl
     * @param contentType
     * @return a Response
     * @throws IOException
     * @since 2023
     */
    public ServiceCallResult uploadFileWithPut(File file, String targetUrl, String contentType) throws IOException {

        if (!file.exists() || !file.isFile()) {
            throw new IllegalArgumentException("Invalid file: " + file.getAbsolutePath());
        }

        HttpURLConnection connection = (HttpURLConnection) new URL(targetUrl).openConnection();
        connection.setDoOutput(true);
        connection.setRequestMethod("PUT");
        connection.setRequestProperty("Content-Type", contentType);
        connection.setFixedLengthStreamingMode(file.length());

        ServiceCallResult result;
        try (OutputStream out = connection.getOutputStream(); InputStream in = new FileInputStream(file)) {

            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
            out.flush();

            result = new ServiceCallResult("{}", connection.getResponseCode(), connection.getResponseMessage());

        } catch (IOException e) {
            log.error("Error uploading file with PUT", e);
            result = new ServiceCallResult("{}", -1, e.getMessage());
        }

        return result;
    }

    /**
     * Utility, used by other methods (get, post, put), cone the call returns a status >= 200 < 300.
     * The "response" field of <code>Response</code> is always an empty JSON object, "{}".
     * 
     * @param connection
     * @return
     * @throws IOException
     * @since 2023
     */
    public ServiceCallResult readResponse(HttpURLConnection connection) throws IOException {

        ServiceCallResult result = null;

        int responseCode = connection.getResponseCode();
        if (ServiceCallResult.isHttpSuccess(responseCode)) {
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder responseStr = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    responseStr.append(line.trim());
                }
                result = new ServiceCallResult(responseStr.toString(), responseCode, connection.getResponseMessage());
            }
        } else {
            result = new ServiceCallResult("{}", responseCode, connection.getResponseMessage());
        }

        return result;
    }

}
