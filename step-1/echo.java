///usr/bin/env jbang --quiet "$0" "$@" ; exit $?
//DEPS com.fasterxml.jackson.core:jackson-databind:2.15.2

import static java.lang.System.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Scanner;

public class echo {
    private static final ObjectMapper mapper = new ObjectMapper();

    public static void main(String... args) {
        Scanner scanner = new Scanner(System.in);

        EchoNode node = new EchoNode();
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            try {
                JsonNode message = mapper.readTree(line);
                String src = message.get("src").asText();
                String dest = message.get("dest").asText();
                JsonNode body = message.get("body");
                String type = body.get("type").asText();

                switch (type) {
                    case "init":
                        node.initialize(src, dest, body);
                        break;

                    case "echo":
                        node.echo(src, dest, body);
                        break;

                    default:
                        node.log("Unknown message type: " + type);
                        break;
                }

            } catch (Exception e) {
                node.log("Error processing message: " + e.getMessage() + "\nInput was: " + line);
            }
        }
    }
}

class EchoNode {
    protected final ObjectMapper mapper = new ObjectMapper();
    String nodeId = new String();

    public void initialize(String src, String dest, JsonNode requestBody) {
        this.nodeId = dest;
        ObjectNode responseBody = mapper.createObjectNode();
        responseBody.put("type", "init_ok");
        responseBody.put("in_reply_to", requestBody.get("msg_id").asInt());

        this.reply(dest, src, responseBody);
    }

    void echo(String src, String dest, JsonNode requestBody) {
        ObjectNode responseBody = mapper.createObjectNode();
        responseBody.put("type", "echo_ok");
        responseBody.put("in_reply_to", requestBody.get("msg_id").asInt());
        responseBody.set("echo", requestBody.get("echo"));

        this.reply(dest, src, responseBody);
    }

    void log(String message) {
        System.err.println(message);
    }

    void reply(String src, String dest, JsonNode body) {
        try {
            ObjectNode response = mapper.createObjectNode();
            response.put("src", src);
            response.put("dest", dest);
            response.set("body", body);

            System.out.println(mapper.writeValueAsString(response));
        } catch (Exception e) {
            log("Error sending reply: " + e.getMessage());
        }
    }
}