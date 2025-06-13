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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.util.BlobList;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.labs.hyland.knowledge.enrichment.service.ContentToProcess;
import org.nuxeo.labs.hyland.knowledge.enrichment.service.HylandKEService;
import org.nuxeo.labs.hyland.knowledge.enrichment.http.ServiceCallResult;

@Operation(id = HylandKEEnrichSeveralOp.ID, category = "Hyland Knowledge Enrichment", label = "CIC Knowledge Enrichement on Blobs or documents", description = ""
        + "Invoke the Hyland Knowledge Enrichment (KE) API to enrich the input documents/blobs. actions is a list of actions to process"
        + " (image-description, image-embeddings, …), classes a list of values to be used for classification,"
        + " and similarValues is used for metadata endpoint. It must be passed as a. (See KE documentation for details, limitation, etc.)"
        + " If input is Documents, use xpath to tell the operation where to find the blobs."
        + " To map the 'objectKey' returned by the service with your blobs, use the sourceIds param (comma separated list of unique valued)."
        + " If input is a list od Documents and sourceIds is not passed, then we use the document UUIDs as 'sourceId'. "
        + " See the documentation for details.")
public class HylandKEEnrichSeveralOp {

    public static final String ID = "HylandKnowledgeEnrichment.EnrichSeveral";

    @Param(name = "actions", required = true)
    protected String actions;

    @Param(name = "classes", required = false)
    protected String classes;

    @Param(name = "similarMetadataJsonArrayStr", required = false)
    protected String similarMetadataJsonArrayStr;

    @Param(name = "extraJsonPayloadStr", required = false)
    protected String extraJsonPayloadStr = null;

    @Param(name = "xpath", required = false)
    protected String xpath = "file:content";

    @Param(name = "sourceIds", required = false)
    protected String sourceIds;

    @Context
    protected HylandKEService ciService;

    @OperationMethod
    public Blob run(DocumentModelList docs) {

        BlobList blobs = new BlobList();

        if (StringUtils.isBlank(sourceIds)) {
            List<String> ids = new ArrayList<String>();
            for (DocumentModel doc : docs) {
                ids.add(doc.getId());
            }
            sourceIds = String.join(",", ids);
        }

        for (DocumentModel doc : docs) {
            blobs.add((Blob) doc.getPropertyValue(xpath));
        }

        return run(blobs);
    }

    @OperationMethod
    public Blob run(BlobList blobs) {

        if (StringUtils.isBlank(sourceIds)) {
            throw new NuxeoException("sourceIds is required.");
        }

        List<String> sourceIdsArray = Arrays.stream(sourceIds.split(",")).map(String::trim).toList();
        if (sourceIdsArray.size() != blobs.size()) {
            throw new NuxeoException("The number od IDs in sourceIds is different than the number of blobs.");
        }

        @SuppressWarnings("rawtypes")
        List<ContentToProcess> contentToProcess = new ArrayList<ContentToProcess>();
        int idx = -1;
        for (Blob blob : blobs) {
            idx += 1;
            ContentToProcess<Blob> oneContent = new ContentToProcess<Blob>(sourceIdsArray.get(idx), blob);
            contentToProcess.add(oneContent);
        }

        List<String> theActions = Arrays.stream(actions.split(",")).map(String::trim).toList();

        List<String> theClasses = null;
        if (StringUtils.isNotBlank(classes)) {
            theClasses = Arrays.stream(classes.split(",")).map(String::trim).toList();
        }

        ServiceCallResult result;
        try {
            result = ciService.enrich(contentToProcess, theActions, theClasses, similarMetadataJsonArrayStr,
                    extraJsonPayloadStr);
        } catch (IOException e) {
            throw new NuxeoException(e);
        }
        return Blobs.createJSONBlob(result.toJsonString());
    }

}
