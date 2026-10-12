package com.careerflow.service;
import com.careerflow.ai.*;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.concurrent.*;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
class ResumeAnalysisServiceTest {
 @Test void concurrentAnalysisIsRejectedAndCapacityIsReleased() throws Exception {
  var resumes=mock(ResumeService.class);var extractor=mock(ResumeTextExtractor.class);var ai=mock(OllamaResumeClient.class);
  var service=new ResumeAnalysisService(resumes,extractor,ai);
  byte[] data={1};when(resumes.downloadOwn()).thenReturn(data);
  when(extractor.extract(data)).thenReturn(new ResumeTextExtractor.ExtractedText("resume",1));
  var entered=new CountDownLatch(1);var release=new CountDownLatch(1);
  when(ai.analyze("resume")).thenAnswer(call->{entered.countDown();if(!release.await(5,TimeUnit.SECONDS))throw new IllegalStateException("Test timed out");return new ResumeFeedback("Summary",List.of(),List.of(),List.of());});
  var executor=Executors.newSingleThreadExecutor();
  try {
   var first=executor.submit(service::analyze);assertThat(entered.await(5,TimeUnit.SECONDS)).isTrue();
   assertThatThrownBy(service::analyze).isInstanceOfSatisfying(org.springframework.web.server.ResponseStatusException.class,e->assertThat(e.getStatusCode().value()).isEqualTo(429));
   release.countDown();assertThat(first.get(5,TimeUnit.SECONDS).feedback().summary()).isEqualTo("Summary");
   assertThat(service.analyze().feedback().summary()).isEqualTo("Summary");
  } finally {release.countDown();executor.shutdownNow();}
 }
}
