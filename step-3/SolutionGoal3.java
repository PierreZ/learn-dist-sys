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
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Collections;

/**
 * SolutionGoal3 - Partition-Tolerant Broadcast System
 * 
 * Goal 3 builds on previous implementations by adding partition tolerance.
 * This solution continues to work correctly even when the network is partitioned,
 * using a gossip protocol to eventually propagate messages to all nodes.
 * 
 * Key improvements:
 * 1. Implements a gossip protocol that periodically exchanges messages
 * 2. Maintains independent message tracking per node
 * 3. Ensures eventual consistency during network partitions
 */
public class SolutionGoal3 {
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
 * BroadcastServer implementation with partition tolerance using a gossip protocol
 * 
 * This implementation solves the third goal of the broadcast exercise:
 * ensuring correct operation even during network partitions through 
 * a gossip protocol.
 * 
 * The gossip protocol works by:
 * 1. Each node periodically selects a random neighbor
 * 2. The node shares all messages it has seen with that neighbor
 * 3. This ensures eventual propagation of all messages to all nodes
 * 4. Even when the network is temporarily partitioned
 */
class BroadcastServer {
    private final ObjectMapper mapper = new ObjectMapper();
    private String nodeId;
    
    // Set of neighbors from topology information
    private List<String> neighbors = Collections.synchronizedList(new ArrayList<>());
    
    // Storage for messages that have been seen by this node
    private Set<Integer> messages = ConcurrentHashMap.newKeySet();
    
    // Track which messages have been sent to each neighbor
    private Map<String, Set<Integer>> messagesSentToNeighbor = new ConcurrentHashMap<>();
    
    // For generating message IDs
    private int nextMsgId = 0;
    
    // Random number generator for selecting gossip targets
    private final Random random = new Random();
    
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
        } else if (type.equals("gossip")) {
            return handleGossip(src, dest, body);
        } else if (type.equals("broadcast_ok") || type.equals("topology_ok") || type.equals("read_ok") || type.equals("gossip_ok")) {
            // Ignore acknowledgment messages
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
        
        // Start gossip protocol (periodically send messages to random neighbors)
        startGossipThread();
        
        ObjectNode responseBody = mapper.createObjectNode();
        responseBody.put("type", "init_ok");
        responseBody.put("in_reply_to", body.get("msg_id").asInt());
        
        return createResponse(src, responseBody);
    }
    
    private String handleTopology(String src, String dest, JsonNode body) throws Exception {
        JsonNode topologyNode = body.get("topology");
        JsonNode nodeNeighbors = topologyNode.get(nodeId);
        
        neighbors.clear();
        for (JsonNode neighborNode : nodeNeighbors) {
            String neighbor = neighborNode.asText();
            neighbors.add(neighbor);
            messagesSentToNeighbor.put(neighbor, ConcurrentHashMap.newKeySet());
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
                    // Mark this message as to be sent during gossip
                    Set<Integer> sentMessages = messagesSentToNeighbor.get(neighbor);
                    if (sentMessages != null && !sentMessages.contains(message)) {
                        // Add to tracking set to avoid resending
                        sentMessages.add(message);
                        
                        // Send immediately to this neighbor
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
    
    /**
     * Handle gossip messages from other nodes
     * 
     * This is the key to partition tolerance - nodes exchange their 
     * full set of messages during gossip to ensure eventual consistency.
     */
    private String handleGossip(String src, String dest, JsonNode body) throws Exception {
        // Process and merge incoming gossip messages
        JsonNode gossipMessages = body.get("messages");
        boolean addedAny = false;
        
        for (JsonNode messageNode : gossipMessages) {
            int message = messageNode.asInt();
            boolean isNew = messages.add(message);
            
            if (isNew) {
                addedAny = true;
                debug("Learned new message " + message + " from gossip");
                
                // Propagate to other neighbors in future gossip rounds
                for (String neighbor : neighbors) {
                    if (!neighbor.equals(src)) {
                        Set<Integer> sentMessages = messagesSentToNeighbor.get(neighbor);
                        if (sentMessages != null) {
                            sentMessages.remove(message); // Ensure it will be sent
                        }
                    }
                }
            }
        }
        
        if (addedAny) {
            debug("Added new messages from gossip from " + src);
        }
        
        // Send acknowledgment back to the gossiping node
        ObjectNode responseBody = mapper.createObjectNode();
        responseBody.put("type", "gossip_ok");
        responseBody.put("in_reply_to", body.get("msg_id").asInt());
        
        return createResponse(src, responseBody);
    }
    
    /**
     * Start a background thread that periodically sends gossip messages to random neighbors
     * 
     * This is the core of the gossip protocol - even if direct message propagation
     * fails due to network partitions, this background process will eventually
     * ensure all messages propagate to all nodes when connectivity is restored.
     */
    private void startGossipThread() {
        Thread gossipThread = new Thread(() -> {
            try {
                // Sleep before starting gossip to ensure full initialization
                Thread.sleep(1000);
                
                while (true) {
                    try {
                        // Only gossip if we have neighbors and messages
                        if (!neighbors.isEmpty() && !messages.isEmpty()) {
                            // Select a random neighbor to gossip with
                            String neighbor = neighbors.get(random.nextInt(neighbors.size()));
                            sendGossip(neighbor);
                        }
                        
                        // Sleep between gossip rounds
                        Thread.sleep(200);
                    } catch (Exception e) {
                        debug("Error in gossip thread: " + e.getMessage());
                    }
                }
            } catch (Exception e) {
                debug("Gossip thread terminated: " + e.getMessage());
            }
        });
        gossipThread.setDaemon(true);
        gossipThread.start();
        debug("Started gossip thread");
    }
    
    private void sendGossip(String dest) throws Exception {
        ObjectNode body = mapper.createObjectNode();
        body.put("type", "gossip");
        body.put("msg_id", nextMsgId++);
        
        ArrayNode messagesArray = mapper.createArrayNode();
        for (int message : messages) {
            messagesArray.add(message);
        }
        body.set("messages", messagesArray);
        
        // Track that we've sent all these messages to this neighbor
        Set<Integer> sentMessages = messagesSentToNeighbor.get(dest);
        if (sentMessages != null) {
            for (int message : messages) {
                sentMessages.add(message);
            }
        }
        
        ObjectNode requestMessage = mapper.createObjectNode();
        requestMessage.put("src", nodeId);
        requestMessage.put("dest", dest);
        requestMessage.set("body", body);
        
        System.out.println(mapper.writeValueAsString(requestMessage));
        debug("Sent gossip to " + dest + " with " + messages.size() + " messages");
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
