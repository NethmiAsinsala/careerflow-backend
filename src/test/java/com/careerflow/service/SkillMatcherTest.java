package com.careerflow.service;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
class SkillMatcherTest {
    final SkillMatcher matcher=new SkillMatcher();
    @Test void normalizesDeduplicatesAndSplits() {
        var result=matcher.match("Java; spring boot\r\nSQL, JAVA",List.of(" JAVA ","Spring   Boot"));
        assertThat(result.percentage()).isEqualTo(67);assertThat(result.requiredCount()).isEqualTo(3);
        assertThat(result.missing()).containsExactly("SQL");
    }
    @Test void preservesPunctuationAndAvoidsSubstringMatches() {
        var result=matcher.match("C++, C#, .NET, Java",List.of("C","JavaScript",".net"));
        assertThat(result.percentage()).isEqualTo(25);assertThat(result.matched()).containsExactly(".NET");
    }
    @Test void handlesMissingRequirementsAndSkills() {
        assertThat(matcher.match(null,List.of()).percentage()).isNull();
        assertThat(matcher.match(" , ; ",List.of()).requiredCount()).isZero();
        assertThat(matcher.match("Java",List.of()).percentage()).isZero();
    }
}
