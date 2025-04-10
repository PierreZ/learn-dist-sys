# Step 2: Implementing a Unique ID Generator

In this step, we'll build a unique ID generator node using Maelstrom. This service responds to requests for globally unique IDs, which is a common requirement in distributed systems for things like transaction IDs, document IDs, and more.

## The Unique ID Generation Challenge

Generating unique IDs in a distributed system can be challenging because:
1. Multiple nodes need to generate IDs independently without coordination
2. IDs must never collide, even when nodes don't communicate
3. The system must remain available even during network partitions

## The Unique ID Workload

For the unique ID workload, our nodes will handle the following message types:

1. **Init message**: Same as in Step 1, to initialize the node
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

2. **Generate message**: Sent by clients to request a new unique ID
   ```json
   {
     "src": "c1",
     "dest": "n1",
     "body": {
       "type": "generate",
       "msg_id": 1
     }
   }
   ```

3. **Generate response**: Sent back to the client with a unique ID
   ```json
   {
     "src": "n1",
     "dest": "c1",
     "body": {
       "type": "generate_ok",
       "msg_id": 1,
       "in_reply_to": 1,
       "id": "n1-1"  // A globally unique ID
     }
   }
   ```

## Getting Started

We've provided a skeleton implementation in the `UniqueId.java` file based on our Echo server from Step 1. You can open and modify this file directly instead of copying code from the README.

The provided code includes:
- The basic setup for reading from stdin and writing to stdout
- A `UniqueIdServer` class with placeholders for message handling
- Methods for handling init messages
- A placeholder for the generate message handler that needs implementation

Your task is to implement the unique ID generation logic in the `handleGenerate` method to ensure IDs are globally unique across all nodes.

## Implementation Steps

### Step 1: Implement a Naive Approach (That Will Fail)

Let's start with what seems like a natural approach - using a simple counter:

```java
class UniqueIdServer {
    private final ObjectMapper mapper = new ObjectMapper();
    private String nodeId;
    private int counter = 0;  // Simple counter for IDs
    
    // ...other methods...
    
    private String handleGenerate(String src, String dest, JsonNode body) throws Exception {
        // Increment the counter for each request
        counter++;
        
        // Use the counter as the ID
        String id = String.valueOf(counter);
        
        ObjectNode responseBody = mapper.createObjectNode();
        responseBody.put("type", "generate_ok");
        responseBody.put("in_reply_to", body.get("msg_id").asInt());
        responseBody.put("id", id);
        
        return createResponse(src, responseBody);
    }
}
```

This approach seems reasonable at first glance. Each time a node receives a generate request, it increments its counter and returns the new value.

### Step 2: Understand Why This Fails in a Distributed System

Let's run this implementation with Maelstrom:

```bash
../bin/maelstrom test -w unique-ids --bin ./NaiveUniqueId.java --time-limit 5 --node-count 3 --availability total --nemesis partition
```

This test will likely fail with an error like:

```
:workload {:valid? false,
           :attempted-count 26,
           :acknowledged-count 26,
           :duplicated-count 7,
           :duplicated {"1" 3,
                        "2" 3,
                        "3" 3,
                        "4" 3,
                        "5" 3,
                        "6" 3,
                        "7" 2},
           :range ["1" "9"]},
:valid? false}

Analysis invalid! (ﾉಥ益ಥ）ﾉ ┻━┻
```

**Understanding the error output:**

This error output is incredibly revealing! Let's analyze what Maelstrom found:

1. `:duplicated-count 7` - Seven IDs were duplicated across the system
2. `:duplicated {"1" 3, ...}` - Each number was seen multiple times:
   - The ID "1" was used by all 3 nodes
   - The ID "2" was used by all 3 nodes
   - And so on...
   - The ID "7" was used by 2 nodes

This proves exactly what we predicted would happen. Each node started its own counter at 1 and incremented it independently. So all three nodes generated IDs 1, 2, 3, etc., causing duplicates.

**Viewing the complete operation history:**

Before diving into the details, let's see how to view the complete test results:

```bash
../bin/maelstrom serve
```

This will start a web server, typically on port 8080. Open a browser and go to:

```
http://localhost:8080
```

From here, you can:
1. Click on the latest test run
2. View "history.txt" to see the complete operation log
3. Examine "messages.svg" to visualize message flow between nodes
4. Check other visualizations like "latency-raw.png" and "rate.png"

The messages visualization is particularly useful for seeing how nodes communicate during partitions. We strongly encourage you to explore these visualizations to develop a better intuition for distributed systems.

**Looking at the operation history:**

Here's a part of the history from a test run that demonstrates the issue:

```
0	:invoke	:generate	nil
:nemesis	:info	:start-partition	:one
:nemesis	:info	:start-partition	[:isolated {"n0" #{"n2" "n1"}, "n2" #{"n0"}, "n1" #{"n0"}}]
0	:ok	:generate	"1"
2	:invoke	:generate	nil
2	:ok	:generate	"1"   <- Duplicate ID "1"
0	:invoke	:generate	nil
0	:ok	:generate	"2"
0	:invoke	:generate	nil
0	:ok	:generate	"3"
1	:invoke	:generate	nil
1	:ok	:generate	"1"   <- Duplicate ID "1" again
2	:invoke	:generate	nil
2	:ok	:generate	"2"   <- Duplicate ID "2"
0	:invoke	:generate	nil
0	:ok	:generate	"4"
0	:invoke	:generate	nil
0	:ok	:generate	"5"
1	:invoke	:generate	nil
1	:ok	:generate	"2"   <- Duplicate ID "2" again
2	:invoke	:generate	nil
2	:ok	:generate	"3"   <- Duplicate ID "3"
```

Let's understand what's happening here:

