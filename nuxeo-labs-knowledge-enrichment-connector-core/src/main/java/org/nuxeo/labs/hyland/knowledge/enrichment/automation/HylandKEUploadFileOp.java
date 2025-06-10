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

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CloseableFile;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeRegistry;
import org.nuxeo.labs.hyland.knowledge.enrichment.service.HylandKEService;
import org.nuxeo.labs.knowledge.enrichment.http.ServiceCall;
import org.nuxeo.labs.knowledge.enrichment.http.ServiceCallResult;
import org.nuxeo.runtime.api.Framework;

@Operation(id = HylandKEUploadFileOp.ID, category = "Hyland Knowledge Enrichment", label = "CIC Knowledge Enrichement Upload File", description = ""
        + "Granular operation, to be used in conjonction with HylandKnowledgeEnrichment.Invoke, once you got a presigned URL for the file."
        + " The result JSON will only have a responseCode (should be 200) and responseMessage properties.")
public class HylandKEUploadFileOp {

    public static final String ID = "HylandKnowledgeEnrichment.UploadFile";

    @Param(name = "presignedUrl", required = true)
    protected String presignedUrl;

    @Param(name = "mimeType", required = false)
    protected String mimeType = null;

    @Context
    protected HylandKEService ciService;

    @OperationMethod
    public Blob run(Blob blob) {

        try (CloseableFile closeableFile = blob.getCloseableFile()) {
            if (StringUtils.isBlank(mimeType)) {
                MimetypeRegistry registry = Framework.getService(MimetypeRegistry.class);
                mimeType = registry.getMimetypeFromBlob(blob);
            }

            ServiceCall serviceCall = new ServiceCall();

            ServiceCallResult result = serviceCall.uploadFileWithPut(closeableFile.getFile(), presignedUrl, mimeType);
            return Blobs.createJSONBlob(result.toJsonString());

        } catch (IOException e) {
            throw new NuxeoException(e);
        }
    }

}
