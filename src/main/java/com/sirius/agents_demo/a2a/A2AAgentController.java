package com.sirius.agents_demo.a2a;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;
import io.a2a.spec.AgentCard;
import io.a2a.spec.Artifact;
import io.a2a.spec.Task;
import io.a2a.spec.TaskState;
import io.a2a.spec.TaskStatus;
import io.a2a.spec.TextPart;
import java.time.OffsetDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

//https://spring.io/blog/2026/01/29/spring-ai-agentic-patterns-a2a-integration

/**
 * Serves A2A-compatible HTTP endpoints for every registered specialist agent.
 * <p>
 * Endpoints exposed per agent (where {agentId} is e.g. "info-fetch"):
 * <pre>
 *   GET  /agents/{agentId}/.well-known/agent-card.json  – standard A2A discovery
 *   GET  /agents/{agentId}/card                         – alternative card endpoint
 *   POST /agents/{agentId}                              – JSON-RPC 2.0 sendMessage
 * </pre>
 * <p>
 * The JSON-RPC layer is intentionally minimal: we only implement the
 * {@code message/send} method as required by {@link org.springaicommunity.agent.subagent.a2a.A2ASubagentExecutor}.
 */
@RestController
@RequestMapping("/agents/{agentId}")
public class A2AAgentController {

    private static final Logger log = LoggerFactory.getLogger(A2AAgentController.class);

    private final Map<String, AgentRegistration> registry;
    private final ObjectMapper objectMapper = tools.jackson.databind.json.JsonMapper.builder().build();

    public A2AAgentController(Map<String, AgentRegistration> agentRegistrationMap) {
        this.registry = agentRegistrationMap;
    }

    // -------------------------------------------------------------------------
    // AgentCard discovery
    // -------------------------------------------------------------------------

    @GetMapping(value = "/.well-known/agent-card.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AgentCard> agentCardWellKnown(@PathVariable String agentId) {
        return cardResponse(agentId);
    }

    @GetMapping(value = "/card", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AgentCard> agentCardAlt(@PathVariable String agentId) {
        return cardResponse(agentId);
    }

    private ResponseEntity<AgentCard> cardResponse(String agentId) {
        AgentRegistration reg = registry.get(agentId);
        if (reg == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(reg.agentCard());
    }

    // -------------------------------------------------------------------------
    // JSON-RPC message/send endpoint
    // -------------------------------------------------------------------------

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ObjectNode> handleJsonRpc(
            @PathVariable String agentId,
            @RequestBody JsonNode requestBody) {

        AgentRegistration reg = registry.get(agentId);
        if (reg == null) {
            return ResponseEntity.notFound().build();
        }

        log.info("Agent '{}' raw A2A request: {}", agentId, requestBody);

        String method = requestBody.path("method").asText("");
        JsonNode id = requestBody.path("id");

        if (!"message/send".equals(method)) {
            return ResponseEntity.ok(errorResponse(id, -32601, "Method not found: " + method));
        }

        // Extract the user text from the first TextPart in the message
        String userText = extractUserText(requestBody);
        if (userText == null || userText.isBlank()) {
            return ResponseEntity.ok(errorResponse(id, -32602, "No text found in message parts"));
        }

        // contextId ties the task back to the session; fall back to a fresh UUID if absent
        String contextId = requestBody.path("params").path("message").path("contextId").asText("");
        if (contextId.isBlank()) {
            contextId = UUID.randomUUID().toString();
        }

        log.info("Agent '{}' received message/send: {}", agentId, userText);

        try {
            String reply = reg.chatClient()
                    .prompt()
                    .user(userText)
                    .call()
                    .content();

            Task task = buildCompletedTask(contextId, reply);
            ObjectNode response = successResponse(id, task);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Agent '{}' error processing request", agentId, e);
            return ResponseEntity.ok(errorResponse(id, -32603, "Internal error: " + e.getMessage()));
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private String extractUserText(JsonNode requestBody) {
        JsonNode parts = requestBody.path("params").path("message").path("parts");
        if (parts.isMissingNode() || !parts.isArray()) {
            log.warn("No 'parts' array found in params.message; body: {}", requestBody);
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (JsonNode part : parts) {
            // A2A spec uses "kind":"text" as the discriminator; fall back to any part with a "text" field
            String kind = part.path("kind").asText("");
            JsonNode textNode = part.path("text");
            if (("text".equals(kind) || kind.isBlank()) && !textNode.isMissingNode() && textNode.isTextual()) {
                sb.append(textNode.asText(""));
            }
        }
        return sb.toString();
    }

    private Task buildCompletedTask(String contextId, String replyText) {
        String taskId = UUID.randomUUID().toString();
        TextPart textPart = new TextPart(replyText, null);
        Artifact artifact = new Artifact.Builder()
                .artifactId(UUID.randomUUID().toString())
                .parts(List.of(textPart))
                .build();
        TaskStatus status = new TaskStatus(TaskState.COMPLETED, null, OffsetDateTime.now());
        return new Task.Builder()
                .id(taskId)
                .contextId(contextId)
                .status(status)
                .artifacts(List.of(artifact))
                .build();
    }

    private ObjectNode successResponse(JsonNode id, Task task) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("jsonrpc", "2.0");
        root.set("id", id);
        root.set("result", objectMapper.valueToTree(task));
        return root;
    }

    private ObjectNode errorResponse(JsonNode id, int code, String message) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("jsonrpc", "2.0");
        root.set("id", id);
        ObjectNode error = root.putObject("error");
        error.put("code", code);
        error.put("message", message);
        return root;
    }
}