The first column (0, 1, or 2) refers to the client ID in Jepsen that's making the request. Each client is talking to a different node in our cluster. We can see that:

1. Client 0 is talking to a node that generates IDs: 1, 2, 3, 4, 5...
2. Client 1 is talking to a node that generates IDs: 1, 2, 3...
3. Client 2 is talking to a node that generates IDs: 1, 2, 3...

The nemesis entry shows that Maelstrom created a network partition, isolating the nodes from each other. Despite this partition, all nodes are still able to generate IDs (which is good for availability), but they're generating duplicate IDs (which is bad for correctness).

When client 0's node's counter reaches 5, it returns "5" as an ID.
When client 1's node's counter reaches 5, it also returns "5" as an ID.
When client 2's node's counter reaches 5, it also returns "5" as an ID.

We now have three different requests all receiving the same ID "5", which violates our uniqueness requirement.

**Why does this fail?** 

In a distributed system with multiple nodes (in this case, 3), each node runs its own instance of the UniqueIdServer class. This means each node has its own independent counter starting at 0.

When a client asks for a unique ID and gets "3" from node A, and then another client asks for a unique ID and gets "3" from node B, we have a conflict - both clients believe they have a unique ID, but they actually have the same value!

This history demonstrates the fundamental distributed systems challenge: the nodes can't coordinate with each other, especially during a partition, but we still need them to generate globally unique IDs.

### Step 3: Design a Better Strategy for Unique ID Generation

To solve this problem, we need a way to ensure IDs are unique across all nodes. Several approaches exist:

1. **Prefix with node ID**: Use the node's ID as a prefix for locally generated IDs
2. **UUID**: Use Java's built-in UUID generator
3. **Timestamp + node ID**: Combine timestamps with node ID
4. **Snowflake-like IDs**: Structured IDs with timestamp, node ID, and sequence number

For this exercise, we'll implement the simplest approach: using the node ID as a prefix for a local counter.

### Step 4: Implement the Node ID Prefixing Strategy

Update the server class to use the node ID as a prefix:

```java
class UniqueIdServer {
    private final ObjectMapper mapper = new ObjectMapper();
    private String nodeId;
    private int lastId = 0;  // Counter for IDs
    
    // ...other methods...
    
    private String handleGenerate(String src, String dest, JsonNode body) throws Exception {
        // Increment the counter for each request
        lastId++;
        
        // Create a unique ID using node ID as prefix
        String uniqueId = nodeId + "-" + lastId;
        
        ObjectNode responseBody = mapper.createObjectNode();
        responseBody.put("type", "generate_ok");
        responseBody.put("in_reply_to", body.get("msg_id").asInt());
        responseBody.put("id", uniqueId);
        
        return createResponse(src, responseBody);
    }
}
```

Now, when client 0's node's counter reaches 5, it returns "n0-5" as an ID.
When client 1's node's counter reaches 5, it returns "n1-5" as an ID.
When client 2's node's counter reaches 5, it returns "n2-5" as an ID.

These IDs are all different, ensuring uniqueness across the entire system.

### Step 5: Test with Maelstrom

We've provided a run script that will test your implementation with Maelstrom:

```bash
./run.sh
```

This will execute the test with multiple nodes and network partitions. The test should pass, confirming that our IDs are truly unique, even when the network experiences partitions.

## Complete Implementation

Here's the complete implementation with the unique ID generation logic:

```java
///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS com.fasterxml.jackson.core:jackson-databind:2.15.2

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Scanner;

public class UniqueId {
    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);
        UniqueIdServer server = new UniqueIdServer();
        
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

class UniqueIdServer {
    private final ObjectMapper mapper = new ObjectMapper();
    private String nodeId;
    private int lastId = 0;  // Counter for IDs
    
    public String handleMessage(String messageJson) throws Exception {
        JsonNode message = mapper.readTree(messageJson);
        String src = message.get("src").asText();
        String dest = message.get("dest").asText();
        JsonNode body = message.get("body");
        String type = body.get("type").asText();
        
        if (type.equals("init")) {
            return handleInit(src, dest, body);
        } else if (type.equals("generate")) {
            return handleGenerate(src, dest, body);
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
    
    private String handleGenerate(String src, String dest, JsonNode body) throws Exception {
        // Increment the counter for each request
        lastId++;
        
        // Create a unique ID using node ID as prefix
        String uniqueId = nodeId + "-" + lastId;
        
        ObjectNode responseBody = mapper.createObjectNode();
        responseBody.put("type", "generate_ok");
        responseBody.put("in_reply_to", body.get("msg_id").asInt());
        responseBody.put("id", uniqueId);
        
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

## Understanding the Test Parameters

When we run the test with:

```bash
../bin/maelstrom test -w unique-ids --bin ./UniqueId.java --time-limit 5 --node-count 3 --availability total --nemesis partition
```

We are:
- Testing the `unique-ids` workload
- Running for 5 seconds
- Using 3 nodes for redundancy
- Requiring total availability (all requests must succeed)
- Introducing network partitions with the `partition` nemesis

This tests that our ID generator works even under challenging conditions like network partitions.

## Reflection on Unique ID Generation

The approach we've taken:
- Ensures uniqueness through node ID prefixing
- Maintains availability during network partitions
- Is simple to implement and understand

However, it has limitations:
- IDs are not sortable by time (unlike timestamps)
- The counter may overflow for very long-running services
- IDs include the node ID, which might leak implementation details

In real distributed systems, more sophisticated ID generation schemes are often used, such as:
- Twitter's Snowflake
- UUID v4 (random) or v1 (time-based)
- ULID (Universally Unique Lexicographically Sortable Identifier)

## Next Steps

In the next step, we'll tackle a more complex challenge: implementing a broadcast service where nodes need to communicate with each other to disseminate messages throughout the cluster.
