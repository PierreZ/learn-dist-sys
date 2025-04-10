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

## Testing Your Implementation

For each goal, we've provided a dedicated run script:

### Goal 1: Testing Basic Broadcast
To test your solution for avoiding message amplification:

```bash
./run-goal1.sh
```

This script runs a basic test with a grid topology and no network latency.

### Goal 2: Testing Latency Optimization
To test your solution for reducing broadcast latency using network topology:

```bash
./run-goal2.sh
```

This script uses a tree topology with simulated network latency to test how well your implementation optimizes message propagation.

### Goal 3: Testing Partition Tolerance
To test your solution for handling network partitions:

```bash
./run-goal3.sh
```

This script introduces network partitions to test how well your implementation handles and recovers from network failures.

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

### Running the Naive Implementation to Observe Message Amplification

To see this problem in action, run the naive implementation with:

```bash
./run-goal1.sh
```

Watch the output closely - you'll see a flood of messages being exchanged between nodes. Pay attention to:
- How many times the same message is sent across the network
- How messages continually bounce between nodes
- The exponential growth in network traffic even with just a few nodes

This visualization of message amplification helps you understand why we need a better approach for broadcasting.

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

### Using Topology Information

Maelstrom provides topology information through the `topology` message, which tells each node who its neighbors are. For example:

```json
{
  "type": "topology",
  "topology": {
    "n1": ["n2", "n3"],
    "n2": ["n1"],
    "n3": ["n1"]
  }
}
```

This topology indicates that node n1 is connected to n2 and n3, while n2 and n3 are only connected to n1. This forms a "star" topology with n1 at the center.

By using this topology information intelligently, we can create more efficient message propagation patterns:

1. Instead of each node broadcasting to all other nodes, nodes only send messages to their neighbors
2. The topology can be designed to minimize the number of "hops" needed for a message to reach all nodes
3. Different topologies (like trees or grids) have different latency characteristics

### Testing with Different Topologies

Our run script now includes variables for `TOPOLOGY` and `LATENCY` that you can modify to test different configurations:

```bash
# Change these values to test different configurations
TOPOLOGY="tree4"
LATENCY=100
```

The topologies available are:
- `grid`: Nodes form a 2D grid
- `tree2`: Nodes form a binary tree
- `tree3`: Nodes form a tree with 3 children per node
- `tree4`: Nodes form a tree with 4 children per node

### Implementing a Low-Latency Solution

To improve latency, you need to create an implementation that:

1. Properly processes the topology information to identify neighbors
2. Only forwards messages to its neighbors (instead of all nodes)
3. Takes advantage of tree-based topologies to minimize propagation time

To test different topologies, modify the run script:

```bash
# Change these values to test different configurations
TOPOLOGY="tree4"
LATENCY=100
```

This approach significantly reduces both the number of messages and the time required for broadcasts to reach all nodes in the network.

## Goal 3: Handling Network Partitions

The final challenge is to make our broadcast system resilient to **network partitions** - situations where some nodes become temporarily unable to communicate with each other.

### The Challenge of Network Partitions

In real distributed systems, network failures are inevitable:
- Network connections can fail temporarily
- Messages can be lost or delayed
- Parts of the network can become completely isolated from others

When these partitions occur, our broadcast system should:
1. Continue functioning within each partition
2. Eventually reach consistency when partitions heal
3. Ensure that all messages eventually propagate to all nodes

### Implementing Partition Tolerance

To handle network partitions effectively, we need several mechanisms:

#### 1. Message Persistence
- Keep track of all messages that need to be delivered
- Don't assume a message is delivered just because it was sent

#### 2. Retry Logic
- Implement a mechanism to retry failed message deliveries
- Use an exponential backoff strategy to avoid overwhelming the network

#### 3. Gossip Protocol
- Periodically share message state with neighbors
- This helps messages propagate even when direct paths are unavailable

#### 4. Conflict Resolution
- When partitions heal, nodes may have inconsistent state
- Implement a strategy to reconcile differences

## Viewing Test Results

After running any of the tests, you can visualize the results using:

```bash
../bin/maelstrom serve
```

Then open a web browser to http://localhost:8080 and examine:
- `messages.svg` to see the message flow between nodes
- `rate.png` to see the message rate over time
- Check the logs for details on message counts

In the naive implementation, you'll see an explosion of messages as each broadcast propagates through the network. Compare this with your improved implementation, which should show a dramatic reduction in message count.

## Conclusion

By completing these three goals, you've built a robust broadcast system that:
1. Avoids message amplification through careful message tracking
2. Minimizes latency by leveraging network topology
3. Handles network partitions to ensure eventual consistency

These concepts are fundamental to building reliable distributed systems in real-world environments.

## Next Steps

After completing this step, you'll be ready to tackle more complex distributed systems challenges in the subsequent steps, building on the foundational principles you've learned here.
