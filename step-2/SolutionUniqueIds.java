///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS com.fasterxml.jackson.core:jackson-databind:2.15.2

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Scanner;

/**
 * Solution implementation for the unique ID generator.
 * This implementation uses node ID prefixing to ensure globally unique IDs
 * even during network partitions.
 */
public class SolutionUniqueIds {
    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);
        UniqueIdServer server = new UniqueIdServer();
        
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

/**
 * Server implementation that handles the Maelstrom protocol and generates
 * unique IDs by prefixing the node ID to a local counter.
 */
class UniqueIdServer {
    private final ObjectMapper mapper = new ObjectMapper();
    private String nodeId;
    private int lastId = 0;  // Counter for IDs
    
    public String handleMessage(String messageJson) throws Exception {
        JsonNode message = mapper.readTree(messageJson);
        String src = message.get("src").asText();
        String dest = message.get("dest").asText();
        JsonNode body = message.get("body");
        String type = body.get("type").asText();
        
        if (type.equals("init")) {
            return handleInit(src, dest, body);
        } else if (type.equals("generate")) {
            return handleGenerate(src, dest, body);
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
    
    private String handleGenerate(String src, String dest, JsonNode body) throws Exception {
        // Increment the counter for each request
        lastId++;
        
        // Create a unique ID using node ID as prefix
        String uniqueId = nodeId + "-" + lastId;
        
        ObjectNode responseBody = mapper.createObjectNode();
        responseBody.put("type", "generate_ok");
        responseBody.put("in_reply_to", body.get("msg_id").asInt());
        responseBody.put("id", uniqueId);
        
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
