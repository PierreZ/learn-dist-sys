# Step 1: Building Your First Maelstrom Node - Echo Server

In this step, we'll implement a simple echo server that communicates with Maelstrom. This will introduce you to the Maelstrom protocol and how nodes interact with the system.

## The Maelstrom Protocol

Maelstrom communicates with nodes through a simple JSON-based protocol:

- Nodes receive messages on STDIN
- Nodes send messages on STDOUT
- Nodes log debug output on STDERR

Each message has the following format:

```json
{
  "src": "source-id",
  "dest": "destination-id",
  "body": {
    "type": "message-type",
    "msg_id": 123,
    ...other fields...
  }
}
```

For request-response patterns, responses include an `in_reply_to` field in the body:

```json
{
  "src": "n1",
  "dest": "c1",
  "body": {
    "type": "echo_ok",
    "in_reply_to": 123,
    "echo": "hello"
  }
}
```

## Messages in the Echo Workload

For the echo workload, our node needs to handle two types of messages:

1. **Init message**: Sent by Maelstrom at the start of the test to provide node ID and topology
   ```json
   {
     "src": "c1",
     "dest": "n1",
     "body": {
       "type": "init",
       "msg_id": 1,
       "node_id": "n1",
       "node_ids": ["n1", "n2", "n3"]
     }
   }
   ```

2. **Echo message**: Sent by clients to request an echo response
   ```json
   {
     "src": "c1",
     "dest": "n1",
     "body": {
       "type": "echo",
       "msg_id": 2,
       "echo": "hello world"
     }
   }
   ```

## Understanding Jackson for JSON Processing

Since Maelstrom uses JSON for communication, we need a way to parse and generate JSON in our Java code. We'll use the [Jackson](https://github.com/FasterXML/jackson) library, which is a popular choice for JSON processing in Java.

### Key Jackson Components

1. **ObjectMapper**: The main entry point for JSON processing. We use it to:
   - Parse JSON strings into Java objects (`readTree`)
   - Convert Java objects to JSON strings (`writeValueAsString`)

2. **JsonNode**: Represents a JSON value in Jackson. We can:
   - Navigate through the JSON structure (`get("fieldName")`)
   - Convert values to Java types (`asText()`, `asInt()`)
   - Check for field existence (`has("fieldName")`)

3. **ObjectNode**: A mutable JsonNode that represents a JSON object. We can:
   - Add key-value pairs (`put("key", value)`)
   - Add nested objects (`set("key", otherNode)`)
   - Create arrays (`putArray("arrayField")`)

### Jackson Usage in Our Code

In our echo server implementation, we'll use Jackson to:

```java
// 1. Create the ObjectMapper (only once per application)
private static final ObjectMapper mapper = new ObjectMapper();

// 2. Parse incoming JSON messages
JsonNode message = mapper.readTree(jsonString);

// 3. Extract fields from the message
String src = message.get("src").asText();
int msgId = message.get("body").get("msg_id").asInt();

// 4. Create JSON responses
ObjectNode responseBody = mapper.createObjectNode();
responseBody.put("type", "echo_ok");
responseBody.put("in_reply_to", 123);

// 5. Convert the response to a JSON string
String jsonResponse = mapper.writeValueAsString(responseBody);
```

This pattern of parsing, processing, and generating JSON will be common across all the examples in this lab.

## Implementation Steps

> **Note**: For maximum learning benefit, we strongly encourage you to type out the code yourself rather than copy-pasting the examples. The process of writing the code line by line will help you better understand the concepts and internalize the patterns.

Let's build our echo server step by step:

### Step 1: Create the basic file structure

Create a file named `Echo.java` in the `step-1` directory:

```java
///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS com.fasterxml.jackson.core:jackson-databind:2.15.2

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Scanner;

public class Echo {
    private static final ObjectMapper mapper = new ObjectMapper();
    private static String nodeId;
    
    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);
        
        // Your code will go here
    }
}
```

### Step 2: Set up the main message processing loop

Add a loop to read and parse incoming messages:

```java
while (scanner.hasNextLine()) {
    String line = scanner.nextLine();
    try {
        JsonNode message = mapper.readTree(line);
        String src = message.get("src").asText();
        String dest = message.get("dest").asText();
        JsonNode body = message.get("body");
        String type = body.get("type").asText();
        
        // We'll handle message types here
    } catch (Exception e) {
        System.err.println("Error processing message: " + e.getMessage());
    }
}
```

### Step 3: Implement the init handler

Add code to handle the init message:

```java
if (type.equals("init")) {
    nodeId = body.get("node_id").asText();
    System.err.println("Node " + nodeId + " initialized");
    
    // Create response
    ObjectNode responseBody = mapper.createObjectNode();
    responseBody.put("type", "init_ok");
    responseBody.put("in_reply_to", body.get("msg_id").asInt());
    
    // Send response
    sendResponse(src, responseBody);
}
```

### Step 4: Implement the echo handler

Add code to handle echo requests:

```java
else if (type.equals("echo")) {
    ObjectNode responseBody = mapper.createObjectNode();
    responseBody.put("type", "echo_ok");
    responseBody.put("in_reply_to", body.get("msg_id").asInt());
    responseBody.set("echo", body.get("echo"));
    
    sendResponse(src, responseBody);
}
```

### Step 5: Add helper method for sending responses

