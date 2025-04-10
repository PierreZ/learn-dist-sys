# Step 3: Building a Broadcast System

In this step, we'll implement a broadcast system where nodes can share messages with all other nodes in the network. This introduces several critical concepts in distributed systems, including:
- Message propagation
- Network efficiency
- Fault tolerance
- Eventual consistency

## The Broadcast Challenge

Broadcasting in a distributed system means ensuring that a message sent by one node reaches all other nodes. This seems straightforward at first, but becomes complex when we consider:

1. **Message Amplification**: Naively forwarding messages can lead to an explosion in network traffic
2. **Latency**: How quickly can a message reach all nodes in the system
3. **Network Partitions**: What happens when parts of the network can't communicate with each other

## Getting Started

To help you tackle this challenge step-by-step, we've provided three script files:

- `run-goal1.sh`: Tests your solution for Goal 1 (message deduplication)
- `run-goal2.sh`: Tests your solution for Goal 2 (latency optimization)
- `run-goal3.sh`: Tests your solution for Goal 3 (partition tolerance)

To run any test, simply execute the appropriate script:

```bash
./run-goal1.sh
```

These scripts are configured with the appropriate parameters for testing each specific goal.

## The Broadcast Workload

For the broadcast workload, our nodes will handle the following message types:

1. **Init message**: Same as in previous steps, to initialize the node
   ```json
   {
     "src": "c1",
     "dest": "n1",
     "body": {
       "type": "init",
       "msg_id": 1,
       "node_id": "n1",
       "node_ids": ["n1", "n2", "n3", "n4", "n5"]
     }
   }
   ```

2. **Broadcast message**: Sent by clients to request broadcasting a message to all nodes
   ```json
   {
     "src": "c1",
     "dest": "n1",
     "body": {
       "type": "broadcast",
       "msg_id": 2,
       "message": 1000
     }
   }
   ```

3. **Read message**: Request to read all messages seen by a node
   ```json
   {
     "src": "c1",
     "dest": "n1",
     "body": {
       "type": "read",
       "msg_id": 3
     }
   }
   ```

4. **Topology message**: Provides information about the network topology
   ```json
   {
     "src": "c1",
     "dest": "n1",
     "body": {
       "type": "topology",
       "msg_id": 4,
       "topology": {
         "n1": ["n2", "n3"],
         "n2": ["n1"],
         "n3": ["n1"]
       }
     }
   }
   ```

## Implementation Details

We've provided a skeleton implementation in the `Broadcast.java` file. The file includes:
- The basic setup for reading from stdin and writing to stdout
- A `BroadcastServer` class with handlers for all message types
- A naive implementation of broadcast that forwards messages to all nodes

Your task is to improve this implementation in several stages to address the challenges mentioned above.

## Goal 1: Avoiding Message Amplification

### The Problem with Naive Broadcasting

The naive implementation forwards every broadcast message to every other node, which seems reasonable at first:

```java
private void broadcastToAll(int message) throws Exception {
    // Send to all other nodes
    for (String node : nodeIds) {
        if (!node.equals(nodeId)) {
            sendBroadcast(node, message);
        }
    }
}
```

However, this approach causes a serious problem called **message amplification**. Let's understand why:

1. Node A broadcasts a message to nodes B, C, D, and E (4 messages)
2. Node B receives the message and broadcasts to A, C, D, and E (4 more messages)
3. Node C does the same (4 more messages)
4. And so on...

For a single broadcast in a system with N nodes, we end up sending N(N-1) messages! This quickly becomes unsustainable as the system grows.

### Testing Goal 1

To test your solution for avoiding message amplification, run:

```bash
./run-goal1.sh
```

This script runs a basic test with a grid topology and no network latency.

### Implementing a Better Solution

To avoid message amplification, we need to track which messages we've already seen and only forward new ones. This is known as a "flooding" algorithm with deduplication:

1. When a node receives a message, it checks if it has seen this message before
2. If it's a new message, it stores it and forwards it to all other nodes
3. If it's a message it has already seen, it ignores it (no forwarding)

Implement this solution by:
1. Tracking which messages each node has seen
2. Only forwarding messages that haven't been seen before

Once you've implemented your solution, run it with:

```bash
./run-goal1.sh
```

