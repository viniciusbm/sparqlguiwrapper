package sparqlguiwrapper;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.NoSuchElementException;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.query.ARQ;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryException;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;

public class QueryManager {

    protected OntModel model = null;
    private String ontologyFileName = null;
    private String lastQuery = "";

    public QueryManager() {
        ARQ.init();
    }

    public String check(String queryString) {
        JsonObjectBuilder answerBuilder = Json.createObjectBuilder();
        answerBuilder.add("fileName", ontologyFileName);
        try {
            answerBuilder.add("URI", model.listOntologies().next().getURI());
        } catch (NoSuchElementException | NullPointerException e) {
            answerBuilder.add("URI", "");
        }
        answerBuilder.add("query", queryString);
        if (!queryString.isEmpty()) {
            lastQuery = queryString;
            try {
                QueryFactory.create(queryString);
                answerBuilder.add("error", false);
            } catch (QueryException e) {
                answerBuilder.add("error", e.getMessage());
            }
        }
        return answerBuilder.build().toString();
    }

    public String runQuery(String queryString) throws FileNotFoundException {
        Query query;
        JsonObjectBuilder answerBuilder = Json.createObjectBuilder();
        lastQuery = queryString;
        try {
            query = QueryFactory.create(queryString);
            answerBuilder.add("error", false);
        } catch (QueryException e) {
            return answerBuilder.add("error", e.getMessage()).build().toString();
        }
        JsonArrayBuilder rows = Json.createArrayBuilder();
        try (QueryExecution qexec = QueryExecutionFactory.create(query, model)) {
            ResultSet results = qexec.execSelect();
            List<String> columns = results.getResultVars();
            answerBuilder.add("columns", Json.createArrayBuilder(columns));
            for (; results.hasNext();) {
                QuerySolution solution = results.next();
                JsonArrayBuilder row = Json.createArrayBuilder();
                for (String c : columns) {
                    RDFNode cell = solution.get(c);
                    if (cell == null) {
                        row.add(Json.createObjectBuilder().add("isLiteral", false));
                    } else if (cell.isLiteral()) {
                        Literal l = cell.asLiteral();
                        String type = l.getDatatype().getURI();
                        JsonObjectBuilder ob = Json.createObjectBuilder().add("isLiteral", true).add("type", type);
                        String cn = l.getDatatype().getJavaClass() == null ? "null"
                                : l.getDatatype().getJavaClass().getSimpleName();
                        switch (cn) {
                        case "Character":
                            ob.add("value", l.getChar());
                            break;
                        case "Byte":
                            ob.add("value", l.getByte());
                            break;
                        case "Short":
                            ob.add("value", l.getShort());
                            break;
                        case "Integer":
                            ob.add("value", l.getInt());
                            break;
                        case "Long":
                            ob.add("value", l.getLong());
                            break;
                        case "BigInteger":
                            try {
                                ob.add("value", l.getLong());
                            } catch (NumberFormatException e) {
                                ob.add("value", l.toString());
                            }
                            break;
                        case "Float":
                            ob.add("value", l.getFloat());
                            break;
                        case "Double":
                            ob.add("value", l.getDouble());
                            break;
                        case "String":
                            ob.add("value", l.getString());
                            break;
                        case "Boolean":
                            ob.add("value", l.getBoolean());
                            break;
                        default:
                            ob.add("value", l.toString());
                            break;
                        }
                        row.add(ob);
                    } else {
                        Resource r = cell.asResource();
                        String localName = r.getLocalName();
                        String fullName = r.toString();
                        row.add(Json.createObjectBuilder().add("isLiteral", false).add("localName", localName)
                                .add("fullName", fullName));
                    }
                }
                rows.add(row);
            }
        }
        answerBuilder.add("results", rows);
        return answerBuilder.build().toString();
    }

    public void setOntologyFileName(String fileName) throws FileNotFoundException {
        if (fileName == null) {
            model = null;
            ontologyFileName = null;
            return;
        }
        try {
            OntModel m = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
            m.read(new FileInputStream(fileName), null);
            model = m;
        } catch (FileNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("The ontology file could not be parsed. Please use RDF/XML format.");
        }
        this.ontologyFileName = fileName;
    }

    public String getOntologyFileName() {
        return ontologyFileName;
    }

    public String getLastQuery() {
        if (lastQuery == null)
            return "";
        return lastQuery;
    }

    public String setLastQuery(String lastQuery) {
        return this.lastQuery = lastQuery;
    }

}
