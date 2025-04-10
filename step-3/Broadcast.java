///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS com.fasterxml.jackson.core:jackson-databind:2.15.2

import java.util.Scanner;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

public class Broadcast {
    private static final ObjectMapper mapper = new ObjectMapper();
    private static String nodeId;
    private static Set<Integer> messages = new HashSet<>();
    private static int msgId = 0;
    
    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);
        
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            JsonNode msg = mapper.readTree(line);
            
            String src = msg.get("src").asText();
            String dest = msg.get("dest").asText();
            JsonNode body = msg.get("body");
            String type = body.get("type").asText();
            
            // Store our node ID from the init message
            if (type.equals("init")) {
                nodeId = body.get("node_id").asText();
                System.err.println("Node " + nodeId + " initialized");
                
                ObjectNode responseBody = mapper.createObjectNode();
                responseBody.put("type", "init_ok");
                
                if (msg.has("id")) {
                    responseBody.put("in_reply_to", msg.get("id").asInt());
                }
                
                ObjectNode response = mapper.createObjectNode();
                response.put("src", dest);
                response.put("dest", src);
                response.put("id", ++msgId);
                response.set("body", responseBody);
                
                System.out.println(mapper.writeValueAsString(response));
                continue;
            }
            
            // Handle operations
            if (type.equals("broadcast")) {
                int message = body.get("message").asInt();
                messages.add(message);
            }
            
            // Create response
            ObjectNode responseBody = mapper.createObjectNode();
            responseBody.put("type", type + "_ok");
            
            // Add in_reply_to if present in the request
            if (msg.has("id")) {
                responseBody.put("in_reply_to", msg.get("id").asInt());
            }
            
            // Add messages array for read requests
            if (type.equals("read")) {
                ArrayNode messagesArray = responseBody.putArray("messages");
                for (Integer m : messages) {
                    messagesArray.add(m);
                }
            }
            
            // Send response
            ObjectNode response = mapper.createObjectNode();
            response.put("src", nodeId);
            response.put("dest", src);
            response.put("id", ++msgId);
            response.set("body", responseBody);
            
            System.out.println(mapper.writeValueAsString(response));
        }
    }
}
