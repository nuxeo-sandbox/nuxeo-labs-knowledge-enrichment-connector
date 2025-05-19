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

1. Have a valid application on Content Intelligence Cloud/Content Innovation Cloud. also look at its documentation. You need valid endpoints (authentication, content intelligence, data curation), and valid clientId and clientSecret.

2. Setup the configuration parameters required by the plugin
3. From Nuxeo Studio, create an Automation Script that calls the operation(s), then handle the JSON result. From this result, you will typically save values in fields.

The returned JSON is formated as follow. It encapsulates a couple information about the call to the service (HTTP Result code) and the response as returned by the service. This response is returned "as is", no modification is applied by the plugin, and it is stored the `"response"` property of the result.

```
{
    "responseCode": Integer, the HTTP code returned by the service. Should be 200,
    "responseMessage": String, the HTTP message returned by the service. Should be "OK",
    "response": A JSON object with the following fields (see Knowledge Enrichment API doumentation)
    {
        "id": String, the ID of the response
        "timestamp": String, the date of the response
        "status": String, "SUCCESS", "FAILURE" or "PARTIAL_FAILURE"
        "results": An array of responses, with only one element for now:
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
                . . . other responses, null if they were not requested. For example:
                "metadata": null,
                "textSummary": null,
                ...etc...

            }
        ]
    }
}
```

For details about each result, please see the Knowledge Enrichment API and schemas. You can also look at some unit tests in the plugin and some examples below.


> [!IMPORTANT]
> **You should always check the `responseCode` is 200** before trying to get other fields.

See examples of Automation Script below


## Nuxeo Configuration Parameters

For calling the service, you need to setup configuration parameters in nuxeo.conf.

Values

* `nuxeo.hyland.cic.endpoint.auth`: The authentication endpoint. The plugin adds the "/connect/token" final path. So your URL is something like https://auth.etc.etc.hyland.com/idp
* `nuxeo.hyland.cic.endpoint.contextEnrichment`: The enrichment endpoint.
* `nuxeo.hyland.cic.endpoint.dataCuration`: The Data Curation endpoint.
* `nuxeo.hyland.cic.enrichment.clientId`: Your enrichment clientId
* `nuxeo.hyland.cic.enrichment.clientSecret`: Your enrichment client secret
* `nuxeo.hyland.cic.datacuration.clientId`: Your data curation clientId
* `nuxeo.hyland.cic.datacuration.clientSecret`: Your data curation client secret

Other parameters are used to tune the behavior:
* As of now, getting the results is asynchronous and we need to poll and check if they are ready. The following parameters are used in a loop, where if the service does not return HTTP Code 200, the thread sleeps a certain time then tries again, until a certain number of tries:
  * `nuxeo.hyland.cic.pullResultsMaxTries`, an intergern max number of tries. Default value is `10`.
  * `nuxeo.hyland.cic.pullResultsSleepIntervall`: an integer, the sleep value in milliseconds. Default value is 3000
  
  So, with these default values, the code will try maximum 10 times and it will take about 30s max.

At startup, if some paraleters are missing, the plugin logs a WARN. For example, if you do not provide data curation clientId:

```
WARN  [main] [org.nuxeo.labs.hyland.knowledge.enrichment.service.HylandKEServiceImpl] No CIC Data Curation ClientId provided (nuxeo.hyland.cic.datacuration.clientId), calls to the service will fail.
```

## Authentication to the Service

This part is always handled by the plugin, using the different info provided in the configuration parameters (auth. end point + clientId + clientSecret).

The service returns a token valid a certain time: The plugin handles this timeout (so as to avoid requesting a token at each call, saving some loads)


## Operations

### `HylandKnowledgeEnrichment.Enrich`

A high level operation that handles all the different calls to the services (get a token -> get a presigned URL -> upload the file -> call for "process actions" -> get the result)

