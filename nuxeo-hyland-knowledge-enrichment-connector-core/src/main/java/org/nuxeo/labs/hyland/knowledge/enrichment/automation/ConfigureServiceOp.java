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

import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.labs.hyland.knowledge.enrichment.service.HylandKEService;

@Operation(id = ConfigureServiceOp.ID, category = "Hyland Knowledge Enrichment", label = "Configure Calls to Service", description = ""
        + "Allows fordynamically changing some settings when calling the service."
        + " if a value is 0 => reset to configuration or default value. If -1 (or not passed) => do not change")
public class ConfigureServiceOp {

    public static final String ID = "HylandKnowledgeEnrichment.Configure";

    @Param(name = "maxTries", required = false)
    protected Integer maxTries = null;

    @Param(name = "sleepIntervalMS", required = false)
    protected Integer sleepIntervalMS = null;

    @Context
    protected HylandKEService ciService;

    @OperationMethod
    public void run() {
        
        ciService.setPullResultsSettings(maxTries == null ? 1 : maxTries, sleepIntervalMS == null ? -1 : sleepIntervalMS);
        
    }

}
