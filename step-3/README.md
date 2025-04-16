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

## Message Exchange Pattern

```
Message Flow:
-------------
1. Init:
   Client ----[init]----> Broadcast Node
   Broadcast Node ----[init_ok]----> Client

2. Topology:
   Client ----[topology]----> Broadcast Node
   Broadcast Node ----[topology_ok]----> Client

3. Broadcast:
   Client ----[broadcast, 1000]----> Node n1
   Node n1 ----[broadcast_ok]----> Client
   
   Node n1 ----[broadcast, 1000]----> Node n2
   Node n1 ----[broadcast, 1000]----> Node n3
   
4. Broadcast Another Value:
   Client ----[broadcast, 2000]----> Node n2
   Node n2 ----[broadcast_ok]----> Client
   
   Node n2 ----[broadcast, 2000]----> Node n1
   Node n2 ----[broadcast, 2000]----> Node n3
   
5. Read (All Nodes Have Both Values):
   Client ----[read]----> Node n1
   Node n1 ----[read_ok, [1000, 2000]]----> Client
   
   Client ----[read]----> Node n2
   Node n2 ----[read_ok, [1000, 2000]]----> Client
   
   Client ----[read]----> Node n3
   Node n3 ----[read_ok, [1000, 2000]]----> Client
```

## JSON Exchange Examples

Here are the complete JSON messages exchanged in the broadcast workload:

1. **Client Broadcast Request**:
```json
{
  "src": "c1",
  "dest": "n1",
  "body": {
    "type": "broadcast",
    "msg_id": 1,
    "message": 1000
  }
}
```

2. **Node Response to Broadcast**:
```json
{
  "src": "n1",
  "dest": "c1",
  "body": {
    "type": "broadcast_ok",
    "msg_id": 2,
    "in_reply_to": 1
  }
}
```

3. **Node-to-Node Broadcast Message**:
```json
{
  "src": "n1",
  "dest": "n2",
  "body": {
    "type": "broadcast",
    "message": 1000
  }
}
```

4. **Client Read Request**:
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

5. **Node Response to Read**:
```json
{
  "src": "n1",
  "dest": "c1",
  "body": {
    "type": "read_ok",
    "msg_id": 4,
    "in_reply_to": 3,
    "messages": [1000, 2000, 3000]
  }
}
```

6. **Topology Message**:
```json
{
  "src": "c1",
  "dest": "n1",
  "body": {
    "type": "topology",
    "msg_id": 5,
    "topology": {
      "n1": ["n2", "n3"],
      "n2": ["n1"],
      "n3": ["n1"]
    }
  }
}
```

7. **Topology Response**:
```json
{
  "src": "n1",
  "dest": "c1",
  "body": {
    "type": "topology_ok",
    "msg_id": 6,
    "in_reply_to": 5
  }
}
```