Compare the message flow with the naive implementation - you should see a dramatic reduction in network traffic.

## Goal 2: Improving Latency

### The Problem with Latency

Even after solving the message amplification problem, we still face another challenge: **latency**. In large distributed systems, messages may need to travel through many nodes to reach all parts of the network.

The time it takes for a message to propagate to all nodes is crucial for applications that require quick updates across the system. This becomes especially important as the number of nodes grows.

### Understanding Network Topology

In the real world, networks have specific topologies - the arrangement of nodes and connections between them. This topology information is critical for optimizing message propagation.

In our system, this information is provided through a `topology` message:

```json
{
  "body": {
    "type": "topology",
    "topology": {
      "n1": ["n2", "n3"],
      "n2": ["n1"],
      "n3": ["n1"]
    }
  }
}
```

This tells us which nodes can directly communicate with each other. For example, in the above topology:
- n1 can directly communicate with n2 and n3
- n2 can directly communicate only with n1
- n3 can directly communicate only with n1

### Testing Goal 2

To test your solution for reducing broadcast latency using network topology, run:

```bash
./run-goal2.sh
```

This script uses a tree topology with simulated network latency to test how well your implementation optimizes message propagation.

When examining the results, pay attention to:
- The pattern of message propagation
- How long it takes for messages to reach all nodes
- Which paths messages follow through the network

### Implementing Latency Optimization

To improve latency, we need to leverage the topology information to make smarter decisions about message forwarding:

1. Instead of broadcasting to all nodes, each node should only forward messages to its direct neighbors
2. This way, messages travel through the most efficient paths in the network

Implement this optimization by:
1. Storing and using the topology information provided in the `topology` message
2. Modifying your broadcast logic to only send messages to neighboring nodes

Once you've implemented this optimization, run the test again:

```bash
./run-goal2.sh
```

You should observe:
- Reduced overall latency for message propagation
- Fewer messages being sent across the network
- More efficient use of the network topology

## Goal 3: Handling Network Partitions

### The Challenge of Network Partitions

In real distributed systems, network failures happen regularly. Links between nodes can break, creating what we call **network partitions** - where the network is split into disconnected subgroups that cannot communicate with each other.

This poses a serious challenge for broadcasting:
- How do we ensure messages eventually reach all nodes?
- How do we handle nodes that rejoin after being disconnected?
- How do we maintain consistency across the system despite partitions?

### Testing Goal 3

To test your solution for handling network partitions, run:

```bash
./run-goal3.sh
```

This script introduces network partitions to test how well your implementation handles and recovers from network failures.

When examining the results, focus on:
- How your system behaves during a partition
- Whether messages are eventually delivered to all nodes after partitions heal
- The consistency of the message set across all nodes

### Implementing Partition Tolerance with Gossip

To handle network partitions, we need a more robust approach. This is where **gossip protocols** come in:

1. Instead of simply forwarding new messages, nodes periodically exchange their entire message sets
2. This ensures that even after a partition heals, nodes can catch up on messages they missed
3. The system eventually reaches consistency across all nodes

#### Gossip Protocol Implementation Details

Here's a detailed approach to implementing a gossip protocol for partition tolerance:

##### 1. Background Gossip Thread

You'll need a background mechanism to periodically run the gossip protocol. Virtual threads (introduced in Java 21) are ideal for this task since they're lightweight and efficient:

```java
// Set up a mechanism for periodic gossip
private boolean gossipStarted = false;
private Thread gossipThread;

private void startGossipProtocol() {
    if (!gossipStarted) {
        // Using virtual threads (Java 21+)
        gossipThread = Thread.ofVirtual().name("gossip-thread").start(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    // Run the gossip protocol
                    propagateMessages();
                    
                    // Wait before next gossip round (e.g., 1 second)
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    System.err.println("Error in gossip protocol: " + e.getMessage());
                }
            }
        });
        
        gossipStarted = true;
    }
}
```

##### 2. Message Tracking

You need to track all messages that each node has seen:

```java
// Track messages that need to be propagated to other nodes
private Set<Integer> messagesToPropagate = ConcurrentHashMap.newKeySet();

// Add a message to the set of seen messages and mark it for propagation
private void addMessage(int message) {
    if (messages.add(message)) {
        messagesToPropagate.add(message);
    }
}
```

