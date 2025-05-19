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
package org.nuxeo.labs.hyland.knowledge.enrichment.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.List;

import javax.inject.Inject;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.labs.hyland.knowledge.enrichment.service.HylandKEService;
import org.nuxeo.labs.hyland.knowledge.enrichment.service.HylandKEServiceImpl;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features({ PlatformFeature.class, ConfigCheckerFeature.class })
@Deploy("nuxeo-hyland-knowledge-enrichment-connector-core")
public class TestHylandKEService {

    protected static final String TEST_IMAGE_PATH = "files/dc-3-smaller.jpg";

    protected static final String TEST_CONTRACT_PATH = "files/samplecontract.pdf";

    protected static final String TEST_IMAGE_MIMETYPE = "image/jpeg";

    @Inject
    protected HylandKEService hylandKEService;

    @Before
    public void onceExecutedBeforeAll() throws Exception {

        // Actually, nothing to do here.
    }

    @Test
    public void testServiceIsDeployed() {
        assertNotNull(hylandKEService);
    }

    @Test
    public void shouldReturn404OnBadEndPoint() {
        Assume.assumeTrue(ConfigCheckerFeature.hasEnrichmentClientInfo());

        String result = hylandKEService.invokeEnrichment("GET", "/INVALID_END_POINT", null);

        assertNotNull(result);
        JSONObject resultJson = new JSONObject(result);

        int responseCode = resultJson.getInt("responseCode");
        assertEquals(responseCode, 404);
    }

    @Test
    public void canGetContentProcessActions() {

        Assume.assumeTrue(ConfigCheckerFeature.hasEnrichmentClientInfo());

        String result = hylandKEService.invokeEnrichment("GET", "/api/content/process/actions", null);

        assertNotNull(result);
        JSONObject resultJson = new JSONObject(result);

        int responseCode = resultJson.getInt("responseCode");
        assertEquals(responseCode, 200);

        JSONArray actions = resultJson.getJSONArray("response");
        assertNotNull(actions);
        assertTrue(actions.length() > 0);
    }

    @Test
    public void shouldGetPresignedUrl() {

        Assume.assumeTrue(ConfigCheckerFeature.hasEnrichmentClientInfo());

        String result = hylandKEService.invokeEnrichment("GET",
                "/api/files/upload/presigned-url?contentType=" + TEST_IMAGE_MIMETYPE.replace("/", "%2F"), null);
        assertNotNull(result);
        JSONObject resultJson = new JSONObject(result);

        int responseCode = resultJson.getInt("responseCode");
        assertEquals(responseCode, 200);

        JSONObject response = resultJson.getJSONObject("response");
        assertNotNull(result);

        String presignedUrl = response.getString("presignedUrl");
        assertNotNull(presignedUrl);

        String objectKey = response.getString("objectKey");
        assertNotNull(objectKey);

    }

    @Test
    public void shouldGetImageDescription() throws Exception {
        Assume.assumeTrue(ConfigCheckerFeature.hasEnrichmentClientInfo());

        File f = FileUtils.getResourceFileFromContext(TEST_IMAGE_PATH);
        String result = hylandKEService.enrich(f, TEST_IMAGE_MIMETYPE, List.of("image-description"), null, null);
        assertNotNull(result);

        JSONObject resultJson = new JSONObject(result);
        /*
         * This is an object built by HylandCIServiceImpl, embedding the response from CIC and the result of the HTTP
         * call
         * {
         * "response": the service response. Something like
         * {
         * "id": "...",
         * "timestamp": "...",
         * "status": "SUCCESS",
         * "results: [
         * {
         * "objectKey": "...",
         * "imageDescription": {...},
         * "metadata": ...,
         * "textSummary": ...,
         * ...etc...
         * }
         * ]
         * }
         * "responseCode": the HTTP status,
         * "responseMessage": the response message
         * }
         */

        // Expecting HTTP OK
        assertEquals(200, resultJson.getInt("responseCode"));

        JSONObject response = resultJson.getJSONObject("response");
        String status = response.getString("status");
        assertEquals("SUCCESS", status);

        JSONArray results = response.getJSONArray("results");
        JSONObject theResult = results.getJSONObject(0);
        JSONObject descriptionJson = theResult.getJSONObject("imageDescription");
        assertTrue(descriptionJson.getBoolean("isSuccess"));

        String description = descriptionJson.getString("result");
        // We should have at least "Mickey"
        assertTrue(description.toLowerCase().indexOf("mickey") > -1);
    }

    @Test
    public void shouldGetImageEmbeddings() throws Exception {

        Assume.assumeTrue(ConfigCheckerFeature.hasEnrichmentClientInfo());

        File f = FileUtils.getResourceFileFromContext(TEST_IMAGE_PATH);
        String result = hylandKEService.enrich(f, TEST_IMAGE_MIMETYPE, List.of("image-embeddings"), null, null);
        assertNotNull(result);

        JSONObject resultJson = new JSONObject(result);

        // Expecting HTTP OK
        assertEquals(200, resultJson.getInt("responseCode"));

        JSONObject response = resultJson.getJSONObject("response");
        String status = response.getString("status");
        assertEquals("SUCCESS", status);

        JSONArray results = response.getJSONArray("results");
        JSONObject theResult = results.getJSONObject(0);
        JSONObject embeddingsJson = theResult.getJSONObject("imageEmbeddings");
        assertTrue(embeddingsJson.getBoolean("isSuccess"));

        JSONArray embeddings = embeddingsJson.getJSONArray("result");
        assertTrue(embeddings.length() == 1024);
    }