* Input: `blob`
* Output: `Blob`, a JSON blob
* Parameters
  * `actions`: String required. A list of comma separated actions to perform. See KE documentation about available actions
  * `classes`: String, optional.  A list of comma separated classes, to be used with some classification actions (can be ommitted or null for other actions)
  * `similarMetadata`: String, optional.  A list of comma separated similarMetadata, to be used with some metadata actions (can be ommitted or null for other actions)

> [!NOTE]
> Again, please, see Knowledge Enrichment API documentation for details on the values that can be used/passed.

The operation calls the service and returns a JSON Blob, that contains the object described above (See Usage).

> [!NOTE]
> Reminder: To get the JSON string from this blob, you must call its `getString()` method (see examples below). Then you can `JSON.parse` this string

#### Example: Get an image Description, store in `dc:description`

In the example, we check the input document behaves as a `Picture`, and we sent the jpeg rendition (we don't want to send the main file, which could be a 300MB Photoshop file)

```javascript
// input: document, output: document
function run(input, params) {
  
  var jpeg, result, resultJson, response, serviceResult,
      descriptionObj, description;
  
  if(!input.hasFacet("Picture")) {
    Console.warn("Input doc should have the Picture facet");
    return input;
  }
    
  // Get the jpeg rendition
  jpeg = Picture.GetView(input, {'viewName': 'FullHD'});
  if(!jpeg) {
    // We've got a problem.
    Console.error("No jpeg rendition found for the input document.");
    return input;
  }
  
  // Call the service
  result = HylandKnowledgeEnrichment.Enrich(
    jpeg, {
      'actions': "image-description",
      // "classes": not used here. Could be passed "" or null,
      // "similarMetadata": not used here Could be passed "" or null
    }
  );
  
  // Do not forget to use the getString() method :-)
  resultJson = JSON.parse(result.getString());
  
  if(resultJson.responseCode !== 200) {
    Console.error("Error calling the service:\n " + JSON.stringify(resultJson, null, 2));
  } else {
    response = resultJson.response;
    
    // For the JSON returned by CIC/KE, please see the documentation/schemas/tech info
    // at https://hyland.github.io/ContentIntelligence-Docs/KnowledgeEnrichment
    // Also, just log the result and look at what was returned. For example:
    // Console.log("RESULT\n" + JSON.stringify(resultJson, null, 2));
    
    if(response.status !== "SUCCESS") {
      // (we could handle PARTIAL FAILURE. It means for example getting
      // a description worked, getitng the embeddings failed)
      Console.error("Error calling the service:\n " + JSON.stringify(resultJson, null, 2));
    } else {
      serviceResult = response.results[0]; // Only one for now
      
      // Get the description.
      // As status is SUCCESS, no need to check descriptionObj.isSuccess 
      descriptionObj = serviceResult.imageDescription;
      // Save in dc:description
      input["dc:description"] = descriptionObj.result;
      
      input = Document.Save(input, {});
    }
  }
  
  return input;
  
}
```

#### Example: Get an image Description + image Embeddings, store in `dc:description` and in a custom `embeddings:image` field

```javascript
function run(input, params) {
  
  var jpeg, result, resultJson, response, serviceResult,
      descriptionObj, description, embeddingsObj, embeddings;
  
  if(!input.hasFacet("Picture")) {
    Console.warn("Input doc should have the Picture facet");
    return input;
  }
  
  // Get the jpeg rendition
  jpeg = Picture.GetView(input, {'viewName': 'FullHD'});
  if(!jpeg) {
    // We've got a problem.
    Console.error("No jpeg rendition found for the input document.");
    return input;
  }
  
  // Call the service
  result = HylandKnowledgeEnrichment.Enrich(
    jpeg, {
      'actions': "image-description,image-embeddings",
      // "classes": not used here. Could be passed "" or null,
      // "similarMetadata": not used here Could be passed "" or null
    }
  );
  
  // Do not forget to use the getString() method :-)
  resultJson = JSON.parse(result.getString());
  
  if(resultJson.responseCode !== 200) {
    Console.error("Error calling the service:\n " + JSON.stringify(resultJson, null, 2));
  } else {
    response = resultJson.response;
    
    // For the JSON returned by CIC/KE, please see the documentation/schemas/tech info
    // at https://hyland.github.io/ContentIntelligence-Docs/KnowledgeEnrichment
    // Also, just log the result and look at what was returned. For example:
    // Console.log("RESULT\n" + JSON.stringify(resultJson, null, 2));
    
    if(response.status !== "SUCCESS") {
      // (we could handle PARTIAL FAILURE. It means for example getting
      // a description worked, getitng the embeddings failed)
      Console.error("Error calling the service:\n " + JSON.stringify(resultJson, null, 2));
    } else {
      serviceResult = response.results[0]; // Only one for now
      
      // Get the description.
      // As status is SUCCESS, no need to check descriptionObj.isSuccess 
      descriptionObj = serviceResult.imageDescription;
      // Save in dc:description
      input["dc:description"] = descriptionObj.result;
      
      // Get the embeddings
      embeddingsObj = serviceResult.imageEmbeddings;
      embeddings = embeddingsObj.result;
      // Save in custom fields embeddings:image
      // In this example, we have the custom schema, embeddings,
      // and the custom Facet, Embeddings, that references the schema.
      if(!input.hasFacet("Embeddings")) {
        input = Document.AddFacet(
          input, {
            'facet': "Embeddings",
            'save': false 
          });
      }
      input["embeddings:image"] = embeddingsObj.result;
      
      input = Document.Save(input, {});
    }
  }
  
  return input;
  
}
```

#### Example: Classification of a text file

("Text file" here means it's a pdf, typically)

```javascript
function run(input, params) {
  
  var blob, result, resultJson, response, serviceResult,
      classificationObj, classification;
    
  blob = input["file:content"];
  if(!blob) {
    // We've got a problem.
    Console.error("No blob in the input document.");
    return input;
  }

  // Call the service
  // With "text-classification", we must pass at least 2 values in "classes"
  result = HylandKnowledgeEnrichment.Enrich(
    blob, {
      'actions': "text-classification",
      "classes": "Contract, Invoice, Report, Policy, Resume"
      // "similarMetadata": not used here Could be passed "" or null
    }
  );
  
  // Do not forget to use the getString() method :-)
  resultJson = JSON.parse(result.getString());
  
  Console.log("Calling the service, response code: " + resultJson.responseCode);
  if(resultJson.responseCode !== 200) {
    Console.error("Error calling the service:\n " + JSON.stringify(resultJson, null, 2));
  } else {
    response = resultJson.response;
    if(response.status !== "SUCCESS") {
      // (we could handle PARTIAL FAILURE. It means for example getting
      // a description worked, getitng the embeddings failed)
      Console.error("Error calling the service:\n " + JSON.stringify(resultJson, null, 2));
    } else {
      serviceResult = response.results[0]; // Only one for now
      
      // Get the classification.
      // As status is SUCCESS, no need to check descriptionObj.isSuccess 
        classificationObj = serviceResult.textClassification;
      // Save in dc:description for the test
      classification = classificationObj.result;
      input["dc:description"] = "Classification returned: " + classification;
      
      input = Document.Save(input, {});
    }
  }
  
  return input;
  
}
```

### `HylandKnowledgeEnrichment.Invoke`

[to be done]


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

Please, see documentation at `ConfigCheckerFeature.class`. Basically, you can setup environement variables to be used as configuration parameters:

* `CIC_ENRICHMENT_CLIENT_ID`
* `CIC_ENRICHMENT_CLIENT_SECRET`
* `CIC_DATA_CURATION_CLIENT_ID`
* `CIC_DATA_CURATION_CLIENT_SECRET`

Thenn run the unit tests (or the full build).


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
