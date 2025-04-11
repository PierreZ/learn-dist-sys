///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS com.fasterxml.jackson.core:jackson-databind:2.15.2

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.util.Scanner;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;

/**
 * SolutionGoal1 - Broadcast System with Message Deduplication
 * 
 * This solution addresses the first goal of the broadcast challenge:
 * avoiding message amplification by implementing message deduplication.
 * 
 * Key Concept: Each node maintains a set of messages it has already seen.
 * When a node receives a message, it only processes and forwards it if 
 * the message hasn't been seen before.
 */
public class SolutionGoal1 {
    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);
        BroadcastServer server = new BroadcastServer();
        
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
 * Implementation of a broadcast server that prevents message amplification.
 * 
 * This class handles four types of messages:
 * 1. init - Initialize the node with its ID and the IDs of all nodes
 * 2. broadcast - Process a broadcast message and forward to other nodes
 * 3. read - Return all messages this node has seen
 * 4. topology - Process network topology information (unused in Goal 1)
 */
class BroadcastServer {
    private final ObjectMapper mapper = new ObjectMapper();
    private String nodeId;
    private List<String> nodeIds;
    
    // Using a HashSet for efficient O(1) lookups when checking for duplicates
    private Set<Integer> messages = new HashSet<>();
    private int nextMsgId = 0;
    
    /**
     * Handle incoming messages from the network.
     * 
     * This method processes the message based on its type and returns a response.
     * 
     * @param messageJson JSON representation of the incoming message
     * @return Response to the incoming message, or null if no response is needed
     * @throws Exception If there's an error processing the message
     */
    public String handleMessage(String messageJson) throws Exception {
        JsonNode message = mapper.readTree(messageJson);
        String src = message.get("src").asText();
        String dest = message.get("dest").asText();
        JsonNode body = message.get("body");
        String type = body.get("type").asText();
        
        if (type.equals("init")) {
            return handleInit(src, dest, body);
        } else if (type.equals("broadcast")) {
            return handleBroadcast(src, dest, body);
        } else if (type.equals("read")) {
            return handleRead(src, dest, body);
        } else if (type.equals("topology")) {
            return handleTopology(src, dest, body);
        } else {
            System.err.println("Unknown message type: " + type);
            return null;
        }
    }
    
    /**
     * Handle initialization messages.
     * 
     * This method sets up the node with its ID and the IDs of all other nodes.
     * 
     * @param src Source node ID
     * @param dest Destination node ID
     * @param body JSON body of the message
     * @return Response to the initialization message
     * @throws Exception If there's an error processing the message
     */
    private String handleInit(String src, String dest, JsonNode body) throws Exception {
        nodeId = body.get("node_id").asText();
        JsonNode nodeIdsNode = body.get("node_ids");
        
        nodeIds = new ArrayList<>();
        for (JsonNode n : nodeIdsNode) {
            nodeIds.add(n.asText());
        }
        
        System.err.println("Node " + nodeId + " initialized with nodes: " + nodeIds);
        
        ObjectNode responseBody = mapper.createObjectNode();
        responseBody.put("type", "init_ok");
        responseBody.put("in_reply_to", body.get("msg_id").asInt());
        
        return createResponse(src, responseBody);
    }
    
    /**
     * Handle broadcast messages.
     * 
     * This method checks if the message has been seen before, and if not,
     * forwards it to all other nodes.
     * 
     * @param src Source node ID
     * @param dest Destination node ID
     * @param body JSON body of the message
     * @return Response to the broadcast message, or null if no response is needed
     * @throws Exception If there's an error processing the message
     */
    private String handleBroadcast(String src, String dest, JsonNode body) throws Exception {
        // Get the message value
        int message = body.get("message").asInt();
        
        // Check if we've seen this message before
        if (!messages.contains(message)) {
            // This is a new message, add it to our set of received messages
            messages.add(message);
            
            // Forward this message to all other nodes (with deduplication)
            broadcastToAll(message);
        } else {
            // We've already seen this message, no need to forward it
            System.err.println("Node " + nodeId + " already seen message: " + message);
        }
        
        // Send back acknowledgment only if the source is a client
        if (src.startsWith("c")) {
            ObjectNode responseBody = mapper.createObjectNode();
            responseBody.put("type", "broadcast_ok");
            responseBody.put("in_reply_to", body.get("msg_id").asInt());
            
            return createResponse(src, responseBody);
        }
        
        return null;
    }
    
    /**
     * Forward a message to all other nodes.
     * 
     * This method sends the message to all nodes in the network, excluding the current node.
     * 
     * @param message Message to forward
     * @throws Exception If there's an error forwarding the message
     */
    private void broadcastToAll(int message) throws Exception {
        // Send to all other nodes
        for (String node : nodeIds) {
            if (!node.equals(nodeId)) {
                sendBroadcast(node, message);
            }
        }
    }
    
    /**
     * Send a broadcast message to a specific node.
     * 
     * This method creates a new broadcast message with the given message value and sends it to the specified node.
     * 
     * @param dest Destination node ID
     * @param message Message to send
     * @throws Exception If there's an error sending the message
     */
    private void sendBroadcast(String dest, int message) throws Exception {
        ObjectNode body = mapper.createObjectNode();
        body.put("type", "broadcast");
        body.put("message", message);
        body.put("msg_id", nextMsgId++);
        
        String payload = createResponse(dest, body);
        System.out.println(payload);
    }
    
    /**
     * Handle read messages.
     * 
     * This method returns all messages this node has seen.
     * 
     * @param src Source node ID
     * @param dest Destination node ID
     * @param body JSON body of the message
     * @return Response to the read message
     * @throws Exception If there's an error processing the message
     */
    private String handleRead(String src, String dest, JsonNode body) throws Exception {
        ObjectNode responseBody = mapper.createObjectNode();
        responseBody.put("type", "read_ok");
        responseBody.put("in_reply_to", body.get("msg_id").asInt());
        
        ArrayNode messagesArray = responseBody.putArray("messages");
        for (Integer message : messages) {
            messagesArray.add(message);
        }
        
        return createResponse(src, responseBody);
    }
    
    /**
     * Handle topology messages.
     * 
     * This method is currently unused in Goal 1, but can be used in future goals to process network topology information.
     * 
     * @param src Source node ID
     * @param dest Destination node ID
     * @param body JSON body of the message
     * @return Response to the topology message
     * @throws Exception If there's an error processing the message
     */
    private String handleTopology(String src, String dest, JsonNode body) throws Exception {
        // For now, we just acknowledge the topology but don't use it
        // We'll use this in more advanced implementations
        ObjectNode responseBody = mapper.createObjectNode();
        responseBody.put("type", "topology_ok");
        responseBody.put("in_reply_to", body.get("msg_id").asInt());
        
        return createResponse(src, responseBody);
    }
    
    /**
     * Create a response message.
     * 
     * This method creates a new message with the given source and destination node IDs, and the given response body.
     * 
     * @param dest Destination node ID
     * @param responseBody JSON body of the response message
     * @return JSON representation of the response message
     * @throws Exception If there's an error creating the response message
     */
    private String createResponse(String dest, ObjectNode responseBody) throws Exception {
        ObjectNode response = mapper.createObjectNode();
        response.put("src", nodeId);
        response.put("dest", dest);
        response.set("body", responseBody);
        
        return mapper.writeValueAsString(response);
    }
}
