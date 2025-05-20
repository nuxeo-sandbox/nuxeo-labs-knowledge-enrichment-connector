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
 * @since TODO
 */
public class ServiceCallResult {
    protected String response;

    protected int responseCode;

    protected String responseMessage;

    public ServiceCallResult(String response, int responseCode, String responseMessage) {
        super();
        this.response = response;
        this.responseCode = responseCode;
        this.responseMessage = responseMessage;
    }

    public JSONObject toJsonObject() {
        String jsonStr = toJsonString();
        return new JSONObject(jsonStr);
    }

    public String toJsonString() {

        // For now, the response is simple enough to quickly build it as a String (instead of creating a JSON object,
        // adding fields, then toString())
        String jsonResponseStr = "{";
        jsonResponseStr += "\"response\": " + response + ",";
        jsonResponseStr += "\"responseCode\": " + responseCode + ",";
        jsonResponseStr += "\"responseMessage\": \"" + (responseMessage == null ? "" : responseMessage) + "\"";
        jsonResponseStr += "}";

        return jsonResponseStr;
    }

    // Some APIs don't return a JSON object (nor array)
    // And it may be quoted in the response.
    public String getResponse() {
        String result = StringUtils.removeStart(response,  "\"");
        result = StringUtils.removeEnd(result,  "\"");
        return result;
    }

    public JSONObject getResponseAsJSONObject() {
        if (response != null && !response.startsWith("{") && !response.startsWith("[")) {
            throw new NuxeoException(
                    "response is a simple string, cannot be converted to JSON Object. Call getResponse() instead.");
        }
        return new JSONObject(response);
    }

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

    public boolean callWasSuccesful() {
        return responseCode >= 200 && responseCode < 300;
    }

    public boolean callFailed() {
        return responseCode < 200 || responseCode >= 300;
    }

}
