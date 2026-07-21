package com.quikko.service;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Produces the top-banner icebreaker text: a tailored prompt when two
 * strangers share an interest tag ("Common Ground"), otherwise a generic
 * random conversation-starter.
 */
@Service
public class IcebreakerService {

    private static final Map<String, String> TAG_PROMPTS = Map.ofEntries(
            Map.entry("music", "🎵 ask them their favorite album."),
            Map.entry("movies", "🎬 ask them their all-time favorite movie."),
            Map.entry("gaming", "🎮 ask them what they've been playing lately."),
            Map.entry("sports", "⚽ ask them which team they root for."),
            Map.entry("books", "📚 ask them what they're currently reading."),
            Map.entry("travel", "✈️ ask them their dream destination."),
            Map.entry("technology", "💻 ask them the last gadget they got excited about."),
            Map.entry("art", "🎨 ask them who their favorite artist is."),
            Map.entry("food", "🍜 ask them their favorite dish to cook."),
            Map.entry("anime", "📺 ask them what anime they'd recommend."),
            Map.entry("fitness", "💪 ask them their favorite way to work out."),
            Map.entry("science", "🔬 ask them the coolest fact they know."),
            Map.entry("photography", "📷 ask them what they love to shoot."),
            Map.entry("fashion", "👗 ask them how they'd describe their style."),
            Map.entry("nature", "🌿 ask them their favorite place outdoors."),
            Map.entry("comedy", "😂 ask them who makes them laugh the most."),
            Map.entry("cars", "🚗 ask them their dream car."),
            Map.entry("cooking", "🍳 ask them their go-to recipe."),
            Map.entry("pets", "🐾 ask them about their pet."),
            Map.entry("politics", "🗳️ ask them (politely!) what issue they care about most.")
    );

    private static final List<String> GENERIC_PROMPTS = List.of(
            "🌍 Ask them where they're chatting from tonight.",
            "🎲 If they could teleport anywhere right now, where would they go?",
            "☕ Coffee or tea? Settle the debate.",
            "🎧 Ask what song is stuck in their head right now.",
            "🍕 Pineapple on pizza: yes or no?",
            "🌙 Night owl or early bird?",
            "🎮 Ask what they'd do with an unexpected day off.",
            "📖 Ask for a book or show recommendation."
    );

    public String icebreakerFor(Set<String> sharedInterests) {
        if (sharedInterests != null && !sharedInterests.isEmpty()) {
            String tag = sharedInterests.iterator().next();
            String prompt = TAG_PROMPTS.get(tag.toLowerCase());
            if (prompt == null) {
                prompt = "🙌 ask them what they love most about it.";
            }
            return "You both like: " + tag + " " + prompt;
        }
        return GENERIC_PROMPTS.get(ThreadLocalRandom.current().nextInt(GENERIC_PROMPTS.size()));
    }
}
