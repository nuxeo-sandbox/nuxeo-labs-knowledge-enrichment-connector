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

import java.io.Closeable;
import java.io.File;
import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CloseableFile;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeRegistry;
import org.nuxeo.runtime.api.Framework;

/**
 * Handle either a Blob or a File.
 * <br>
 * When calling getFile(), if the main object is a blob, the class first get a
 * CloseableFile to make sure there is a file ((File on S3 not yet cached for example)
 * This why caller must call the close() method once done with the ContentToProcess.
 * 
 * @since 2023
 */
public class ContentToProcess<T> implements Closeable {

    private static final Logger log = LogManager.getLogger(ContentToProcess.class);

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
        } else if (content instanceof File) {
            file = (File) content;
        } else {
            throw new IllegalArgumentException("Expecting Blob or File");
        }

        updateMimeType();
    }

    protected void updateMimeType() {

        MimetypeRegistry registry = Framework.getService(MimetypeRegistry.class);
        if (blob != null) {
            mimeType = blob.getMimeType();
            if (StringUtils.isBlank(mimeType)) {
                mimeType = registry.getMimetypeFromBlob(blob);
            }
        } else {
            mimeType = registry.getMimetypeFromFile(file);
        }
    }

    public void close() {
        if (closeableFile != null) {
            try {
                closeableFile.close();
            } catch (IOException e) {
                log.warn("Failed to close the CloseableFile.", e);
            }
            closeableFile = null;
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
        
        if(file != null) {
            return file;
        }

        File f = blob.getFile();
        if (f == null) {
            // This is possible (File on S3 not yet cached for example)
            try {
                closeableFile = blob.getCloseableFile();
                f = closeableFile.getFile();
            } catch (IOException e) {
                throw new NuxeoException("Failed to get a CloseableFile", e);
            }
        }

        return f;
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
