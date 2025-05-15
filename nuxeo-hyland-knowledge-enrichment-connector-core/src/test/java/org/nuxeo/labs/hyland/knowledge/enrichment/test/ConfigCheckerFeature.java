/*
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

import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RunnerFeature;

import org.nuxeo.labs.hyland.knowledge.enrichment.service.HylandCIService;
import org.nuxeo.labs.hyland.knowledge.enrichment.service.HylandCIServiceImpl;

/**
 * Check the misc expected config parameters are set so a call to Hyland Content Intelligence
 * can be made.
 * also checks for environment variables and convert them to config. parameters, this may be useful
 * when testing quickly, so you can set the following variables:
 * CIC_ENRICHMENT_CLIENT_ID (=> nuxeo.hyland.cic.enrichment.clientId)
 * CIC_ENRICHMENT_CLIENT_SECRET (=> nuxeo.hyland.cic.enrichment.clientSecret)
 * CIC_DATA_CURATION_CLIENT_ID (=> nuxeo.hyland.cic.datacuration.clientId)
 * CIC_DATA_CURATION_CLIENT_SECRET (=> nuxeo.hyland.cic.datacuration.clientSecret)
 * These are obsolete
 * HYLAND_CONTENT_INTELL_URL (correspond to nuxeo.hyland.content.intelligence.baseUrl.)
 * HYLAND_CONTENT_INTELL_HEADER_NAME (=> nuxeo.hyland.content.intelligence.authenticationHeaderName)
 * HYLAND_CONTENT_INTELL_HEADER_VALUE (=> nuxeo.hyland.content.intelligence.authenticationHeaderValue)
 * 
 * @since 2023
 */
public class ConfigCheckerFeature implements RunnerFeature {

    // ========== <obsolete> (but still used for quick tests, so not deleted)
    public static final String ENV_URL = "HYLAND_CONTENT_INTELL_URL";

    public static final String ENV_HEADER_NAME = "HYLAND_CONTENT_INTELL_HEADER_NAME";

    public static final String ENV_HEADER_VALUE = "HYLAND_CONTENT_INTELL_HEADER_VALUE";
    // ========== </obsolete>

    public static final String ENV_CIC_ENRICHMENT_CLIENT_ID = "CIC_ENRICHMENT_CLIENT_ID";

    public static final String ENV_CIC_ENRICHMENT_CLIENT_SECRET = "CIC_ENRICHMENT_CLIENT_SECRET";

    public static final String ENV_CIC_DATA_CURATION_CLIENT_ID = "CIC_DATA_CURATION_CLIENT_ID";

    public static final String ENV_CIC_DATA_CURATION_CLIENT_SECRET = "CIC_DATA_CURATION_CLIENT_SECRET";

    protected static boolean hasEnrichmentClientInfo = false;

    protected static boolean hasDataCurationClientInfo = false;

    Properties systemProps = null;

    public static boolean hasEnrichmentClientInfo() {
        return hasEnrichmentClientInfo;
    }

    public static boolean hasDataCurationClientInfo() {
        return hasDataCurationClientInfo;
    }

    // Original, initial plugin was using a demo test server. Since then, CIC has
    // considerably evolved, API changes (this was expected), authentication changed,
    // etc., but we sill have a quick test here and there.
    // This code should be removed once all is OK with the final, DEV/PROD CIC service.
    protected static boolean hasObsoleteQuickDemoInfo = false;

    public static boolean hasObsoleteQuickDemoInfo() {
        return hasObsoleteQuickDemoInfo;
    }

    @Override
    public void initialize(FeaturesRunner runner) throws Exception {

        systemProps = System.getProperties();

        boolean hasEnrichmentClientId = hasProperty(HylandCIServiceImpl.ENRICHMENT_CLIENT_ID_PARAM,
                ENV_CIC_ENRICHMENT_CLIENT_ID);
        boolean hasEnrichmentClientSecret = hasProperty(HylandCIServiceImpl.ENRICHMENT_CLIENT_SECRET_PARAM,
                ENV_CIC_ENRICHMENT_CLIENT_SECRET);
        hasEnrichmentClientInfo = hasEnrichmentClientId && hasEnrichmentClientSecret;
        if (!hasEnrichmentClientInfo) {
            String msg = "Missing CIC Enrichment Client info => no tests";
            System.out.println(msg);
        }

        boolean hasDataCurationClientId = hasProperty(HylandCIServiceImpl.DATA_CURATION_CLIENT_ID_PARAM,
                ENV_CIC_DATA_CURATION_CLIENT_ID);
        boolean hasDataCurationClientSecret = hasProperty(HylandCIServiceImpl.DATA_CURATION_CLIENT_SECRET_PARAM,
                ENV_CIC_DATA_CURATION_CLIENT_SECRET);
        hasDataCurationClientInfo = hasDataCurationClientId && hasDataCurationClientSecret;
        if (!hasDataCurationClientInfo) {
            String msg = "Missing CIC Data Curation Client info => no tests";
            System.out.println(msg);
        }

        // The obsolete stuff still used in quick tests
        boolean hasUrl = hasProperty(HylandCIService.CONTENT_INTELL_URL_PARAM, ENV_URL);
        boolean hasheaderName = hasProperty(HylandCIService.CONTENT_INTELL_HEADER_NAME_PARAM, ENV_HEADER_NAME);
        boolean hasHeaderValue = hasProperty(HylandCIService.CONTENT_INTELL_HEADER_VALUE_PARAM, ENV_HEADER_VALUE);
        hasObsoleteQuickDemoInfo = hasUrl && hasheaderName && hasHeaderValue;
        if (!hasObsoleteQuickDemoInfo) {
            String msg = "OBSOLETE QUICK DEMO TEST: Missing at least a parameter to connect to Hyland Content Intelligence, ";
            msg += " we need URL and authentication header Name and authentication header Value.";
            System.out.println(msg);
        }

    }

    protected boolean hasProperty(String property, String envVar) {

        String value = systemProps.getProperty(property);

        if (StringUtils.isBlank(value)) {
            value = System.getenv(envVar);
            if (!StringUtils.isBlank(value)) {
                systemProps.put(property, value);
                return true;
            }
        }

        return false;
    }

}
