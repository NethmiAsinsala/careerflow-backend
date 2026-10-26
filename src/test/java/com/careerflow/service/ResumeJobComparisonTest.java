package com.careerflow.service;
import com.careerflow.ai.*;
import com.careerflow.entity.Job;
import com.careerflow.repository.JobRepository;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
class ResumeJobComparisonTest {
 @Test void scoresResumeSkillsAndPassesJobContext() {
  var resumes=mock(ResumeService.class); var extractor=mock(ResumeTextExtractor.class); var ai=mock(OllamaResumeClient.class); var jobs=mock(JobRepository.class);
  var service=new ResumeAnalysisService(resumes,extractor,ai,jobs,new SkillMatcher());
  var job=Job.builder().id(7L).title("Java developer").skills("Java, SQL, Angular").description("Build APIs").build();
  when(jobs.findById(7L)).thenReturn(Optional.of(job));
  byte[] data={1};when(resumes.downloadOwn()).thenReturn(data);
  when(extractor.extract(data)).thenReturn(new ResumeTextExtractor.ExtractedText("CV evidence",1));
  when(ai.analyze(eq("CV evidence"),contains("Build APIs"))).thenReturn(new ResumeFeedback("Review",List.of("java","SQL"),List.of(),List.of("Show an Angular project if you have one")));
  var result=service.compare(7L);
  assertThat(result.comparison().percentage()).isEqualTo(67);
  assertThat(result.comparison().missingSkills()).containsExactly("Angular");
  assertThat(result.comparison().jobId()).isEqualTo(7L);
  job.setSkills("");assertThat(service.compare(7L).comparison().percentage()).isNull();
 }
 @Test void missingJobDoesNotSendResumeToModel() {
  var ai=mock(OllamaResumeClient.class);var jobs=mock(JobRepository.class);
  var service=new ResumeAnalysisService(mock(ResumeService.class),mock(ResumeTextExtractor.class),ai,jobs,new SkillMatcher());
  assertThatThrownBy(()->service.compare(999L)).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
  verifyNoInteractions(ai);
 }
}
