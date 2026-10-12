package com.careerflow.ai;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import static org.assertj.core.api.Assertions.*;
@EnabledIfEnvironmentVariable(named="CAREERFLOW_AI_LIVE_TESTS",matches="true")
class OllamaLiveTest {
 @Test void localModelAnalyzesSyntheticResume() {
  try(var factory=Validation.buildDefaultValidatorFactory()) {
   var client=new OllamaResumeClient(new ObjectMapper(),factory.getValidator(),"http://127.0.0.1:11434","qwen2.5:1.5b",120);
   var result=client.analyze("Example Candidate. Junior Java developer. Skills: Java, Spring Boot, SQL. Education: Diploma in software engineering. Project: built a job application API with Spring Boot and MySQL, with 150 passing tests.");
   assertThat(result.summary()).isNotBlank();assertThat(result.skills()).isNotEmpty();
  }
 }
}
