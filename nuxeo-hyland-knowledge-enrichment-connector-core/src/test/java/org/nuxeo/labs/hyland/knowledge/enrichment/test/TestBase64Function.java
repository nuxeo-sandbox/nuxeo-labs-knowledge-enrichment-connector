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

import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.test.AutomationFeature;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.labs.hyland.knowledge.enrichment.automation.function.Base64Function;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import java.io.File;
import java.io.IOException;

@RunWith(FeaturesRunner.class)
@Features({AutomationFeature.class})
@Deploy("nuxeo-hyland-knowledge-enrichment-connector-core")
// @Ignore, see automation-contrib.xml
@Ignore
public class TestBase64Function {

    @Test
    public void testBlob2Base64Conversion() throws IOException {
        Blob blob = new FileBlob(new File(getClass().getResource("/files/musubimaru.png").getPath()));
        Base64Function fn = new Base64Function();
        String base64str = fn.blob2Base64(blob);
        Assert.assertTrue(StringUtils.isNotBlank(base64str));
    }

    @Test
    public void testString2Base64Conversion() throws IOException {
        Base64Function fn = new Base64Function();
        String base64str = fn.string2Base64("This is a test");
        Assert.assertEquals("VGhpcyBpcyBhIHRlc3Q=", base64str);
    }
}
