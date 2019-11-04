package sparqlguiwrapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.Map;

import javax.json.Json;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.Response.Status;

public class Server extends NanoHTTPD {

    private QueryManager qm;

    public Server(int port, QueryManager qm) throws IOException {
        super(port);
        start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
        this.qm = qm;
    }

    @Override
    public Response serve(String uri, Method method, Map<String, String> header, Map<String, String> parameters,
            Map<String, String> files) {
        if (method == Method.GET) {
            if (uri.compareTo("/get-last-query") == 0) {
                return newFixedLengthResponse(Status.OK, "application/json",
                        Json.createObjectBuilder().add("query", qm.getLastQuery()).build().toString());
            } else {
                if (uri.compareTo("/") == 0)
                    uri = "/index.htm";
                InputStream resource = Server.class.getResourceAsStream("/static" + uri);
                String r;
                try {
                    r = new String(resource.readAllBytes());
                } catch (IOException | NullPointerException e) {
                    e.printStackTrace();
                    return newFixedLengthResponse(Status.NOT_FOUND, "", "");
                }
                String mime;
                switch (uri.substring(uri.lastIndexOf('.') + 1)) {
                case "css":
                    mime = "text/css";
                    break;
                case "htm":
                case "html":
                    mime = "text/html";
                    break;
                case "js":
                    mime = "application/javascript";
                    break;
                default:
                    mime = "text/plain";
                }
                return newFixedLengthResponse(Status.OK, mime, r);
            }
        } else if (method == Method.POST) {
            switch (uri) {
            case "/run-query":
                try {
                    if (qm.getOntologyFileName() == null) {
                        return newFixedLengthResponse(Status.INTERNAL_ERROR, "text/plain",
                                "Please choose the ontology file on the program and try again.");
                    }
                    String data = files.get("postData");
                    String query = Json.createReader(new StringReader(data)).readObject().getString("query");
                    String result = qm.runQuery(query);
                    return newFixedLengthResponse(Status.OK, "application/json", result);
                } catch (Exception e) {
                    e.printStackTrace();
                    return newFixedLengthResponse(Status.BAD_REQUEST, "", "");
                }
            case "/ping":
                try {
                    if (qm.getOntologyFileName() == null) {
                        return newFixedLengthResponse(Status.INTERNAL_ERROR, "text/plain",
                                "Please choose the ontology file on the program and try again.");
                    }
                    String data = files.get("postData");
                    String query = Json.createReader(new StringReader(data)).readObject().getString("query");
                    String result = qm.check(query);
                    return newFixedLengthResponse(Status.OK, "application/json", result);
                } catch (Exception e) {
                    e.printStackTrace();
                    return newFixedLengthResponse(Status.BAD_REQUEST, "", "");
                }
            default:
                return newFixedLengthResponse(Status.NOT_FOUND, "", "");
            }
        }
        return newFixedLengthResponse(Status.METHOD_NOT_ALLOWED, "", "");
    }

}
