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
package org.nuxeo.labs.hyland.knowledge.enrichment.service;

import java.io.File;
import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CloseableFile;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeRegistry;
import org.nuxeo.runtime.api.Framework;

/**
 * @since TODO
 */
public class ContentToProcess<T> {

    protected String sourceId;

    protected Blob blob;

    protected File file;

    protected CloseableFile closeableFile;

    protected String mimeType;

    protected String objectKey;
    
    protected boolean processingSuccess;
    
    protected String errorMessage;

    public ContentToProcess(String sourceId, T content) {
        super();
        
        this.sourceId = sourceId;

        if (content instanceof Blob) {
            blob = (Blob) content;
            setFileFromBlob();
        } else if (content instanceof File) {
            file = (File) content;
        } else {
            throw new IllegalArgumentException("Expecting Blob or File");
        }

        updateMimeType();
    }

    protected void setFileFromBlob() {

        file = blob.getFile();
        if (file == null) {
            // This is possible (File on S3 not yet cached for example)
            try {
                closeableFile = blob.getCloseableFile();
                file = closeableFile.getFile();
            } catch (IOException e) {
                throw new NuxeoException("Failed to get a CloseableFile", e);
            }
        }
    }

    protected void updateMimeType() {

        if (blob != null) {
            mimeType = blob.getMimeType();
            if (StringUtils.isBlank(mimeType)) {
                MimetypeRegistry registry = Framework.getService(MimetypeRegistry.class);
                mimeType = registry.getMimetypeFromBlob(blob);
            }
        } else {
            MimetypeRegistry registry = Framework.getService(MimetypeRegistry.class);
            mimeType = registry.getMimetypeFromFile(file);
        }
    }

    public void cleanup() throws IOException {

        if (closeableFile != null) {
            try {
                closeableFile.close();
            } catch (IOException e) {
                // Ignore
            }
        }
    }

    public String getObjectKey() {
        return objectKey;
    }

    public void setObjectKey(String objectKey) {
        this.objectKey = objectKey;
    }

    public String getSourceId() {
        return sourceId;
    }

    public Blob getBlob() {
        return blob;
    }

    public File getFile() {
        return file;
    }

    public String getMimeType() {
        return mimeType;
    }

    public boolean isProcessingSuccess() {
        return processingSuccess;
    }

    public void setProcessingSuccess(boolean processingSuccess) {
        this.processingSuccess = processingSuccess;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

}
