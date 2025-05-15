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
package org.nuxeo.labs.hyland.knowledge.enrichment.automation;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.labs.hyland.knowledge.enrichment.service.HylandKEService;

@Operation(id = HylandKEEnrichOp.ID, category = "Hyland Knowledge Enrichment", label = "CIC Knowledge Enrichement on Blob", description = ""
        + "Invoke the Hyland Knwoledge Enrichment (KE) API to enrich the blob. actions is a list of actions to process"
        + " (image-description, image-embeddings, …), classes a list of values to be used for classification,"
        + " and similarValues is used for metadata endpoint. (See KE documentation for details, limitation, etc.)")
public class HylandKEEnrichOp {

    public static final String ID = "HylandKnowledgeEnrichment.Enrich";

    @Param(name = "actions", required = true)
    protected String actions;

    @Param(name = "classes", required = false)
    protected String classes;

    @Param(name = "similarMetadata", required = false)
    protected String similarMetadata;

    @Context
    protected HylandKEService ciService;

    @OperationMethod
    public Blob run(Blob blob) {

        List<String> theActions = Arrays.stream(actions.split(",")).map(String::trim).toList();

        List<String> theClasses = null;
        if (StringUtils.isNotBlank(classes)) {
            theClasses = Arrays.stream(classes.split(",")).map(String::trim).toList();
        }

        List<String> theSimilarMetadata = null;
        if (StringUtils.isNotBlank(similarMetadata)) {
            theSimilarMetadata = Arrays.stream(similarMetadata.split(",")).map(String::trim).toList();
        }

        String response;
        try {
            response = ciService.enrich(blob, theActions, theClasses, theSimilarMetadata);
        } catch (IOException e) {
            throw new NuxeoException(e);
        }
        return Blobs.createJSONBlob(response);
    }

}