For the complete Maelstrom protocol documentation, please refer to the [official Maelstrom protocol documentation](https://github.com/jepsen-io/maelstrom/blob/main/doc/protocol.md).

## Getting Started

To help you tackle this challenge step-by-step, we've provided three script files:

- `run-goal1.sh`: Tests your solution for Goal 1 (message deduplication)
- `run-goal2.sh`: Tests your solution for Goal 2 (latency optimization)
- `run-goal3.sh`: Tests your solution for Goal 3 (partition tolerance)

To run any test, simply execute the appropriate script:

```bash
# Make sure you're in the step-3 directory
cd step-3  # if you're not already in this directory

# Start the visualization server in a separate terminal
../bin/maelstrom serve

# Run the goal 1 test
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
# Make sure you're in the step-3 directory
cd step-3  # if you're not already in this directory

# Start the visualization server in a separate terminal
../bin/maelstrom serve

# Run the goal 1 test
./run-goal1.sh
```

This script runs a basic test with a grid topology and no network latency.

### Implementing a Better Solution

To avoid message amplification, we need to track which messages we've already seen and only forward new ones. This is known as a "flooding" algorithm with deduplication:

1. When a node receives a message, it checks if it has seen this message before
2. If it's a new message, it stores it and forwards it to all other nodes
3. If it's a message it has already seen, it ignores it (no forwarding)

### ✅ TODO: Implement Message Deduplication

**Your implementation tasks:**

- [ ] **Track which messages each node has seen**
- [ ] **Only forward messages that haven't been seen before**

Once you've implemented your solution, run it with:

```bash
# Make sure you're in the step-3 directory
cd step-3  # if you're not already in this directory

# Start the visualization server in a separate terminal
../bin/maelstrom serve

# Run the goal 1 test
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
# Make sure you're in the step-3 directory
cd step-3  # if you're not already in this directory

# Start the visualization server in a separate terminal (if not already running)
../bin/maelstrom serve

# Run the goal 2 test
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

### ✅ TODO: Implement Latency Optimization

**Your implementation tasks:**

- [ ] **Store and use the topology information** provided in the `topology` message
- [ ] **Modify your broadcast logic** to only send messages to neighboring nodes

Once you've implemented this optimization, run the test again:

```bash
# Make sure you're in the step-3 directory
cd step-3  # if you're not already in this directory

# Start the visualization server in a separate terminal (if not already running)
../bin/maelstrom serve

# Run the goal 2 test
./run-goal2.sh
```

You should observe:
- Reduced overall latency for message propagation
- Fewer messages being sent across the network
- More efficient use of the network topology

### Experimenting with Network Conditions

To truly understand how different network topologies and latency settings affect your distributed system, try modifying the `run-goal2.sh` script to experiment with various configurations:

```bash
# Look for these variables
TOPOLOGY="tree4"   # Try changing to "line", "grid", "tree2", "tree3", "tree4", "total"
LATENCY="100"   # Try different values like "50", "200", "500"
```

When you increase these parameters, observe:
- **How message volume scales** with more nodes and higher message rates
- **How gossip frequency affects** convergence time with higher loads
- **Where bottlenecks appear** in your implementation as the system scales

This kind of experimentation is crucial for understanding the real-world performance characteristics of distributed systems. In production environments, systems often behave differently at scale than they do with just a few nodes.

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
# Make sure you're in the step-3 directory
cd step-3  # if you're not already in this directory

# Start the visualization server in a separate terminal (if not already running)
../bin/maelstrom serve

# Run the goal 3 test
./run-goal3.sh
```

This script introduces network partitions to test how well your implementation handles and recovers from network failures.

When examining the results, focus on:
- How your system behaves during a partition
- Whether messages are eventually delivered to all nodes after partitions heal
- The consistency of the message set across all nodes

### ✅ TODO: Implement Partition Tolerance

**Your implementation tasks:**

- [ ] **Create a background gossip mechanism** to periodically exchange messages
- [ ] **Implement message tracking** to keep track of which messages need to be propagated
- [ ] **Build a message exchange protocol** to share messages between nodes
- [ ] **Handle gossip messages** from other nodes to reconcile differences

Once you've implemented these features, run:

```bash
# Make sure you're in the step-3 directory
cd step-3  # if you're not already in this directory

# Start the visualization server in a separate terminal (if not already running)
../bin/maelstrom serve

# Run the goal 3 test
./run-goal3.sh
```

You should observe:
- Messages eventually reaching all nodes despite partitions
- The system recovering after partitions heal
- Eventual consistency across all nodes in the system

### Gossip Protocol: Implementation Guide

To handle network partitions, we need a robust approach called a **gossip protocol**:

1. Instead of simply forwarding new messages, nodes periodically exchange their entire message sets
2. This ensures that even after a partition heals, nodes can catch up on messages they missed
3. The system eventually reaches consistency across all nodes

Here's how to implement an effective gossip protocol:

#### 1. Background Gossip Thread

Create a mechanism to periodically run gossip in the background:

```java
// Set up a mechanism for periodic gossip using virtual threads (Java 21+)
private boolean gossipStarted = false;
private Thread gossipThread;

private void startGossipProtocol() {
    if (!gossipStarted) {
        gossipThread = Thread.ofVirtual().name("gossip-thread").start(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    // Run the gossip protocol periodically
                    propagateMessages();
                    Thread.sleep(1000); // Wait between rounds
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

#### 2. Message Tracking

Track messages that need to be propagated:

```java
// We need two separate sets for different purposes:
// 1. 'messages' - stores all messages we've seen (our local state)
// 2. 'messagesToPropagate' - tracks which messages need to be sent to other nodes
// Both are thread-safe to handle concurrent access from multiple threads
private Set<Integer> messages = ConcurrentHashMap.newKeySet();
private Set<Integer> messagesToPropagate = ConcurrentHashMap.newKeySet();

// Add a message to the set and mark it for propagation
private void addMessage(int message) {
    if (messages.add(message)) {
        messagesToPropagate.add(message);
    }
}
```

#### 3. Message Exchange via Gossip

The gossip protocol consists of two main parts:

1. **Sending gossip messages**: Periodically share your message set with other nodes
2. **Processing received gossip**: Handle gossip messages received from other nodes

First, implement a method to send gossip messages to other nodes:

```java
// Send gossip messages to a neighbor
private void sendGossip(String dest) throws Exception {
    // TODO: Create a gossip message containing all your known messages
    // TODO: Send this message to the selected neighbor
    // This enables message sharing even during network partitions
}
```

Then, handle incoming gossip messages:

```java
// In your handleMessage method, add a case for gossip messages
case "gossip":
    handleGossip(body, src);
    return null;

// Gossip handler method
private void handleGossip(JsonNode body, String src) throws Exception {
    // TODO: Extract messages from the gossip message
    // TODO: Add each received message to your local message set
    // This is where reconciliation happens - when you receive a message
    // you haven't seen before, you should add it to your own set and 
    // it will be propagated in future gossip rounds
}
```

### Best Practices and Considerations

#### Key Implementation Strategies

- **Complete message set exchange**: Share your entire message set during gossip, not just new messages
- **Random neighbor selection**: Select a random subset of neighbors for each gossip round
- **Periodic execution**: Run the gossip protocol continuously in the background
- **Thread safety**: Use thread-safe collections for shared data structures
- **Error handling**: Ensure the gossip thread continues running despite individual failures

#### Common Pitfalls to Avoid

- **Over-optimization**: Don't minimize message exchange too aggressively
- **Static neighbor selection**: Avoid always gossiping with the same neighbors
- **Insufficient gossip frequency**: If gossip happens too rarely, recovery is slow
- **Missing error handling**: Exceptions shouldn't terminate the gossip process

#### Advanced Optimizations

Once your basic implementation works, consider these improvements:

- **Incremental Gossip**: Only send messages the other node hasn't seen yet
- **Push-Pull Gossip**: Ask nodes what messages they have before sending
- **Exponential Backoff**: Increase the interval between gossip attempts when congested
- **Priority-based Propagation**: Prioritize newer messages over older ones

### Experimenting with Scaling Parameters

Now that you have a working partition-tolerant broadcast system, you can experiment with how it behaves under different loads and scales:

```bash
# Edit the run-goal3.sh script to try different parameters
# Look for these variables:
--node-count 3     # Try increasing to 5, 10, or more nodes
--rate 1           # Try increasing to 5, 10, or 20 messages per second
```

When you increase these parameters, observe:
- **How message volume scales** with more nodes and higher message rates
- **How gossip frequency affects** convergence time with higher loads
- **Where bottlenecks appear** in your implementation as the system scales

This kind of experimentation is crucial for understanding the real-world performance characteristics of distributed systems. In production environments, systems often behave differently at scale than they do with just a few nodes.

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
