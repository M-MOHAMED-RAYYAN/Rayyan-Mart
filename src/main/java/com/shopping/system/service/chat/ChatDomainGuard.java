package com.shopping.system.service.chat;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Restricts assistant conversations to the shopping/product domain. Questions
 * that are clearly outside the RayyanMart domain are refused before they reach
 * any (potentially paid) AI provider.
 */
@Component
public class ChatDomainGuard {

    private static final List<Pattern> OFF_DOMAIN = List.of(
            Pattern.compile("\\b(write|generate|compose) (a |an |some )?(poem|song|lyrics|essay)\\b"),
            Pattern.compile("\\b(code|program(ming)?|software|bug|compile|refactor)\\b"),
            Pattern.compile("\\b(weather|forecast|temperature outside)\\b"),
            Pattern.compile("\\b(recipe|bake|cook|ingredients)\\b"),
            Pattern.compile("\\b(politics|election|government)\\b"),
            Pattern.compile("\\b(movie|film|celebrity|sports (team|match)|cricket score)\\b"),
            Pattern.compile("\\b(translate|means in|language learning)\\b"),
            Pattern.compile("\\b(solve|math|homework|answers for|exam)\\b"),
            Pattern.compile("\\b(hack|crack|stolen|steal|fraud)\\b"),
            Pattern.compile("\\b(my (bank|password|password is|ssn|social security))\\b"),
            Pattern.compile("\\b(gambl|bet on)\\b"),
            Pattern.compile("\\b(adult|dating|relationship advice)\\b"),
            Pattern.compile("\\b(medical advice|diagnose|cure for)\\b"),
            Pattern.compile("\\b(investment advice|stock (market|tip))\\b"),
            Pattern.compile("\\b(tell me a joke|joke about)\\b")
    );

    /**
     * @return {@code false} when the message is clearly outside the RayyanMart
     *         domain and must not be forwarded to an AI provider.
     */
    public boolean isInDomain(String message) {
        if (message == null || message.isBlank()) {
            return false;
        }
        String q = message.trim().toLowerCase(Locale.ROOT);
        for (Pattern pattern : OFF_DOMAIN) {
            if (pattern.matcher(q).find()) {
                return false;
            }
        }
        return true;
    }

    /** Helpful, in-domain message used when a question is refused. */
    public String refusal() {
        return "I can only answer questions about RayyanMart shopping \u2014 products, prices, categories, stock, "
                + "orders, delivery, payments and returns. Could you ask something about our store?";
    }
}