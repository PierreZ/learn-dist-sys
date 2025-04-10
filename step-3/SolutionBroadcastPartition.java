///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS com.fasterxml.jackson.core:jackson-databind:2.15.2

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.util.*;
import java.util.concurrent.*;

/**
 * Solution implementation for the broadcast service that handles network partitions
 * by using reliable gossip protocol to ensure eventual consistency.
 */
public class SolutionBroadcastPartition {
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

class BroadcastServer {
    private final ObjectMapper mapper = new ObjectMapper();
    private String nodeId;
    private List<String> nodeIds;
    
    // Track messages we've seen with their source
    private Map<Integer, String> messages = new ConcurrentHashMap<>();
    
    // Track messages we need to propagate
    private Set<Integer> messagesToPropagate = ConcurrentHashMap.newKeySet();
    
    // Track failed message deliveries for retry
    private Map<String, Set<Integer>> pendingMessages = new ConcurrentHashMap<>();
    
    // Topology information
    private Map<String, List<String>> topology = new ConcurrentHashMap<>();
    private List<String> neighbors = new ArrayList<>();
    
    // Message counter for generating unique IDs
    private int nextMsgId = 0;
    
    // Gossip scheduler for periodic propagation
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    
    // Flag to keep track if we've started the gossip scheduler
    private boolean gossipStarted = false;
    
    public BroadcastServer() {
        // Start the gossip protocol when the server is created
        startGossipProtocol();
    }
    
    private void startGossipProtocol() {
        if (!gossipStarted) {
            scheduler.scheduleAtFixedRate(() -> {
                try {
                    propagateMessages();
                } catch (Exception e) {
                    System.err.println("Error in gossip: " + e.getMessage());
                }
            }, 1, 1, TimeUnit.SECONDS);
            gossipStarted = true;
        }
    }
    
    private void propagateMessages() throws Exception {
        if (nodeId == null || messagesToPropagate.isEmpty()) {
            return; // Not initialized or no messages to propagate
        }
        
        // Make a copy to avoid concurrent modification
        Set<Integer> currentMessages = new HashSet<>(messagesToPropagate);
        List<String> targets = neighbors.isEmpty() ? nodeIds : neighbors;
        
        for (String node : targets) {
            if (node.equals(nodeId)) {
                continue; // Skip self
            }
            
            // Get pending messages for this node or create a new set
            Set<Integer> nodePending = pendingMessages.computeIfAbsent(node, k -> ConcurrentHashMap.newKeySet());
            
            // Add all current messages to pending for this node
            nodePending.addAll(currentMessages);
            
            // Try to send all pending messages
            for (Integer message : new HashSet<>(nodePending)) {
                try {
                    sendBroadcast(node, message);
                    // If successful, remove from pending
                    nodePending.remove(message);
                } catch (Exception e) {
                    // Keep in pending if failed
                    System.err.println("Failed to send message " + message + " to " + node + ": " + e.getMessage());
                }
            }
        }
    }
    
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
        if (!messages.containsKey(message)) {
            // Add to our set of received messages and mark for propagation
            messages.put(message, src);
            messagesToPropagate.add(message);
        }
        
        // Send back acknowledgment
        ObjectNode responseBody = mapper.createObjectNode();
        responseBody.put("type", "broadcast_ok");
        responseBody.put("in_reply_to", body.get("msg_id").asInt());
        
        return createResponse(src, responseBody);
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
        for (Integer message : messages.keySet()) {
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
