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

public class Broadcast {
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
    private Set<Integer> messages = new HashSet<>();
    private int nextMsgId = 0;
    
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
        
        // Add to our set of received messages
        messages.add(message);
        
        // Forward this message to all other nodes (naive flooding approach)
        broadcastToAll(message);
        
        // Send back acknowledgment
        ObjectNode responseBody = mapper.createObjectNode();
        responseBody.put("type", "broadcast_ok");
        responseBody.put("in_reply_to", body.get("msg_id").asInt());
        
        return createResponse(src, responseBody);
    }
    
    private void broadcastToAll(int message) throws Exception {
        // TODO: This is a naive approach that causes message amplification
        // Send to all other nodes
        for (String node : nodeIds) {
            if (!node.equals(nodeId)) {
                sendBroadcast(node, message);
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
        // For now, we just acknowledge the topology but don't use it
        // We'll use this in more advanced implementations
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
