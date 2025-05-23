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
package org.nuxeo.labs.knowledge.enrichment.http;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.nuxeo.ecm.core.api.NuxeoException;

/**
 * Class handling the result of a HTTP call to the service. It encapsulates 3 values:
 * <ul>
 * <li>responseCode: The HTTP status (200, 401, 404, etc.), as returned by the service</li>
 * <li>responseMessage: The response message, as returned by the service ("OK" for example)</li>
 * <li>response: The response as returned by the service</li>
 * </ul
 * 
 * @since 2023
 */
public class ServiceCallResult {

    protected String response;

    protected int responseCode;

    protected String responseMessage;

    protected JSONArray objectKeysMapping = null;

    public ServiceCallResult(String response, int responseCode, String responseMessage) {
        super();
        this.response = response;
        this.responseCode = responseCode;
        this.responseMessage = responseMessage;
    }

    public ServiceCallResult(String response, int responseCode, String responseMessage, JSONArray objectKeysMapping) {
        super();
        this.response = response;
        this.responseCode = responseCode;
        this.responseMessage = responseMessage;
        this.objectKeysMapping = objectKeysMapping;
    }
    
    // M%ainly used in unit tests.
    public ServiceCallResult(String jsonStr) {
        JSONObject obj = new JSONObject(jsonStr);
        
        response = obj.getJSONObject("response").toString();
        responseCode = obj.getInt("responseCode");
        responseMessage = obj.getString("responseMessage");
        objectKeysMapping = obj.getJSONArray("objectKeysMapping");
    }

    /**
     * @return the JSON object of thsi object
     * @since 2023
     */
    public JSONObject toJsonObject() {

        JSONObject obj = new JSONObject();

        obj.put("response", new JSONObject(response));
        obj.put("responseCode", responseCode);
        obj.put("responseMessage", (responseMessage == null ? "" : responseMessage));
        obj.put("objectKeysMapping", objectKeysMapping);

        return obj;
    }

    /**
     * @return the JSON String of this object
     * @since 2023
     */
    public String toJsonString() {

        return toJsonString(0);
    }

    public String toJsonString(int indentFactor) {

        JSONObject obj = toJsonObject();
        return obj.toString(indentFactor);
    }

    /**
     * Some APIs don't return a JSON object (nor array).
     * And it even may be quoted/double quoted in the response.
     * 
     * @return the response. If it started and ended with ", these are removed.
     * @since And it may be quoted in the response.
     */
    public String getResponse() {
        String result = StringUtils.removeStart(response, "\"");
        result = StringUtils.removeEnd(result, "\"");
        return result;
    }

    /**
     * Return the response from the service as JSONObject. Throws an exception if the response cannot be parsed as JSON
     * 
     * @return the response from the service as JSONObject
     * @since 2023
     */
    public JSONObject getResponseAsJSONObject() {
        if (response != null && !response.startsWith("{") && !response.startsWith("[")) {
            throw new NuxeoException(
                    "response is a simple string, cannot be converted to JSON Object. Call getResponse() instead.");
        }
        return new JSONObject(response);
    }

    /**
     * Always return a JSON Object with a single field, "result", holding the raw response (that can be a simple String,
     * or JSON
     * 
     * @return
     * @since TODO
     */
    public JSONObject forceResponseAsJSONObject() {

        String resultStr;

        if (response == null) {
            resultStr = "{\"result\": null}";

            return new JSONObject(resultStr);
        }

        if (response.startsWith("{") || response.startsWith("[")) {
            JSONObject responseJson = new JSONObject(response);
            JSONObject result = new JSONObject();
            result.put("result", responseJson);

            return result;
        }

        // Not null and not JSON string
        resultStr = "{\"result\":";
        if (response.startsWith("\"")) {
            // Assume it ends with "
            resultStr += response;
        } else {
            resultStr = "\"" + response + "\"";
        }
        resultStr += "}";
        return new JSONObject(resultStr);

        // throw new NuxeoException("response is a simple string, cannot be converted to JSON Object. Call
        // getResponse() instead.");
    }

    public void setObjectKeysMapping(JSONArray mapping) {
        this.objectKeysMapping = mapping;
    }
    
    public JSONArray getObjectKeysMapping() {
        return objectKeysMapping;
    }

    /**
     * Return the response from the service as JSONArray. Throws an exception if the response cannot be parsed as JSON
     * 
     * @return the response from the service as JSONArray
     * @since 2023
     */
    public JSONArray getResponseAsJSONArray() {
        if (response != null && !response.startsWith("[")) {
            throw new NuxeoException(
                    "response is a simple string, cannot be converted to JSON Array. Call getResponse() instead.");
        }
        return new JSONArray(response);
    }

    public int getResponseCode() {
        return responseCode;
    }

    public String getResponseMessage() {
        return responseMessage;
    }

    /**
     * @return true if responseCode is in the "OK" range
     * @since 2023
     */
    public boolean callWasSuccesful() {
        return isHttpSuccess(responseCode);
    }

    /**
     * @return true if responseCode is not in the "OK" range
     * @since 2023
     */
    public boolean callFailed() {
        return !isHttpSuccess(responseCode);
    }

    /**
     * @return true if statusCode is in the "OK" range
     * @since 2023
     */
    public static boolean isHttpSuccess(int statusCode) {
        return statusCode >= 200 && statusCode < 300;
    }

}
