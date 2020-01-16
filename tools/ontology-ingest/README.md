
## Ontology Ingest

These OpenLumify modules provide code generation and run time supoort for developing easy to use data ingestion code.

:white_check_mark: Before running the code generation tool, you or your organization will need to have developed and configured an [ontology for OpenLumify](http://docs.openlumify.org/getting-started/ontology.html). This ontology specification may be provided to the code generation tool:

1. from a running instance of OpenLumify using username/password authentication, or
1. saved for offline use by using a web browser to login to OpenLumify
   and then saving the JSON response when accessing https://hostname/ontology


### Setup

We recommend that you configure three Maven modules to support running the code generator and developing ingestion code for OpenLumify:

##### codegen

The `codegen` module is helpful for using your IDE to run the `ModelCodeGen` command line tool. It will not include any source files. Specify the following Maven dependency:

```xml
        <dependency>
            <groupId>org.openlumify</groupId>
            <artifactId>openlumify-tools-ontology-ingest-codegen</artifactId>
            <version>${openlumify.version}</version>
        </dependency>
```

##### generated

The `generated` module will only include the generated source files and will be a dependency of your `ingest` module. Specify the follwing Maven dependency:

```xml
        <dependency>
            <groupId>org.openlumify</groupId>
            <artifactId>openlumify-tools-ontology-ingest-common</artifactId>
            <version>${openlumify.version}</version>
        </dependency>
```

##### ingest

The `ingest` modile will include the source files that you write. It will depend on the `generated` module in addition to any project specific dependancies:

```xml
        <dependency>
            <groupId>${project.groupId}</groupId>
            <artifactId>generated</artifactId>
            <version>${project.version}</version>
        </dependency>
```

### Run `ModelCodeGen`

The `ModelCodeGen` command line tool is used to generate model classes representing the concepts and relationships defined in a OpenLumify ontology.

Run `org.openlumify.tools.ontology.ingest.codegen.ModelCodeGen` in your IDE using the classpath for the `codegen` Maven module.

If using a running instance of OpenLumify provide the following command line arguments:

```bash
        --openlumifyUrl https://hostname
        --openlumifyUsername username
        --openlumifyPassword password
        --outputDirectory /path/to/project/generated/src/main/java
```

If using a saved `.json` file provide the following command line arguments:

```bash
        --inputJsonFile /path/to/saved/ontology.json
        --outputDirectory /path/to/project/generated/src/main/java
```

### Use

The generated code will have package and class names resembling the OpenLumify ontolgy concept and relationship IRIs.

```java
        import com.google.inject.Inject;
        import org.openlumify.tools.ontology.ingest.common.IngestRepository;
        import com.company.project.*;

        class Example {
            @Inject
            private IngestRepository ingestRepository;

            public void example() {
                // Create an Employee providing a value for the vertex id using a natural key if possible.
                // Set any properties defined in the ontology.
                // Optionally specify Metadata, a Timestamp, and/or Visibility for the property.
                //
                Employee dilbert = new Employee("dilbert_vertex_id");
                dilbert.setName("Dilbert");
                dilbert.setJobSatisfaction(0.1).withVisibility("CATBERT_EYES_ONLY");

                // Create a Manager.
                //
                Manager phb = new Manager("phb_vertex_id");
                phb.setName("Pointy-Haired Boss");

                // Create a relationship providing a value for the edge id.
                // The signiture of the constructor will enforce the domain and range specified in the ontology.
                //
                WorksFor worksForRelationship = new WorksFor("dilbert_works_for_phb_edge_id", dilbert, phb);

                // Use the IngestRepository to save the entities and relationships.
                // The repository will validate that the used entities, relationships, and properties
                // are compatible with the ontology of the OpenLumify instance.
                //
                ingestRepository.save(dilbert, phb, worksForRelationship);
            }
        }
```

