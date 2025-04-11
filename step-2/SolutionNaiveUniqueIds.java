///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS com.fasterxml.jackson.core:jackson-databind:2.15.2

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Scanner;

/**
 * SolutionNaiveUniqueIds - Naive Approach to Unique ID Generation
 * 
 * This implementation demonstrates a simple but flawed approach to generating
 * unique IDs in a distributed system. Each node maintains an independent counter
 * and uses it to generate IDs without any coordination with other nodes.
 * 
 * IMPORTANT: This solution will fail in a distributed environment because
 * multiple nodes will generate the same IDs. For example, all nodes will
 * independently generate IDs like "1", "2", "3", etc., causing ID collisions.
 * 
 * This code is provided as a pedagogical example of why distributed systems
 * need to consider the challenges of coordination-free algorithms.
 */
public class SolutionNaiveUniqueIds {
    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);
        NaiveUniqueIdServer server = new NaiveUniqueIdServer();
        
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
 * Naive implementation of a unique ID server that doesn't work in a distributed setting.
 * 
 * Problems with this approach:
 * 1. Each node independently increments its own counter starting from 0
 * 2. All nodes will generate the same sequence of IDs (1, 2, 3, etc.)
 * 3. During network partitions, nodes continue to generate potentially conflicting IDs
 * 
 * Contrast this with the solution in SolutionUniqueIds.java, which prefixes 
 * each ID with the node ID to ensure global uniqueness without coordination.
 */
class NaiveUniqueIdServer {
    private final ObjectMapper mapper = new ObjectMapper();
    private String nodeId;
    
    // Simple counter that starts at 0 - this is the root of the problem
    // as all nodes will have their own counters starting at 0
    private int counter = 0;  // Simple counter for IDs
    
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
        counter++;
        
        // Use the counter as the ID without node ID prefix
        String id = String.valueOf(counter);
        
        ObjectNode responseBody = mapper.createObjectNode();
        responseBody.put("type", "generate_ok");
        responseBody.put("in_reply_to", body.get("msg_id").asInt());
        responseBody.put("id", id);
        
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
