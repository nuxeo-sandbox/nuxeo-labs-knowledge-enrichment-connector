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
import static org.nuxeo.labs.hyland.knowledge.enrichment.service.HylandCIServiceImpl.CONTENT_INTELL_CACHE;
import static org.nuxeo.labs.hyland.knowledge.enrichment.service.HylandCIServiceImpl.getCacheKey;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.cache.Cache;
import org.nuxeo.ecm.core.cache.CacheService;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.labs.hyland.knowledge.enrichment.service.HylandCIService;
import org.nuxeo.labs.hyland.knowledge.enrichment.service.HylandCIServiceImpl;
import org.nuxeo.labs.hyland.knowledge.enrichment.service.HylandCIServiceImpl.CICService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.util.Base64;
import java.util.List;

@RunWith(FeaturesRunner.class)
@Features({ PlatformFeature.class, ConfigCheckerFeature.class })
@Deploy("nuxeo-hyland-content-intelligence-connector-core")
public class TestHylandCIService {

    private static final Logger log = LogManager.getLogger(TestHylandCIService.class);

    protected static final String TEST_IMAGE_PATH = "/files/dc-3-smaller.jpg";

    protected static final String TEST_IMAGE_MIMETYPE = "image/jpeg";

    protected static final String TEST_IMAGE_DESCRIPTION = "The image contains several iconic Disney characters,"
            + " including Goofy, Daisy Duck, and Mickey Mouse. "
            + "There is no human face visible in the image. The image appears to be a stylized, minimalist"
            + " representation of these classic cartoon characters. The characters are depicted as simple"
            + " line drawings in a limited color palette of black, white, yellow, and red.Goofy is shown with"
            + " his signature large ears and mouth, while Daisy Duck is recognizable by her distinctive red bow."
            + " Mickey Mouse is depicted as a minimalist silhouette, capturing his iconic rounded shape and ears."
            + " This seems to be an artistic rendering or logo design featuring these beloved Disney characters,"
            + " rather than a scene from a specific show or movie.";

    protected static String testImageBase64 = null;

    @Inject
    protected HylandCIService hylandCIService;

    @Before
    public void onceExecutedBeforeAll() throws Exception {

        if (testImageBase64 == null) {
            byte[] fileContent = FileUtils.readFileToByteArray(
                    new File(getClass().getResource(TEST_IMAGE_PATH).getPath()));
            testImageBase64 = Base64.getEncoder().encodeToString(fileContent);
        }
    }

    @Test
    public void testServiceIsDeployed() {
        assertNotNull(hylandCIService);
    }

    @Test
    public void quickTestToBeReworked() {

        Assume.assumeTrue(ConfigCheckerFeature.hasEnrichmentClientInfo());

        HylandCIServiceImpl sce = (HylandCIServiceImpl) hylandCIService;
        // Quick test. This methig should not be public
        String token = sce.fetchAuthTokenIfNeeded(CICService.ENRICHMENT);
        assertNotNull(token);
    }

    @Test
    public void shouldReturn404OnBadEndPoint() {
        Assume.assumeTrue(ConfigCheckerFeature.hasEnrichmentClientInfo());

        String result = hylandCIService.invokeEnrichment("GET", "/INVALID_END_POINT", null);

        assertNotNull(result);
        JSONObject resultJson = new JSONObject(result);

        int responseCode = resultJson.getInt("responseCode");
        assertEquals(responseCode, 404);
    }

    @Test
    public void canGetContentProcessActions() {

        Assume.assumeTrue(ConfigCheckerFeature.hasEnrichmentClientInfo());

        String result = hylandCIService.invokeEnrichment("GET", "/api/content/process/actions", null);

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

        String result = hylandCIService.invokeEnrichment("GET",
                "/api/files/upload/presigned-url?contentType=" + TEST_IMAGE_MIMETYPE.replace("/", "%2F"), null);
        assertNotNull(result);
        JSONObject resultJson = new JSONObject(result);

        int responseCode = resultJson.getInt("responseCode");
        assertEquals(responseCode, 200);

        JSONObject response = resultJson.getJSONObject("response");
        assertNotNull(result);

        String presignedUrl = response.getString("presignedUrl");
        assertNotNull(result);

        String objectKey = response.getString("objectKey");
        assertNotNull(objectKey);

    }

    @Test
    public void shouldGetImageDescription() throws Exception {
        Assume.assumeTrue(ConfigCheckerFeature.hasEnrichmentClientInfo());

        HylandCIServiceImpl sce = (HylandCIServiceImpl) hylandCIService;

        File f = new File(getClass().getResource(TEST_IMAGE_PATH).getPath());
        String result = sce.enrich(f, TEST_IMAGE_MIMETYPE, List.of("image-description"), null, null);
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

        HylandCIServiceImpl sce = (HylandCIServiceImpl) hylandCIService;

        File f = new File(getClass().getResource(TEST_IMAGE_PATH).getPath());
        String result = sce.enrich(f, TEST_IMAGE_MIMETYPE, List.of("image-embeddings"), null, null);
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

        HylandCIServiceImpl sce = (HylandCIServiceImpl) hylandCIService;

        File f = new File(getClass().getResource(TEST_IMAGE_PATH).getPath());
        String result = sce.enrich(f, TEST_IMAGE_MIMETYPE, List.of("image-classification"),
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
    public void shouldGetSeveralStuffOnImage() throws Exception {

        Assume.assumeTrue(ConfigCheckerFeature.hasEnrichmentClientInfo());

        HylandCIServiceImpl sce = (HylandCIServiceImpl) hylandCIService;

        File f = new File(getClass().getResource(TEST_IMAGE_PATH).getPath());
        String result = sce.enrich(f, TEST_IMAGE_MIMETYPE,
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

}
