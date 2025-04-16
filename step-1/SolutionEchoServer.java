///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS com.fasterxml.jackson.core:jackson-databind:2.15.2

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Scanner;

/**
 * SolutionEchoServer - Complete implementation of a Maelstrom Echo server
 * 
 * This solution implements a server that:
 * 1. Handles initialization messages from Maelstrom
 * 2. Responds to echo requests by echoing back the message
 * 
 * This is the simplest possible distributed system, demonstrating the
 * basic request-response pattern and JSON message format of Maelstrom.
 */
public class SolutionEchoServer {
    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);
        EchoServerSolution server = new EchoServerSolution();
        
        // Main loop: read messages from STDIN and process them
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            try {
                String response = server.handleMessage(line);
                if (response != null) {
                    // All protocol messages must go to STDOUT
                    System.out.println(response);
                }
            } catch (Exception e) {
                // All error logging must go to STDERR
                System.err.println("Error processing message: " + e.getMessage() + "\nInput was: " + line);
            }
        }
    }
}

/**
 * Implementation of the Echo server that handles the Maelstrom protocol
 * and implements the echo functionality.
 */
class EchoServerSolution {
    // Jackson mapper for JSON serialization/deserialization
    private final ObjectMapper mapper = new ObjectMapper();
    private String nodeId;
    
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
    
    /**
     * Processes incoming JSON messages from Maelstrom.
     * 
     * @param messageJson The raw JSON message string
     * @return A response message or null if no response is needed
     */
    public String handleMessage(String messageJson) throws Exception {
        // Parse the message JSON
        JsonNode message = mapper.readTree(messageJson);
        String src = message.get("src").asText();
        String dest = message.get("dest").asText();
        JsonNode body = message.get("body");
        String type = body.get("type").asText();
        
        // Route to the appropriate handler based on message type
        if (type.equals("init")) {
            return handleInit(src, dest, body);
        } else if (type.equals("echo")) {
            return handleEcho(src, dest, body);
        } else {
            debug("Unknown message type: " + type);
            return null;
        }
    }
    
    /**
     * Handles the initialization message from Maelstrom.
     * The init message provides this node's ID and the IDs of all nodes in the cluster.
     */
    private String handleInit(String src, String dest, JsonNode body) throws Exception {
        // Store our node ID for future use
        nodeId = body.get("node_id").asText();
        debug("Node " + nodeId + " initialized");
        
        // Create and send the init_ok response
        ObjectNode responseBody = mapper.createObjectNode();
        responseBody.put("type", "init_ok");
        responseBody.put("in_reply_to", body.get("msg_id").asInt());
        
        return createResponse(src, responseBody);
    }
    
    /**
     * Handles echo requests by echoing back the 'echo' field from the request body.
     */
    private String handleEcho(String src, String dest, JsonNode body) throws Exception {
        // Extract the echo value and message ID
        String echo = body.get("echo").asText();
        int msgId = body.get("msg_id").asInt();
        debug("Received echo request with value: " + echo);
        
        // Create and send the echo_ok response with the same echo value
        ObjectNode responseBody = mapper.createObjectNode();
        responseBody.put("type", "echo_ok");
        responseBody.put("in_reply_to", msgId);
        responseBody.put("echo", echo);
        
        return createResponse(src, responseBody);
    }
    
    /**
     * Creates a response message with the given destination and body.
     */
    private String createResponse(String dest, ObjectNode body) throws Exception {
        ObjectNode response = mapper.createObjectNode();
        response.put("src", nodeId);
        response.put("dest", dest);
        response.set("body", body);
        
        return mapper.writeValueAsString(response);
    }
}
