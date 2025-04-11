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
import java.util.Map;
import java.util.HashMap;

/**
 * SolutionGoal2 - Broadcast System with Latency Optimization
 * 
 * This solution addresses the second goal of the broadcast challenge:
 * optimizing latency by using network topology information to create a more
 * efficient message propagation pattern.
 * 
 * Key Concept: Rather than naively broadcasting to all nodes,
 * this implementation only forwards messages to direct neighbors
 * based on the topology provided. This reduces the number of hops
 * a message needs to take to reach all nodes in the network.
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
 * Implementation of a broadcast server that optimizes message propagation using topology information.
 * 
 * This class extends the basic functionality from Goal 1 (message deduplication)
 * by also tracking the network topology and only forwarding messages to direct neighbors.
 */
class BroadcastServer {
    private final ObjectMapper mapper = new ObjectMapper();
    private String nodeId;
    private List<String> nodeIds;
    private Set<Integer> messages = new HashSet<>();
    private int nextMsgId = 0;
    
    // Topology information - each node ID maps to list of neighbors
    private Map<String, List<String>> topology = new HashMap<>();
    
    // Direct neighbors of this node in the network topology
    private List<String> neighbors = new ArrayList<>();
    
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
    
    private String handleBroadcast(String src, String dest, JsonNode body) throws Exception {
        // Get the message value
        int message = body.get("message").asInt();
        
        // Check if we've already seen this message
        if (!messages.contains(message)) {
            // Add to our set of received messages
            messages.add(message);
            
            // Forward this message to neighbors according to topology
            broadcastToNeighbors(message);
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
    
    private void broadcastToNeighbors(int message) throws Exception {
        // If we have topology information, use it to broadcast to neighbors only
        if (!neighbors.isEmpty()) {
            for (String node : neighbors) {
                sendBroadcast(node, message);
            }
        } else {
            // Fallback to broadcasting to all nodes if no topology is defined
            for (String node : nodeIds) {
                if (!node.equals(nodeId)) {
                    sendBroadcast(node, message);
                }
            }
        }
    }
    
    private void sendBroadcast(String dest, int message) throws Exception {
        ObjectNode body = mapper.createObjectNode();
        body.put("type", "broadcast");
        body.put("message", message);
        body.put("msg_id", nextMsgId++);
        
        String payload = createResponse(dest, body);
        System.out.println(payload);
    }
    
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
    
    private String handleTopology(String src, String dest, JsonNode body) throws Exception {
        // Store the topology information
        JsonNode topologyNode = body.get("topology");
        
        topology.clear();
        neighbors.clear();
        
        // Parse the topology
        topologyNode.fields().forEachRemaining(entry -> {
            String node = entry.getKey();
            List<String> nodeNeighbors = new ArrayList<>();
            for (JsonNode n : entry.getValue()) {
                nodeNeighbors.add(n.asText());
            }
            topology.put(node, nodeNeighbors);
            
            // Store our own neighbors
            if (node.equals(nodeId)) {
                neighbors.addAll(nodeNeighbors);
            }
        });
        
        System.err.println("Node " + nodeId + " received topology: " + topology);
        System.err.println("Node " + nodeId + " neighbors: " + neighbors);
        
        ObjectNode responseBody = mapper.createObjectNode();
        responseBody.put("type", "topology_ok");
        responseBody.put("in_reply_to", body.get("msg_id").asInt());
        
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
