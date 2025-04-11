///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS com.fasterxml.jackson.core:jackson-databind:2.15.2

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Scanner;

/**
 * Echo - Simple Maelstrom node that echoes back received messages
 * 
 * This implementation demonstrates the basics of the Maelstrom protocol,
 * including message handling and the request-response pattern.
 * 
 * Remember:
 * - All messages to Maelstrom must be sent to STDOUT
 * - All debug logging must be sent to STDERR
 * - Never mix protocol messages and debug output on the same stream
 */
public class Echo {
    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);
        EchoServer server = new EchoServer();
        
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

class EchoServer {
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
    
    // This method processes incoming messages by parsing the JSON string, 
    // extracting the src, dest, body, and message type, and calling the 
    // appropriate handler based on the message type.
    // 
    // @param messageJson The incoming message as a JSON string
    // @return The response string or null if no response is needed
    // @throws Exception If an error occurs during message processing
    public String handleMessage(String messageJson) throws Exception {
        // Parse the message and call the appropriate handler based on message type
        return null;
    }
    
    // This method handles init messages by extracting the node_id from the body, 
    // storing it in the nodeId field, creating a response body with type "init_ok" 
    // and in_reply_to the original message ID, and returning the response using 
    // createResponse().
    // 
    // @param src The source node ID
    // @param dest The destination node ID
    // @param body The message body as a JsonNode
    // @return The response string
    // @throws Exception If an error occurs during message handling
    private String handleInit(String src, String dest, JsonNode body) throws Exception {
        // Store the node ID and return an init_ok response
        return null;
    }
    
    // This method handles echo messages by creating a response body with type 
    // "echo_ok", in_reply_to the original message ID, and echo the same value 
    // from the request's echo field, and returning the response using createResponse().
    // 
    // @param src The source node ID
    // @param dest The destination node ID
    // @param body The message body as a JsonNode
    // @return The response string
    // @throws Exception If an error occurs during message handling
    private String handleEcho(String src, String dest, JsonNode body) throws Exception {
        // Create and return an echo_ok response with the echo field from the request
        return null;
    }
    
    // TODO: Implement the response creation helper method
    // This method should:
    // 1. Create a new ObjectNode for the response
    // 2. Set "src" to this node's ID (nodeId)
    // 3. Set "dest" to the destination node (the original sender)
    // 4. Set "body" to the provided body ObjectNode
    // 5. Convert the entire response to a JSON string
    // Expected format:
    // {
    //   "src": "n1",        // Our node ID
    //   "dest": "c1",       // The destination (original sender)
    //   "body": {           // The body object passed to this method
    //     ...response body fields...
    //   }
    // }
    private String createResponse(String dest, ObjectNode body) throws Exception {
        // Create a response with the proper src, dest, and body
        return null;
    }
}