##### 3. Message Set Exchange

The core of the gossip protocol is the exchange of message sets:

```java
private void propagateMessages() throws Exception {
    if (nodeId == null || messagesToPropagate.isEmpty()) {
        return; // Not initialized or no messages to propagate
    }
    
    // Make a copy to avoid concurrent modification
    Set<Integer> currentMessages = new HashSet<>(messagesToPropagate);
    
    // Send a gossip message to each neighbor
    for (String neighbor : neighbors) {
        sendGossip(neighbor, currentMessages);
    }
}

private void sendGossip(String dest, Set<Integer> messagesToSend) throws Exception {
    ObjectNode body = mapper.createObjectNode();
    body.put("type", "gossip");
    body.put("msg_id", nextMsgId++);
    
    ArrayNode messagesArray = body.putArray("messages");
    for (Integer message : messagesToSend) {
        messagesArray.add(message);
    }
    
    send(dest, body);
}
```

##### 4. Handling Gossip Messages

You need to handle incoming gossip messages and reconcile differences:

```java
// In your handleMessage method, add a case for gossip messages
case "gossip":
    handleGossip(body, src);
    return null;

// Gossip handler method
private void handleGossip(JsonNode body, String src) throws Exception {
    if (body.has("messages")) {
        JsonNode messagesNode = body.get("messages");
        for (JsonNode messageNode : messagesNode) {
            int message = messageNode.asInt();
            addMessage(message);
        }
    }
}
```

##### 5. Error Handling and Resilience

Ensure your implementation is resilient to various failure scenarios:

```java
// Example of robust message sending with error handling
private void sendWithRetry(String dest, ObjectNode body) {
    try {
        send(dest, body);
    } catch (Exception e) {
        // Log error but don't crash - the gossip protocol will retry later
        System.err.println("Failed to send to " + dest + ": " + e.getMessage());
    }
}
```

##### 6. Thread Safety Considerations

When implementing gossip protocols, ensure thread safety for shared data structures:

```java
// Use thread-safe collections for data shared between the main thread and gossip thread
private Set<Integer> messages = Collections.synchronizedSet(new HashSet<>());
// OR
private Set<Integer> messages = ConcurrentHashMap.newKeySet();
```

##### 7. Optimizations

To make your gossip protocol more efficient:

- **Incremental Gossip**: Only send messages the other node hasn't seen yet
- **Push-Pull Gossip**: Ask nodes what messages they have before sending
- **Exponential Backoff**: Increase the interval between gossip attempts if the network is congested
- **Priority-based Propagation**: Prioritize newer messages over older ones

Implement a gossip protocol by following these steps:
1. Adding a periodic gossip mechanism where nodes share their complete message sets
2. Implementing message set reconciliation to identify and share missing messages
3. Ensuring each node eventually receives all messages, even after network partitions

Once you've implemented your gossip-based solution, run:

```bash
./run-goal3.sh
```

You should observe:
- Messages eventually reaching all nodes despite partitions
- The system recovering after partitions heal
- Eventual consistency across all nodes in the system

## Analyzing Results

After completing your implementation, you can analyze the results in detail:

1. **Visualizing Message Flow**: Maelstrom generates detailed visualizations in its store directory
   ```bash
   cd /tmp/maelstrom-store
   ```
   Then open a web browser to http://localhost:8080 and examine:
   - `messages.svg` to see the message flow between nodes
   - `rate.png` to see the message rate over time
   - Check the logs for details on message counts

2. **Understanding Performance Metrics**: Pay attention to:
   - **Message Count**: How many messages were sent in total
   - **Convergence Time**: How long it took for all nodes to receive all messages
   - **Recovery Time**: How quickly the system recovered after partitions

## Conclusion

In this step, you've tackled three fundamental challenges in distributed systems:
1. **Message Efficiency**: Avoiding the explosion of duplicate messages
2. **Latency Optimization**: Using topology information to minimize propagation time
3. **Partition Tolerance**: Ensuring the system works despite network failures

These concepts are fundamental to many real-world distributed systems, from distributed databases to messaging platforms.

Next steps to consider:
- How would you optimize further for very large networks?
- What trade-offs exist between consistency, availability, and partition tolerance?
- How might you handle node failures in addition to network partitions?
