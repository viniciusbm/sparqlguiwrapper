SPARQL GUI Wrapper
------------------

This is a small tool that aims to provide a user-friendly way to type
SPARQL queries and obtain the results.  All the processing is
performed by [Apache Jena](https://jena.apache.org/).

### Usage

Run the program, choose the OWL ontology file (RDF/XML only, with
inferred axioms), change the port if needed and click *Start*.  Then,
open `localhost` with the chosen port in your local web browser. Type
any query and click *Execute query*. The results will be shown on the
bottom portion of the page.

### Limitations

I wrote this program in a couple of days to fit a specific need.
There is **no** support for query loading and saving, result
exporting, remote resource loading, inference of any sort, or any OWL
formats other than RDF/XML.

### Binaries

A JAR file can be downloaded [here](releases). It comes bundled with
all dependencies, hence no external library is required.

### Compilation

This program can be build with [Maven](https://maven.apache.org/)
(just run `mvn build`, or `mvn package` to obtain the JAR file).

### Licence

The source code and the binary files are licensed under the Apache
License, Version 2.0.

Information about licences for the dependencies can be found
[in the `licence-info` subdirectory](licence-info/).
