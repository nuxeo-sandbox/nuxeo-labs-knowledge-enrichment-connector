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
import org.nuxeo.ecm.automation.test.AutomationFeature;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.labs.hyland.knowledge.enrichment.automation.HylandKEEnrichOp;
import org.nuxeo.labs.hyland.knowledge.enrichment.service.HylandKEService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features({ AutomationFeature.class, ConfigCheckerFeature.class })
@Deploy("nuxeo-hyland-knowledge-enrichment-connector-core")
public class TestHylandKEEnrichOp {

    protected static final String TEST_IMAGE_PATH = "files/dc-3-smaller.jpg";

    protected static final String TEST_IMAGE_MIMETYPE = "image/jpeg";

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
        
        File f = FileUtils.getResourceFileFromContext(TEST_IMAGE_PATH);
        Blob blob = new FileBlob(f);
        blob.setMimeType(TEST_IMAGE_MIMETYPE);
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
