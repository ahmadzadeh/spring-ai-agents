package com.sirius.agents_demo.a2a;

import io.a2a.spec.AgentCapabilities;
import io.a2a.spec.AgentCard;
import io.a2a.spec.AgentSkill;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Registers each specialist agent as an {@link AgentRegistration} (AgentCard + ChatClient),
 * then exposes all registrations in a map keyed by agent id.
 * <p>
 * The orchestrator is intentionally excluded from the registry because it is the
 * coordinator, not a target agent for A2A calls.
 */
@Configuration
public class AgentRegistry {

    @Value("${server.port:2020}")
    private int serverPort;

    @Bean
    public AgentRegistration infoFetchRegistration(ChatClient infoFetchChatClient) {
        AgentCard card = buildCard(
                "info-fetch",
                "Supplier Info Fetch Agent",
                "Gathers publicly available digital evidence about a supplier using web search: " +
                "company identity, certifications, ESG, cybersecurity posture, adverse media.",
                List.of("supplier", "research", "web-search"),
                List.of("info_fetch", "What is the digital footprint of supplier ACME GmbH?")
        );
        return new AgentRegistration(card, infoFetchChatClient);
    }

    @Bean
    public AgentRegistration initialAssessmentRegistration(ChatClient initialAssessmentChatClient) {
        AgentCard card = buildCard(
                "initial-assessment",
                "Supplier Initial Assessment Agent",
                "Transforms raw evidence into a structured supplier digital profile summary " +
                "with completeness scoring and red-flag detection.",
                List.of("supplier", "assessment", "profiling"),
                List.of("initial_assessment", "Build a structured profile for this evidence: ...")
        );
        return new AgentRegistration(card, initialAssessmentChatClient);
    }

    @Bean
    public AgentRegistration riskAssessmentRegistration(ChatClient riskAssessmentChatClient) {
        AgentCard card = buildCard(
                "risk-assessment",
                "Supplier Risk Assessment Agent",
                "Scores supplier risk across operational, compliance, reputational, cybersecurity, " +
                "and financial dimensions with RAG ratings and mitigation actions.",
                List.of("supplier", "risk", "scoring"),
                List.of("risk_assessment", "Assess the risk for this supplier profile: ...")
        );
        return new AgentRegistration(card, riskAssessmentChatClient);
    }

    @Bean
    public AgentRegistration synthesisRegistration(ChatClient synthesisChatClient) {
        AgentCard card = buildCard(
                "synthesis",
                "Supplier Synthesis Agent",
                "Consolidates supplier profile and risk assessment into a final onboarding " +
                "recommendation (approve / approve-with-conditions / reject).",
                List.of("supplier", "synthesis", "recommendation"),
                List.of("synthesis", "Synthesize this profile and risk assessment into a recommendation: ...")
        );
        return new AgentRegistration(card, synthesisChatClient);
    }

    /**
     * Flat map of agentId → registration, used by {@link A2AAgentController}.
     */
    @Bean
    public Map<String, AgentRegistration> agentRegistrationMap(
            AgentRegistration infoFetchRegistration,
            AgentRegistration initialAssessmentRegistration,
            AgentRegistration riskAssessmentRegistration,
            AgentRegistration synthesisRegistration) {

        Map<String, AgentRegistration> map = new LinkedHashMap<>();
        map.put("info-fetch", infoFetchRegistration);
        map.put("initial-assessment", initialAssessmentRegistration);
        map.put("risk-assessment", riskAssessmentRegistration);
        map.put("synthesis", synthesisRegistration);
        return map;
    }

    // -------------------------------------------------------------------------

    private AgentCard buildCard(String agentId, String name, String description,
                                List<String> tags, List<String> examples) {
        String url = "http://localhost:" + serverPort + "/agents/" + agentId;
        return new AgentCard.Builder()
                .name(name)
                .description(description)
                .url(url)
                .version("1.0.0")
                .capabilities(new AgentCapabilities.Builder().streaming(false).build())
                .defaultInputModes(List.of("text"))
                .defaultOutputModes(List.of("text"))
                .skills(List.of(new AgentSkill.Builder()
                        .id(agentId)
                        .name(name)
                        .description(description)
                        .tags(tags)
                        .examples(examples)
                        .build()))
                .protocolVersion("0.3.0")
                .build();
    }
}
