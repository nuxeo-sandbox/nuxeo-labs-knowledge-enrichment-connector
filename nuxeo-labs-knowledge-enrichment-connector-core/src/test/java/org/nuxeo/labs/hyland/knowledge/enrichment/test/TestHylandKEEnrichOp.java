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
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.util.BlobList;
import org.nuxeo.ecm.automation.test.AutomationFeature;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.labs.hyland.knowledge.enrichment.automation.HylandKEEnrichOp;
import org.nuxeo.labs.hyland.knowledge.enrichment.automation.HylandKEEnrichSeveralOp;
import org.nuxeo.labs.hyland.knowledge.enrichment.service.HylandKEService;
import org.nuxeo.labs.knowledge.enrichment.http.ServiceCallResult;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features({ AutomationFeature.class, ConfigCheckerFeature.class })
@Deploy("nuxeo-hyland-knowledge-enrichment-connector-core")
public class TestHylandKEEnrichOp {

    @Inject
    protected CoreSession session;

    @Inject
    protected AutomationService automationService;

    @Inject
    protected HylandKEService hylandKEService;

    @Test
    public void shouldEnrichBlob() throws Exception {

        Assume.assumeTrue(ConfigCheckerFeature.hasEnrichmentClientInfo());

        OperationContext ctx = new OperationContext(session);
        
        File f = FileUtils.getResourceFileFromContext(TestHylandKEService.TEST_IMAGE_PATH);
        Blob blob = new FileBlob(f);
        blob.setMimeType(TestHylandKEService.TEST_IMAGE_MIMETYPE);
        blob.setFilename(f.getName());
        ctx.setInput(blob);
        
        Map<String, Object> params = new HashMap<>();
        params.put("actions", "image-description,image-embeddings,image-classification");
        params.put("classes", "Disney,DC Comics,Marvel");
        // No similarMetadat in this test

        Blob result = (Blob) automationService.run(ctx, HylandKEEnrichOp.ID, params);
        Assert.assertNotNull(result);

        JSONObject resultJson = new JSONObject(result.getString());
        // Chekc HTTP call
        int responseCode = resultJson.getInt("responseCode");
        assertEquals(200, responseCode);
        
        // Now check service results
        JSONObject response = resultJson.getJSONObject("response");
        String status = response.getString("status");
        assertNotEquals("FAILURE", status); // We accept PARTIAL_FAILURE

        JSONArray results = response.getJSONArray("results");
        JSONObject theResult = results.getJSONObject(0);
        
        // ==========> Description
        JSONObject descriptionJson = theResult.getJSONObject("imageDescription");
        if(descriptionJson.getBoolean("isSuccess")) {
            String description = descriptionJson.getString("result");
            // We should have at least "Mickey"
            assertTrue(description.toLowerCase().indexOf("mickey") > -1);
        }
        
        // ==========> Embeddings
        JSONObject embeddingsJson = theResult.getJSONObject("imageEmbeddings");
        if(embeddingsJson.getBoolean("isSuccess")) {
            JSONArray embeddings = embeddingsJson.getJSONArray("result");
            assertTrue(embeddings.length() == 1024);
        }
        
        
        // ==========> Classification
        JSONObject classificationJson = theResult.getJSONObject("imageClassification");
        if(classificationJson.getBoolean("isSuccess")) {
            String classification = classificationJson.getString("result");
            // So far the service returns the value lowercase anyway (which is a problem if the list of values are from a
            // vocabulary)
            assertEquals("disney", classification.toLowerCase());
        }
    }
    @Test
    public void shouldEnrichSeveralBlobs() throws Exception {

        Assume.assumeTrue(ConfigCheckerFeature.hasEnrichmentClientInfo());

        File f1 = FileUtils.getResourceFileFromContext(TestHylandKEService.TEST_IMAGE_PATH);
        Blob blob1 = new FileBlob(f1);
        blob1.setMimeType(TestHylandKEService.TEST_IMAGE_MIMETYPE);
        blob1.setFilename(f1.getName());
        
        File f2 = FileUtils.getResourceFileFromContext(TestHylandKEService.TEST_OTHER_IMAGE_PATH);
        Blob blo2b = new FileBlob(f1);
        blo2b.setMimeType(TestHylandKEService.TEST_OTHER_IMAGE_MIMETYPE);
        blo2b.setFilename(f2.getName());
        
        BlobList blobs = new BlobList();
        blobs.add(blob1);
        blobs.add(blo2b);

        OperationContext ctx = new OperationContext(session);
        ctx.setInput(blobs);
        Map<String, Object> params = new HashMap<>();
        params.put("actions", "image-description");
        params.put("sourceIds", "12345,67890");
        // no classes in this test
        // No similarMetadat in this test

        Blob resultBlob = (Blob) automationService.run(ctx, HylandKEEnrichSeveralOp.ID, params);
        Assert.assertNotNull(resultBlob);
        
        ServiceCallResult result = new ServiceCallResult(resultBlob.getString());
        assertNotNull(result);
        
        // Expecting HTTP OK
        assertTrue(result.callWasSuccesful());
        
        JSONArray mapping = result.getObjectKeysMapping();
        assertNotNull(mapping);
        assertEquals(2, mapping.length());
        // And we have our IDs
        assertTrue(TestHylandKEService.hasValueInJSONArray(mapping, "sourceId", "12345"));
        assertTrue(TestHylandKEService.hasValueInJSONArray(mapping, "sourceId", "67890"));

        JSONObject responseJson = result.getResponseAsJSONObject();
        String status = responseJson.getString("status");
        // We accept PARTIAL FAILURE.
        assertNotEquals("FAILURE", status);

        JSONArray results = responseJson.getJSONArray("results");
        assertTrue(results.length() == 2);
        
        //Check we have a description with the correct object Mapping
        results.forEach(oneResult -> {
            JSONObject resultObj = (JSONObject) oneResult;
            
            JSONObject descriptionObj = resultObj.getJSONObject("imageDescription");
            assertNotNull(descriptionObj);
            
            boolean isSuccess = descriptionObj.getBoolean("isSuccess");
            if(isSuccess) {
                String objectKey = resultObj.getString("objectKey");
                // Must exists in the returned mapping
                assertTrue(TestHylandKEService.hasValueInJSONArray(mapping, "objectKey", objectKey));
            }
        });
        
    }
}
