# Nuxeo JavaScript Automation, examples using nuxeo-labs-knowledge-enrichment-connector

These examples are referenced from the README of the plugin.

> [!IMPORTANT]
> All the examples assume Nuxeo was correctly configured to access Hyland CIC Knowledge Enrichment service. See README > Nuxeo Configuration Parameters

## Examples Using `HylandKnowledgeEnrichment.Enrich`

### Get an image Description, store in `dc:description`

In the example, we check the input document behaves as a `Picture`, and we send its jpeg rendition (we don't want to send the main file, which could be a 300MB Photoshop file)

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
      // a description worked, getting the embeddings failed)
      Console.error("Error calling the service:\n " + JSON.stringify(resultJson, null, 2));
    } else {
      serviceResult = response.results[0]; // Only one since we sent only one file
      
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

### Get an image Description + and image Embeddings, store in `dc:description` and in a custom `embeddings:image` field

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
      // a description worked, getting the embeddings failed)
      Console.error("Error calling the service:\n " + JSON.stringify(resultJson, null, 2));
    } else {
      serviceResult = response.results[0]; // Only one since we sent only one file
      
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

### Classification of a text file

("Text file" here means it's a pdf)

```javascript
function run(input, params) {
  
  var blob, result, resultJson, response, serviceResult,
      classificationObj, classification,;
    
  blob = input["file:content"];
  if(!blob) {
    // We've got a problem.
    Console.error("No blob in the input document.");
    return input;
  }

  // As of today (June 2025, the service only accepts PDF => Convert of needed
  if(blob.getMimeType() !== "application/pdf") {
    blob = Blob.ToPDF(blob, {});
  }
  
  pdf = Blob.ToPDF(blob, {});

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
      serviceResult = response.results[0]; // Only one since we sent only one file
      
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


## Example(s) Using `HylandKnowledgeEnrichment.EnrichSeveral`

### getting Image Description of several Picture Documents

Say we have a list of Picture, for example, after a query. We want the image-description for each of them, calling in a batch.

```javascript
function run(input, params) {
  
  var docs, resultDocs, oneDoc, blobs, jpeg, blob, i, sourceIds,
      result, resultJson, results,
      objectKeysMapping, response, objKey, sourceId;
  
  // ... docs is a list of Picture filled previously...
  // Fill sourceIds and blobs
  // We will send the jpeg rendition for each
  sourceIds = [];
  blobs = [];
  for(i = 0; i < docs.size(); i++) {
    oneDoc = docs[i];
    sourceIds.push(oneDoc.id);
    
    jpeg = Picture.GetView(input, {'viewName': 'FullHD'});
    blobs.push(jpeg);
    
  }
  
  // Initialize result
  resultDocs = [];
  
  // Call operation
  result = HylandKnowledgeEnrichment.EnrichSeveral(
    blobs, {
      'actions': "image-description",
      // "classes": not used here. Could be passed "" or null,
      // "similarMetadataJsonArrayStr": not used here Could be passed "" or null
      'sourceIds': sourceIds.join(),
      //'xpath': 
    });

  // Do not forget to use the getString() method :-)
  resultJson = JSON.parse(result.getString());
  
  Console.log("Calling the service, response code: " + resultJson.responseCode);
  if(resultJson.responseCode !== 200) {
    Console.error("Error calling the service:\n " + JSON.stringify(resultJson, null, 2));
  } else {
    // Get the array of mappings
    objectKeysMapping = resultJson.objectKeysMapping;
    
    // Get the array of results
    response = resultJson.response;
    if(response.status !== "SUCCESS") {
      // (we could handle PARTIAL FAILURE. It means for example getting
      // a description worked, getitng the embeddings failed)
      Console.error("Error calling the service:\n " + JSON.stringify(resultJson, null, 2));
    } else {
      // Loop the results
      resultJson.results.forEach(function(oneResult) {
        // description.
        descriptionObj = oneResult.imageDescription;
        // ... if handling PARTIAL_FAILURE, you shoudl check descriptionObj.isSuccess...
        
        // Get the key
        objKey = oneResult.objectKey;
        // deduce the source doc ID
        sourceId = getSourceIdByObjectKey(objectKeysMapping, objKey);
        if(!sourceId) {
          Console.warn("sourceId not found for objectKey " + objKey);
        } else {
          // Load the Document
          oneDoc = Repository.GetDocument(null, {'value': sourceid});
          
          // Set-up description
          oneDoc["dc:description"] = descriptionObj.result;
          
          // Save
          oneDoc = Document.Save(oneDoc, {});
          resultDocs.push(oneDoc);
        }
        
      });
    }
  }
  
  return resultDocs;

}

// Can't us ARRAY.find() of ECMAScript6, halas.
function getSourceIdByObjectKey(objectKeysMapping, targetKey) {
  for (var i = 0; i < objectKeysMapping.length; i++) {
    if (objectKeysMapping[i].objectKey === targetKey) {
      return objectKeysMapping[i].sourceId;
    }
  }
  return null;
}
```


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