    @Test
    public void shouldGetImageClassification() throws Exception {

        Assume.assumeTrue(ConfigCheckerFeature.hasEnrichmentClientInfo());

        File f = FileUtils.getResourceFileFromContext(TEST_IMAGE_PATH);
        String result = hylandKEService.enrich(f, TEST_IMAGE_MIMETYPE, List.of("image-classification"),
                List.of("Disney", "DC Comics", "Marvel"), null);
        assertNotNull(result);

        JSONObject resultJson = new JSONObject(result);

        // Expecting HTTP OK
        assertEquals(200, resultJson.getInt("responseCode"));

        JSONObject response = resultJson.getJSONObject("response");
        String status = response.getString("status");
        assertEquals("SUCCESS", status);

        JSONArray results = response.getJSONArray("results");
        JSONObject theResult = results.getJSONObject(0);
        JSONObject classificationJson = theResult.getJSONObject("imageClassification");
        assertTrue(classificationJson.getBoolean("isSuccess"));

        String classification = classificationJson.getString("result");
        // So far the service returns the value lowercase anyway (which is a problem if the list of values are from a
        // vocabulary)
        assertEquals("disney", classification.toLowerCase());
    }

    @Test
    public void shouldGetSeveralEnrichmentsOnImage() throws Exception {

        Assume.assumeTrue(ConfigCheckerFeature.hasEnrichmentClientInfo());

        File f = FileUtils.getResourceFileFromContext(TEST_IMAGE_PATH);
        String result = hylandKEService.enrich(f, TEST_IMAGE_MIMETYPE,
                List.of("image-description", "image-embeddings", "image-classification"),
                List.of("Disney", "DC Comics", "Marvel"), null);
        assertNotNull(result);

        JSONObject resultJson = new JSONObject(result);

        // Expecting HTTP OK
        assertEquals(200, resultJson.getInt("responseCode"));

        JSONObject response = resultJson.getJSONObject("response");
        String status = response.getString("status");
        assertEquals("SUCCESS", status);

        JSONArray results = response.getJSONArray("results");
        JSONObject theResult = results.getJSONObject(0);
        
        // ==========> Description
        JSONObject descriptionJson = theResult.getJSONObject("imageDescription");
        assertTrue(descriptionJson.getBoolean("isSuccess"));

        String description = descriptionJson.getString("result");
        // We should have at least "Mickey"
        assertTrue(description.toLowerCase().indexOf("mickey") > -1);
        
        
        // ==========> Embeddings
        JSONObject embeddingsJson = theResult.getJSONObject("imageEmbeddings");
        assertTrue(embeddingsJson.getBoolean("isSuccess"));

        JSONArray embeddings = embeddingsJson.getJSONArray("result");
        assertTrue(embeddings.length() == 1024);
        
        
        // ==========> Classification
        JSONObject classificationJson = theResult.getJSONObject("imageClassification");
        assertTrue(classificationJson.getBoolean("isSuccess"));

        String classification = classificationJson.getString("result");
        // So far the service returns the value lowercase anyway (which is a problem if the list of values are from a
        // vocabulary)
        assertEquals("disney", classification.toLowerCase());
    }
    
    @Test
    public void shouldGetDataCuration() throws Exception {

        Assume.assumeTrue(ConfigCheckerFeature.hasDataCurationClientInfo());
        
        File f = FileUtils.getResourceFileFromContext(TEST_CONTRACT_PATH);
        
        // No embeddings
        // schema MDATS - FULL - PIPELINE. See https://hyland.github.io/DocumentFilters-Docs/latest/getting_started_with_document_filters/about_json_output.html#json_output_schema
        String options = "{\"normalization\": {\"quotations\": true},\"chunking\": true,\"embedding\": false, \"json_schema\": \"MDAST\"}";
        String result = hylandKEService.curate(f, options);
        assertNotNull(result);
        
        File file = new File("/Users/thibaud.arguillere/Desktop/output-MDAST.json");
        org.apache.commons.io.FileUtils.writeStringToFile(file, result, "UTF-8");
        
        
        options = "{\"normalization\": {\"quotations\": true},\"chunking\": true,\"embedding\": false, \"json_schema\": \"FULL\"}";
        result = hylandKEService.curate(f, options);
        assertNotNull(result);
        file = new File("/Users/thibaud.arguillere/Desktop/output-FULL.json");
        org.apache.commons.io.FileUtils.writeStringToFile(file, result, "UTF-8");
        
        
        options = "{\"normalization\": {\"quotations\": true},\"chunking\": true,\"embedding\": false, \"json_schema\": \"PIPELINE\"}";
        result = hylandKEService.curate(f, options);
        assertNotNull(result);
        file = new File("/Users/thibaud.arguillere/Desktop/output-PIPELINE.json");
        org.apache.commons.io.FileUtils.writeStringToFile(file, result, "UTF-8");
        
        
        
        
        
        
        

        JSONObject resultJson = new JSONObject(result);

        // Expecting HTTP OK
        assertEquals(200, resultJson.getInt("responseCode"));

        JSONObject response = resultJson.getJSONObject("response");
        assertNotNull(response);
    }

}
