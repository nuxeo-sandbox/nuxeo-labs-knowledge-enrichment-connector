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
 */
package org.nuxeo.labs.hyland.knowledge.enrichment.automation.function;

import org.apache.commons.io.IOUtils;
import org.nuxeo.ecm.automation.context.ContextHelper;
import org.nuxeo.ecm.core.api.Blob;

import java.io.IOException;
import java.util.Base64;

public class Base64Function implements ContextHelper {

    public Base64Function() {}

    public String blob2Base64(Blob blob) throws IOException {
        byte[] fileContent = IOUtils.toByteArray(blob.getStream());
        return Base64.getEncoder().encodeToString(fileContent);
    }

    public String string2Base64(String text) throws IOException {
        return Base64.getEncoder().encodeToString(text.getBytes());
    }

}

