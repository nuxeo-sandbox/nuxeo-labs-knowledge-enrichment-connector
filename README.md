# nuxeo-labs-knowledge-enrichment-connector

A plugin that connects to [**Hyland Content Intelligence**](https://www.hyland.com/en/solutions/products/hyland-content-intelligence) and leverages its [**Knowledge Enrichment**](https://hyland.github.io/ContentIntelligence-Docs/KnowledgeEnrichment) APIs.

> [!IMPORTANT]
> The first release handles only the enrichment API, not the Data Curation API.

The plugin provides two kinds of operations handling the calls to the service (see details for each operation below):

* A low-level operation, `HylandKnowledgeEnrichment.Invoke`, that calls the service and returns the JSON response without adding any logic. This is for flexibility: When/if Hyland Content Intelligence adds new endpoints, and/or adds/changes endpoint expected payload, no need to wait for a new version the plugin, just modify the caller (in most of our usages, Nuxeo Studio project and JavaScript Automation).
* A high-level operation, `HylandKnowledgeEnrichment.Enrich` that performs all the different individual calls required to get the enrichement for a blob: Get a presigned URL, then upload the file, etc. 

> [!NOTE]
> The plugin handles the authentication token, you never need to handle it (see below).


## Usage

1. Setup the configuration parameters required by the plugin
2. From Nuxeo Studio, create an Automation Script that calls the operation(s), then handle the JSON result. From this result, you will typically save values in fields.

The returned JSON is formated as follow:

```
{
    "responseCode": Integer, the HTTP code returned by the service,
    "responseMessage": String, the HTTP message returned by the service,
    "response": A JSON object with the following fields:
    {
        "id": String, the ID of the response
        "timestamp": String, the date of the response
        "status": String, "SUCCESS", "FAILURE" or "PARTIAL_FAILURE"
        "results": An array of response, with only one element for now:
        [
            {
                "objectKey": String, the object key (as returned by the getPresignedUrl endpoint),
                "imageDescription": {
                    "isSuccess": boolean,
                    "result": String, the description
                },
                "imageEmbeddings": {
                    "isSuccess": boolean,
                    "result": array of doubles
                },
                . . . etc . . .
            }
        ]
    }
}
```
For details about each result, please see the Knowledge Enrichment API and schemas. You can also look at some unit tests in the plugin and some examples below.


> [!IMPORTANT]
> **You should always check the `responseCode` is 200** before trying to get other fields.

See example of automation script(s) below


### Nuxeo Configuration Parameters

For calling the service, you need to setup configuration parameters in nuxeo.conf. The plugin provides some default values, but it is recommanded to set all the values so as to make sure your call to the service will work as expected.

* `nuxeo.hyland.cic.endpoint.auth`: The authentication endpoint. A default value is provided if this parameter is empty.
* `nuxeo.hyland.cic.endpoint.contextEnrichment`: The enricgment endpoint. A default value is provided if this parameter is empty.
* `nuxeo.hyland.cic.endpoint.dataCuration`: The Data Curation endpoint. A default value is provided if this parameter is empty.
* `nuxeo.hyland.cic.enrichment.clientId`: Your enrichment clientId
* `nuxeo.hyland.cic.enrichment.clientSecret`: Your enrichment client secret
* `nuxeo.hyland.cic.datacuration.clientId`: Your data curation clientId
* `nuxeo.hyland.cic.datacuration.clientSecret`: Your data curation client secret

Other parameters are used to tune the behavior:
* As of now, getting the results is asynchronous and we need to poll and check if they are ready. The following parameters are used in a loop, where if the service does not return HTTP Code 200, the thread sleeps a certain time then tries again, until a certain number of tries:
  * `nuxeo.hyland.cic.pullResultsMaxTries`, an intergern max number of tries. Default value is `10`.
  * `nuxeo.hyland.cic.pullResultsSleepIntervall`: an integer, the sleep value in milliseconds. Default value is 3000
  
  So, with these default values, the code will try maximum 10 times and it will take about 30s max.

### The ... Operation

[To Be Continued]

### Example

[To Be Continued]

## How to build
```bash
git clone https://github.com/nuxeo-sandbox/nuxeo-hyland-knowledge-enrichment-connector
cd nuxeo-hyland-knowledge-enrichment-connector
mvn clean install
```

You can add the `-DskipDocker` flag to skip building with Docker.

Also you can use the `-DskipTests` flag.

> [!NOTE]
> The Marketplace Package ID is `nuxeo-labs-knowledge-enrichment-connector`, not `nuxeo-hyland-knowledge-enrichment-connector`


### How to UnitTest

[To Be Done]


## Support
**These features are not part of the Nuxeo Production platform.**

These solutions are provided for inspiration and we encourage customers to use them as code samples and learning
resources.

This is a moving project (no API maintenance, no deprecation process, etc.) If any of these solutions are found to be
useful for the Nuxeo Platform in general, they will be integrated directly into platform, not maintained here.


## License
[Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.html)


## About Nuxeo
Nuxeo Platform is an open source Content Services platform, written in Java. Data can be stored in both SQL & NoSQL
databases.

The development of the Nuxeo Platform is mostly done by Nuxeo employees with an open development model.

The source code, documentation, roadmap, issue tracker, testing, benchmarks are all public.

Typically, Nuxeo users build different types of information management solutions
for [document management](https://www.nuxeo.com/solutions/document-management/), [case management](https://www.nuxeo.com/solutions/case-management/),
and [digital asset management](https://www.nuxeo.com/solutions/dam-digital-asset-management/), use cases. It uses
schema-flexible metadata & content models that allows content to be repurposed to fulfill future use cases.

More information is available at [www.nuxeo.com](https://www.nuxeo.com).
