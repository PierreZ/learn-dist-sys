///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS com.fasterxml.jackson.core:jackson-databind:2.15.2

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Scanner;

/**
 * Solution implementation for the echo server challenge.
 * This file contains the complete implementation that properly handles
 * both init and echo message types.
 */
public class SolutionEchoServer {
    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);
        EchoServerSolution server = new EchoServerSolution();
        
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            try {
                String response = server.handleMessage(line);
                if (response != null) {
                    System.out.println(response);
                }
            } catch (Exception e) {
                System.err.println("Error processing message: " + e.getMessage() + "\nInput was: " + line);
            }
        }
    }
}

class EchoServerSolution {
    private final ObjectMapper mapper = new ObjectMapper();
    private String nodeId;
    
    public String handleMessage(String messageJson) throws Exception {
        JsonNode message = mapper.readTree(messageJson);
        String src = message.get("src").asText();
        String dest = message.get("dest").asText();
        JsonNode body = message.get("body");
        String type = body.get("type").asText();
        
        if (type.equals("init")) {
            return handleInit(src, dest, body);
        } else if (type.equals("echo")) {
            return handleEcho(src, dest, body);
        } else {
            System.err.println("Unknown message type: " + type);
            return null;
        }
    }
    
    private String handleInit(String src, String dest, JsonNode body) throws Exception {
        nodeId = body.get("node_id").asText();
        System.err.println("Node " + nodeId + " initialized");
        
        ObjectNode responseBody = mapper.createObjectNode();
        responseBody.put("type", "init_ok");
        responseBody.put("in_reply_to", body.get("msg_id").asInt());
        
        return createResponse(src, responseBody);
    }
    
    private String handleEcho(String src, String dest, JsonNode body) throws Exception {
        ObjectNode responseBody = mapper.createObjectNode();
        responseBody.put("type", "echo_ok");
        responseBody.put("in_reply_to", body.get("msg_id").asInt());
        responseBody.set("echo", body.get("echo"));
        
        return createResponse(src, responseBody);
    }
    
    private String createResponse(String dest, ObjectNode body) throws Exception {
        ObjectNode response = mapper.createObjectNode();
        response.put("src", nodeId);
        response.put("dest", dest);
        response.set("body", body);
        
        return mapper.writeValueAsString(response);
    }
}
