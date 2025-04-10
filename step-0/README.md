# Distributed Systems Lab - Step 0: Getting Started

## Introduction

Welcome to this hands-on lab on distributed systems! In this lab, you'll be using Maelstrom to test and verify distributed systems implementations. We'll be writing our implementations in Java using JBang.

This lab is broken down into steps of increasing complexity:
- Step 0 (this step): Introduction to Maelstrom and JBang
- Step 1-3: Implementing basic distributed systems patterns
- And more to follow!

## What is Maelstrom?

[Maelstrom](https://github.com/jepsen-io/maelstrom) is a workbench for learning distributed systems by writing your own implementations of distributed algorithms. It's part of the [Jepsen](https://jepsen.io/) suite of tools for distributed systems testing.

Key features of Maelstrom:
- Simulates a distributed environment on a single machine
- Handles network communication between nodes
- Introduces chaos (like network partitions) to test system resilience
- Verifies correctness of your implementation against formal specifications

Maelstrom is specifically designed as an educational tool to help developers learn about distributed systems concepts in a practical, hands-on way. It provides a safe, simulated environment where you can build "toy" versions of distributed systems without the complexity of deploying actual infrastructure. This allows you to:

- Focus on the core algorithms rather than infrastructure details
- Experiment with different approaches in a controlled setting
- Learn about failure modes without affecting real systems
- Develop intuition about distributed systems behavior

Maelstrom allows you to focus on implementing the core algorithms while it handles the testing infrastructure. You can build distributed systems in any language that can read from stdin and write to stdout since that's how Maelstrom communicates with your nodes.

## Preparing for Step 1

In the subsequent steps, we'll build increasingly complex distributed systems using Maelstrom and JBang. We'll start with a simple echo server and work our way up to a broadcast system and eventually a distributed key-value store.

> **Note:** Before proceeding, make sure you've run the `setup.sh` script from the root directory as described in the main README. This will install Maelstrom and check for required dependencies.

For now, make sure you have:
1. JDK 23 or higher installed
2. JBang installed
3. Maelstrom installed

You can verify your installation by running these commands:

```bash
# Check Java version
java -version
# Should show Java 23 or higher

# Check JBang is installed
jbang --version

# Check Graphviz (required by Maelstrom)
dot -V

# Check Gnuplot (required by Maelstrom)
gnuplot --version

# Check Maelstrom is installed and working
maelstrom help
```

Ensure all commands return valid responses before proceeding to the next steps.

## What is JBang?

[JBang](https://www.jbang.dev/) is a tool that simplifies running and scripting Java code. It makes Java feel more like a scripting language by eliminating the need for project structures, build tools, or complex IDE setups.

Key features of JBang:
- Run Java source files directly (no explicit compilation step)
- Easily add dependencies with simple annotations
- Create scripts with proper shebang support
- No need for `pom.xml`, `build.gradle`, or other build system files

Instead of dealing with all the Java project boilerplate, you can just write your code and run it immediately.

## JBang Example

Let's create a simple "Hello World" example with JBang to get familiar with it.

1. Create a file named `HelloWorld.java` in the `step-0` directory:

```java
///usr/bin/env jbang "$0" "$@" ; exit $?

class HelloWorld {
    public static void main(String[] args) {
        System.out.println("Hello, Distributed Systems!");
    }
}
```

2. Run the file with JBang:

```bash
jbang HelloWorld.java
```

You should see the output: `Hello, Distributed Systems!`

## Building with JBang

In addition to running Java files directly, JBang can also build your code into a JAR file for distribution:

```bash
jbang build HelloWorld.java
```

This will create a standalone JAR file with all dependencies included. You can find the built JAR in the `.jbang/cache` directory. This is useful when you want to:

- Share your application with others
- Deploy your code to environments where JBang isn't installed
- Create distributable packages of your application

## Using Dependencies with JBang

One of JBang's powerful features is the ability to add dependencies with simple annotations. Let's see how to add and use a dependency:

```java
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
```

Save this as `JsonExample.java` and run it with `jbang JsonExample.java`. JBang will automatically download the Jackson dependency and run your code.

## Why Testing Distributed Systems is Important

Distributed systems are notoriously difficult to build correctly. Research has shown that many production distributed systems experience failures due to network partitions. The paper ["An Analysis of Network-Partitioning Failures in Cloud Systems"](https://www.usenix.org/conference/osdi18/presentation/alquraan) from OSDI 2018 demonstrated that a significant number of catastrophic failures in modern distributed systems are caused by network partitions, and many of these issues could have been caught with proper testing.

Maelstrom helps address this problem by simulating network partitions and other chaotic conditions, allowing you to test your system's resilience to these real-world scenarios.

## Next Steps

Once you're comfortable with JBang, head over to Step 1 where we'll implement our first simple distributed system protocol!
