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

## Getting Started

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

### Implementing a Better Solution

To avoid message amplification, we need to track which messages we've already seen and only forward new ones. This is known as a "flooding" algorithm with deduplication:

1. When a node receives a message, it checks if it has seen this message before
2. If it's a new message, it stores it and forwards it to all other nodes
3. If it's a message it has already seen, it ignores it (no forwarding)

Implement this solution by:
1. Tracking which messages each node has seen
2. Only forwarding messages that haven't been seen before

## Running the Broadcast Test

We've provided a run script that will test your implementation with Maelstrom:

```bash
./run.sh
```

This will execute a test with 5 nodes at a moderate message rate. The test will measure:
- Message count (how many messages were sent/received)
- Latency (how quickly messages propagate)
- Correctness (whether all nodes received all messages)

## Viewing Test Results

After running the test, you can visualize the results using:

```bash
../bin/maelstrom serve
```

Then open a web browser to http://localhost:8080 and examine:
- `messages.svg` to see the message flow between nodes
- `rate.png` to see the message rate over time
- Check the logs for details on message counts

In the naive implementation, you'll see an explosion of messages as each broadcast propagates through the network. Compare this with your improved implementation, which should show a dramatic reduction in message count.

## Goal 2: Improving Latency

After solving the message amplification problem, we'll tackle latency in a future implementation. The goal will be to reduce the time it takes for a message to reach all nodes in the system.

## Goal 3: Handling Network Partitions

The final challenge will be to make our broadcast system resilient to network partitions, where parts of the network can't communicate with each other.

## Next Steps

Once you've implemented and tested your solution for Goal 1, you'll move on to Goals 2 and 3 with more advanced broadcast implementations.
