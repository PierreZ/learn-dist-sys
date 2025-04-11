///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS com.fasterxml.jackson.core:jackson-databind:2.15.2

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Scanner;

/**
 * SolutionEchoServer - Simple Echo Server for Maelstrom
 * 
 * This solution implements a basic echo server that responds to:
 * 1. init messages - Stores the node ID for later use
 * 2. echo messages - Returns the same message back to the sender
 *
 * This is the foundational pattern for Maelstrom nodes, demonstrating:
 * - Basic message parsing and processing
 * - Request-response pattern handling
 * - Proper message structure with src/dest/body
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

/**
 * Implementation of the echo server functionality.
 * 
 * This class:
 * - Parses JSON messages using Jackson
 * - Handles different message types based on their "type" field
 * - Constructs proper response messages with the appropriate fields
 */
class EchoServerSolution {
    private final ObjectMapper mapper = new ObjectMapper();
    private String nodeId;
    
    /**
     * Handles incoming messages by parsing their JSON content and
     * determining the appropriate response based on the message type.
     * 
     * @param messageJson JSON representation of the incoming message
     * @return Response message as a JSON string, or null if no response is needed
     * @throws Exception If there's an error parsing or processing the message
     */
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
    
    /**
     * Handles init messages by storing the node ID and sending an init_ok response.
     * 
     * @param src Source node ID
     * @param dest Destination node ID
     * @param body Message body containing the node ID
     * @return init_ok response message as a JSON string
     * @throws Exception If there's an error constructing the response
     */
    private String handleInit(String src, String dest, JsonNode body) throws Exception {
        nodeId = body.get("node_id").asText();
        System.err.println("Node " + nodeId + " initialized");
        
        ObjectNode responseBody = mapper.createObjectNode();
        responseBody.put("type", "init_ok");
        responseBody.put("in_reply_to", body.get("msg_id").asInt());
        
        return createResponse(src, responseBody);
    }
    
    /**
     * Handles echo messages by returning the same message back to the sender.
     * 
     * @param src Source node ID
     * @param dest Destination node ID
     * @param body Message body containing the echo message
     * @return echo_ok response message as a JSON string
     * @throws Exception If there's an error constructing the response
     */
    private String handleEcho(String src, String dest, JsonNode body) throws Exception {
        ObjectNode responseBody = mapper.createObjectNode();
        responseBody.put("type", "echo_ok");
        responseBody.put("in_reply_to", body.get("msg_id").asInt());
        responseBody.set("echo", body.get("echo"));
        
        return createResponse(src, responseBody);
    }
    
    /**
     * Creates a response message with the given destination and body.
     * 
     * @param dest Destination node ID
     * @param body Response message body
     * @return Response message as a JSON string
     * @throws Exception If there's an error constructing the response
     */
    private String createResponse(String dest, ObjectNode body) throws Exception {
        ObjectNode response = mapper.createObjectNode();
        response.put("src", nodeId);
        response.put("dest", dest);
        response.set("body", body);
        
        return mapper.writeValueAsString(response);
    }
}
