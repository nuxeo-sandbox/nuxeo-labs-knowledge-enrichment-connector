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
 *     Thibaud Arguillere
 */
package org.nuxeo.labs.hyland.knowledge.enrichment.automation;

import java.io.IOException;

import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.labs.hyland.knowledge.enrichment.service.HylandKEService;
import org.nuxeo.labs.knowledge.enrichment.http.ServiceCallResult;

@Operation(id = HylandKEGetEnrichmentResultsOp.ID, category = "Hyland Knowledge Enrichment", label = "CIC Knowledge Enrichement Get Results", description = ""
        + "Invoke the Hyland Knowledge Enrichment (KE) API to get the processing results. Pass in jobId the value received"
        + " after a call to the HylandKnowledgeEnrichment.SendForEnrichment operation.")
public class HylandKEGetEnrichmentResultsOp {

    public static final String ID = "HylandKnowledgeEnrichment.GetEnrichmentResults";

    @Param(name = "jobId", required = true)
    protected String jobId;

    @Context
    protected HylandKEService ciService;

    @OperationMethod
    public Blob run() {

        ServiceCallResult result;
        try {
            result = ciService.getJobIdResult(jobId);
        } catch (IOException e) {
            throw new NuxeoException(e);
        }
        return Blobs.createJSONBlob(result.toJsonString());
    }

}
