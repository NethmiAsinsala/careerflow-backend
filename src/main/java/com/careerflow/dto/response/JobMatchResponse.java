package com.careerflow.dto.response;

import java.util.List;

public record JobMatchResponse(Long jobId, Long jobSeekerId, Integer matchPercentage,
        List<String> matchedSkills, List<String> missingSkills, int requiredSkillCount,
        String note) {
}
