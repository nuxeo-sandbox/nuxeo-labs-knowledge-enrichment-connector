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
import org.nuxeo.labs.hyland.knowledge.enrichment.automation.ConfigureServiceOp;
import org.nuxeo.labs.hyland.knowledge.enrichment.automation.HylandKEEnrichOp;
import org.nuxeo.labs.hyland.knowledge.enrichment.service.HylandKEService;
import org.nuxeo.labs.hyland.knowledge.enrichment.service.HylandKEServiceImpl;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features({ AutomationFeature.class, ConfigCheckerFeature.class })
@Deploy("nuxeo-hyland-knowledge-enrichment-connector-core")
public class TestMiscAutomation {

    @Inject
    protected CoreSession session;

    @Inject
    protected AutomationService automationService;

    @Inject
    protected HylandKEService hylandKEService;
    
    @Test
    public void shouldChangeConfig() throws Exception {
        
        HylandKEServiceImpl impl = (HylandKEServiceImpl) hylandKEService;

        OperationContext ctx = new OperationContext(session);
        
        Map<String, Object> params = new HashMap<>();
        params.put("maxTries", 20);
        params.put("sleepIntervalMS", 5000);
        // No similarMetadat in this test

        automationService.run(ctx, ConfigureServiceOp.ID, params);
        assertEquals(20, impl.getPullResultsMaxTries());
        assertEquals(5000, impl.getPullResultsSleepIntervalMS());
        

        params.put("maxTries", -1);
        params.put("sleepIntervalMS", -1);
        automationService.run(ctx, ConfigureServiceOp.ID, params);
        assertEquals(20, impl.getPullResultsMaxTries());
        assertEquals(5000, impl.getPullResultsSleepIntervalMS());
        

        params.put("maxTries", 0);
        params.put("sleepIntervalMS", 0);
        automationService.run(ctx, ConfigureServiceOp.ID, params);
        assertEquals(HylandKEServiceImpl.PULL_RESULTS_MAX_TRIES_DEFAULT, impl.getPullResultsMaxTries());
        assertEquals(HylandKEServiceImpl.PULL_RESULTS_SLEEP_INTERVAL_DEFAULT, impl.getPullResultsSleepIntervalMS());
        
    }
}
