///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS com.fasterxml.jackson.core:jackson-databind:2.15.2

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class JsonExample {
    public static void main(String[] args) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        
        // Create a simple JSON object
        ObjectNode node = mapper.createObjectNode();
        node.put("message", "Hello from JBang with Jackson!");
        node.put("timestamp", System.currentTimeMillis());
        
        // Print the JSON
        System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(node));
    }
}
