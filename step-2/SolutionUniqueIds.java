///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS com.fasterxml.jackson.core:jackson-databind:2.15.2

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Scanner;

/**
 * SolutionUniqueIds - Distributed Unique ID Generator
 * 
 * This solution implements a globally unique ID generator that:
 * - Works across multiple nodes in a distributed system
 * - Functions correctly even during network partitions
 * - Maintains availability (each node can generate IDs independently)
 * 
 * The implementation uses the node-prefix strategy: combining the node's
 * unique ID (e.g., "n1") with a local counter to ensure IDs are globally
 * unique without requiring coordination between nodes.
 * 
 * For example: "n1-1", "n1-2", "n2-1", "n3-1", etc.
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
 * Server implementation for generating unique IDs in a distributed system.
 * 
 * Key aspects of this implementation:
 * 1. Uses a node-specific prefix (nodeId) to guarantee global uniqueness
 * 2. Maintains a local counter for sequential IDs within this node
 * 3. Requires no coordination with other nodes
 * 4. Remains available even during network partitions
 * 
 * This approach trades off ID compactness for availability and partition tolerance,
 * embodying the AP side of the CAP theorem.
 */
class UniqueIdServer {
    private final ObjectMapper mapper = new ObjectMapper();
    private String nodeId;
    
    // Counter for local sequential IDs, combined with nodeId for uniqueness
    private int lastId = 0;
    
    /**
     * Logs a debug message to STDERR.
     * 
     * IMPORTANT: Maelstrom protocol requires all debug output to go to STDERR.
     * Never use System.out for logging as it will corrupt the message protocol.
     * 
     * @param message The debug message to log
     */
    private void debug(String message) {
        System.err.println("[" + (nodeId != null ? nodeId : "uninit") + "] " + message);
    }
    
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
            debug("Unknown message type: " + type);
            return null;
        }
    }
    
    private String handleInit(String src, String dest, JsonNode body) throws Exception {
        nodeId = body.get("node_id").asText();
        debug("Node " + nodeId + " initialized");
        
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
        debug("Generated unique ID: " + uniqueId);
        
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
