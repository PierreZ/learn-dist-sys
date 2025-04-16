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

## Message Exchange Pattern

```
Message Flow:
-------------
1. Init:
   Client/Maelstrom ----[init]----> Echo Node
   Echo Node ----[init_ok]----> Client/Maelstrom

2. Echo:
   Client ----[echo, "hello"]----> Echo Node
   Echo Node ----[echo_ok, "hello"]----> Client
```

## Client-Response Communication

The echo server in this step demonstrates the basic client-response communication pattern used throughout Maelstrom:

```
    Client c1                     Server n1
       |                             |
       |        Request Message      |
       | -------------------------> |
       |   {                         |
       |     "src": "c1",            |
       |     "dest": "n1",           |
       |     "body": {               |
       |       "type": "echo",       |
       |       "msg_id": 123,        |
       |       "echo": "hello"       |
       |     }                       |
       |   }                         |
       |                             |
       |                             | [Processing]
       |                             |
       |        Response Message     |
       | <-------------------------- |
       |   {                         |
       |     "src": "n1",            |
       |     "dest": "c1",           |
       |     "body": {               |
       |       "type": "echo_ok",    |
       |       "msg_id": 456,        |
       |       "in_reply_to": 123,   |
       |       "echo": "hello"       |
       |     }                       |
       |   }                         |
```

The key elements of this communication pattern are:

1. **Request-Response**: The client sends a request (e.g., "echo") and expects a response
2. **Message IDs**: Each message has a unique `msg_id` for tracking purposes
3. **Reply Correlation**: The response includes an `in_reply_to` field matching the original request's `msg_id`
4. **Message Types**: Request types typically have corresponding "_ok" response types (e.g., "echo" → "echo_ok")
5. **Message Structure**: Both requests and responses follow the same outer structure with `src`, `dest`, and `body`

This pattern is consistent across all Maelstrom workloads, allowing for a standardized way to handle distributed system communications.

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

For the complete Maelstrom protocol documentation, please refer to the [official Maelstrom protocol documentation](https://github.com/jepsen-io/maelstrom/blob/main/doc/protocol.md).

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

## Logging Best Practices

When implementing distributed systems with Maelstrom, proper logging is crucial for debugging and understanding system behavior. Follow these guidelines:

1. **Use STDERR for all logging**: Maelstrom expects messages on STDOUT and debugging on STDERR
   ```java
   // Correct way to log debug information
   System.err.println("Processing message: " + messageType);
   
   // INCORRECT - never do this as it will corrupt the protocol
   // System.out.println("Debug info");
   ```

2. **Log initialization**: Always log when your node is initialized with its ID
   ```java
   nodeId = body.get("node_id").asText();
   System.err.println("Node " + nodeId + " initialized");
   ```

3. **Log important state changes**: Log when significant events occur
   ```java
   System.err.println("Received message " + messageId + " of type " + messageType);
   ```

4. **View logs in Maelstrom's store**: After running tests, you can find logs at:
   ```
   /tmp/maelstrom-store/<test-name>/node-logs/
   ```

Remember, any output to STDOUT that is not a correctly formatted message will break the Maelstrom protocol and cause your tests to fail.

## Getting Started

We've provided a boilerplate implementation in the `Echo.java` file to help you get started. The file includes:

- The basic setup for reading from stdin and writing to stdout
- A skeleton `EchoServer` class with placeholders for message handling
- An implementation of the `init` message handler
- TODOs where you need to implement the echo functionality

### ✅ TODO: Implement the Echo Server

**Your implementation tasks:**

- [ ] **Create an `echo_ok` response handler** that properly responds to echo requests
- [ ] **Pass the original echo message back** to the client in your response
- [ ] **Include the proper `in_reply_to` field** to link your response to the request

Your task is to implement the echo message handler to properly respond to echo requests from Maelstrom.

If you get stuck or want to see a complete solution, you can reference the `SolutionEchoServer.java` file which contains a fully working implementation.

## Running the Echo Server

We've provided a `run.sh` script that will build and execute the echo test with Maelstrom:

```bash
# Make sure you're in the step-1 directory
cd step-1  # if you're not already in this directory

# Start the visualization server in a separate terminal
../bin/maelstrom serve

# Run the echo server test
./run.sh
```

This script will:
1. Make sure your Echo.java file is executable
2. Build it with JBang to check for compilation errors
3. Run the Maelstrom test with appropriate parameters

You should see Maelstrom's output indicating whether your test passed or failed.

## Debug Visualization

If you want to visualize the message flow, after running the test, open a web browser to http://localhost:8080.

## Next Steps

In [Step 2](../step-2), we'll tackle a more complex challenge: generating unique IDs in a distributed system.

## Troubleshooting

### "Command not found" for jbang

If you encounter an error like:

```
./Echo.java: line 1: jbang: command not found
```

Make sure JBang is properly installed or refer to [Step 0](../step-0) for installation instructions.

### JSON Parse Error

If you get JSON parsing errors, check that:
1. Your JSON is well-formed
2. You're properly escaping strings
3. You're using the correct field names in your response