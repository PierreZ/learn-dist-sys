///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS com.fasterxml.jackson.core:jackson-databind:2.15.2

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.util.Scanner;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

/**
 * SolutionGoal2 - Broadcast System with Latency Optimization
 * 
 * Goal 2 builds on Goal 1 by adding latency optimization using topology information.
 * Instead of broadcasting to all nodes, this solution only sends messages to direct
 * neighbors in the topology, reducing network traffic and improving message delivery latency.
 * 
 * Key improvements:
 * 1. Uses topology information to determine neighbors
 * 2. Only sends messages to direct neighbors rather than all nodes
 * 3. Maintains the message deduplication from Goal 1
 */
public class SolutionGoal2 {
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
 * BroadcastServer implementation with topology-aware message propagation
 * 
 * This implementation solves the second goal of the broadcast exercise:
 * optimizing message delivery by using topology information to reduce latency.
 * 
 * By only sending messages to direct neighbors instead of all nodes, this
 * approach reduces network traffic and the number of hops required for
 * message delivery, resulting in lower overall latency.
 */
class BroadcastServer {
    private final ObjectMapper mapper = new ObjectMapper();
    private String nodeId;
    
    // Set of neighbors from topology information
    private List<String> neighbors = new ArrayList<>();
    
    // Storage for messages that have been seen by this node
    private Set<Integer> messages = new HashSet<>();
    
    // Track messages sent to each neighbor to avoid resending
    private Map<String, Set<Integer>> messagesSentToNeighbor = new HashMap<>();
    
    // For generating local message IDs
    private int nextMsgId = 0;
    
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
        } else if (type.equals("topology")) {
            return handleTopology(src, dest, body);
        } else if (type.equals("broadcast")) {
            return handleBroadcast(src, dest, body);
        } else if (type.equals("read")) {
            return handleRead(src, dest, body);
        } else if (type.equals("broadcast_ok") || type.equals("topology_ok") || type.equals("read_ok")) {
            // Ignore these messages as they are responses to our own requests
            return null;
        } else {
            debug("Unknown message type: " + type);
            return null;
        }
    }
    
    private String handleInit(String src, String dest, JsonNode body) throws Exception {
        nodeId = body.get("node_id").asText();
        JsonNode nodeIdsNode = body.get("node_ids");
        List<String> nodeIds = new ArrayList<>();
        for (JsonNode nodeIdNode : nodeIdsNode) {
            nodeIds.add(nodeIdNode.asText());
        }
        debug("Node " + nodeId + " initialized with " + nodeIds.size() + " nodes in cluster");
        
        ObjectNode responseBody = mapper.createObjectNode();
        responseBody.put("type", "init_ok");
        responseBody.put("in_reply_to", body.get("msg_id").asInt());
        
        return createResponse(src, responseBody);
    }
    
    private String handleTopology(String src, String dest, JsonNode body) throws Exception {
        // Extract our neighbors from the topology
        JsonNode topologyNode = body.get("topology");
        JsonNode nodeNeighbors = topologyNode.get(nodeId);
        
        neighbors.clear();
        for (JsonNode neighborNode : nodeNeighbors) {
            String neighbor = neighborNode.asText();
            neighbors.add(neighbor);
            messagesSentToNeighbor.put(neighbor, new HashSet<>());
        }
        debug("Received topology: neighbors = " + neighbors);
        
        ObjectNode responseBody = mapper.createObjectNode();
        responseBody.put("type", "topology_ok");
        responseBody.put("in_reply_to", body.get("msg_id").asInt());
        
        return createResponse(src, responseBody);
    }
    
    private String handleBroadcast(String src, String dest, JsonNode body) throws Exception {
        // Add the message to our known messages
        int message = body.get("message").asInt();
        boolean isNew = messages.add(message);
        
        if (isNew) {
            debug("Received new message: " + message + " from " + src);
            
            // Propagate to neighbors (except the source)
            for (String neighbor : neighbors) {
                if (!neighbor.equals(src)) {
                    // Check if we've already sent this message to this neighbor
                    Set<Integer> sentMessages = messagesSentToNeighbor.get(neighbor);
                    if (sentMessages != null && !sentMessages.contains(message)) {
                        // Add to tracking set to avoid resending
                        sentMessages.add(message);
                        
                        // Propagate to this neighbor
                        sendBroadcast(neighbor, message);
                        debug("Propagated message " + message + " to neighbor " + neighbor);
                    }
                }
            }
        } else {
            debug("Ignoring duplicate message: " + message);
        }
        
        // Send acknowledgment back to the client/node that sent us this message
        ObjectNode responseBody = mapper.createObjectNode();
        responseBody.put("type", "broadcast_ok");
        responseBody.put("in_reply_to", body.get("msg_id").asInt());
        
        return createResponse(src, responseBody);
    }
    
    private String handleRead(String src, String dest, JsonNode body) throws Exception {
        debug("Received read request from " + src);
        
        ObjectNode responseBody = mapper.createObjectNode();
        responseBody.put("type", "read_ok");
        responseBody.put("in_reply_to", body.get("msg_id").asInt());
        
        ArrayNode messagesArray = mapper.createArrayNode();
        for (int message : messages) {
            messagesArray.add(message);
        }
        responseBody.set("messages", messagesArray);
        
        return createResponse(src, responseBody);
    }
    
    private void sendBroadcast(String dest, int message) throws Exception {
        ObjectNode body = mapper.createObjectNode();
        body.put("type", "broadcast");
        body.put("message", message);
        body.put("msg_id", nextMsgId++);
        
        ObjectNode requestMessage = mapper.createObjectNode();
        requestMessage.put("src", nodeId);
        requestMessage.put("dest", dest);
        requestMessage.set("body", body);
        
        System.out.println(mapper.writeValueAsString(requestMessage));
    }
    
    private String createResponse(String dest, ObjectNode body) throws Exception {
        ObjectNode response = mapper.createObjectNode();
        response.put("src", nodeId);
        response.put("dest", dest);
        response.set("body", body);
        
        return mapper.writeValueAsString(response);
    }
}
