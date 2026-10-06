package com.careerflow.service;

import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class SkillMatcher {
    public record Result(Integer percentage, List<String> matched, List<String> missing, int requiredCount) { }

    public Result match(String requirements, Collection<String> candidateSkills) {
        Map<String, String> required = new LinkedHashMap<>();
        if (requirements != null) {
            for (String skill : requirements.split("[,;\\r\\n]+")) {
                if (!skill.isBlank()) required.putIfAbsent(normalize(skill), skill.strip());
            }
        }
        Set<String> known = new HashSet<>();
        candidateSkills.stream().filter(Objects::nonNull).filter(s -> !s.isBlank()).forEach(s -> known.add(normalize(s)));
        List<String> matched = new ArrayList<>(), missing = new ArrayList<>();
        required.forEach((normalized, display) -> { if (known.contains(normalized)) matched.add(display); else missing.add(display); });
        Integer score = required.isEmpty() ? null : (int) Math.round(100.0 * matched.size() / required.size());
        return new Result(score, matched, missing, required.size());
    }
    private String normalize(String value) {
        return value.strip().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }
}