Add a helper method to format and send responses:

```java
private static void sendResponse(String dest, ObjectNode body) {
    try {
        ObjectNode response = mapper.createObjectNode();
        response.put("src", nodeId);
        response.put("dest", dest);
        response.set("body", body);
        
        System.out.println(mapper.writeValueAsString(response));
    } catch (Exception e) {
        System.err.println("Error sending response: " + e.getMessage());
    }
}
```

### Step 6: Test with Maelstrom

Once you've completed the implementation, you can test your echo server using the provided run.sh script:

```bash
chmod +x Echo.java  # Make sure the file is executable
./run.sh
```

This will run the Maelstrom test with the echo workload. If everything is working correctly, you should see this success message:

```
Everything looks good! ヽ('ー`)ノ
```

The run.sh script calls Maelstrom with the appropriate parameters for testing the echo workload:

```bash
../bin/maelstrom test -w echo --bin ./Echo.java --node-count 1 --time-limit 10
```

## Final Solution

If you've followed all the steps, your complete Echo.java file should look something like this:

```java
///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS com.fasterxml.jackson.core:jackson-databind:2.15.2

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Scanner;

public class Echo {
    private static final ObjectMapper mapper = new ObjectMapper();
    private static String nodeId;
    private static int msgId = 0;
    
    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);
        
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            try {
                JsonNode message = mapper.readTree(line);
                String src = message.get("src").asText();
                String dest = message.get("dest").asText();
                JsonNode body = message.get("body");
                String type = body.get("type").asText();
                
                if (type.equals("init")) {
                    nodeId = body.get("node_id").asText();
                    System.err.println("Node " + nodeId + " initialized");
                    
                    ObjectNode responseBody = mapper.createObjectNode();
                    responseBody.put("type", "init_ok");
                    responseBody.put("in_reply_to", body.get("msg_id").asInt());
                    
                    sendResponse(src, responseBody);
                }
                else if (type.equals("echo")) {
                    ObjectNode responseBody = mapper.createObjectNode();
                    responseBody.put("type", "echo_ok");
                    responseBody.put("in_reply_to", body.get("msg_id").asInt());
                    responseBody.set("echo", body.get("echo"));
                    
                    sendResponse(src, responseBody);
                }
                else {
                    System.err.println("Unknown message type: " + type);
                }
                
            } catch (Exception e) {
                System.err.println("Error processing message: " + e.getMessage() + "\nInput was: " + line);
            }
        }
    }
    
    private static void sendResponse(String dest, ObjectNode body) {
        try {
            ObjectNode response = mapper.createObjectNode();
            response.put("src", nodeId);
            response.put("dest", dest);
            response.set("body", body);
            
            System.out.println(mapper.writeValueAsString(response));
        } catch (Exception e) {
            System.err.println("Error sending response: " + e.getMessage());
        }
    }
}
```

### Step 7: Refactor to Use an EchoServer Class

Let's improve our solution by refactoring it to use an object-oriented approach with an `EchoServer` class that handles the node functionality:

```java
///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS com.fasterxml.jackson.core:jackson-databind:2.15.2

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Scanner;

public class Echo {
    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);
        EchoServer server = new EchoServer();
        
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            try {
                JsonNode message = server.handleMessage(line);
                if (message != null) {
                    System.out.println(message);
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
    
    private String handleInit(String src, String dest, JsonNode body) throws Exception {
        nodeId = body.get("node_id").asText();
        System.err.println("Node " + nodeId + " initialized");
        
        ObjectNode responseBody = mapper.createObjectNode();
        responseBody.put("type", "init_ok");
        responseBody.put("in_reply_to", body.get("msg_id").asInt());
        
        return createResponse(src, responseBody);
    }
    
    private String handleEcho(String src, String dest, JsonNode body) throws Exception {
        ObjectNode responseBody = mapper.createObjectNode();
        responseBody.put("type", "echo_ok");
        responseBody.put("in_reply_to", body.get("msg_id").asInt());
        responseBody.set("echo", body.get("echo"));
        
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
```

## Understanding How Maelstrom Tests Work

When you run a Maelstrom test:

1. Maelstrom starts multiple copies of your node
2. It sends an `init` message to each node
3. It simulates clients that send `echo` requests 
4. It verifies that each node responds correctly to the requests
5. It generates reports on the behavior of your system

This is a simple example, but the principles apply to more complex distributed systems we'll build in later steps.

## Visualizing Results with Maelstrom Serve

After running tests, Maelstrom generates detailed logs and visualizations in the `store/` directory. To easily view these results in a web browser, you can use the `serve` command:

```bash
../bin/maelstrom serve
```

This will start a local web server (typically on port 8080) that allows you to:

1. Browse all test runs
2. View node logs
3. Examine message flow visualizations
4. Analyze performance graphs
5. Inspect error cases

The web interface makes it much easier to understand what's happening in your system, especially for more complex workloads. 

To access the interface, open [http://localhost:8080](http://localhost:8080) in your web browser after running the command.

## Key Takeaways

- Maelstrom communicates with nodes via standard input/output using JSON messages
- Each node has a unique ID provided during initialization
- Nodes must respond to client requests with the correct message format
- Debugging information can be sent to standard error

## Next Steps

In the next step, we'll build a more complex system that involves communication between nodes and state management.