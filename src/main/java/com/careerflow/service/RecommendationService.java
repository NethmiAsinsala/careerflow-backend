package com.careerflow.service;
import com.careerflow.dto.response.JobResponse;
import com.careerflow.entity.Job;
import com.careerflow.mapper.JobMapper;
import com.careerflow.repository.*;
import com.careerflow.security.AuthenticatedUser;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import java.time.*;
import java.util.*;
@Service @RequiredArgsConstructor @Transactional(readOnly=true)
@PreAuthorize("hasRole('JOB_SEEKER')")
public class RecommendationService {
 private final EntityManager entityManager;
 private final SkillRepository skills;
 private final JobSeekerRepository seekers;
 private final SkillMatcher matcher;
 private final JobMapper mapper;
 private final Clock clock;
 public record RecommendedJob(JobResponse job,int matchPercentage,List<String> matchedSkills,List<String> missingSkills) { }
 public record Recommendations(List<RecommendedJob> jobs,long consideredJobs,String note) { }
 public Recommendations recommend(int limit){
  Long userId=((AuthenticatedUser)SecurityContextHolder.getContext().getAuthentication().getPrincipal()).id();
  var seeker=seekers.findByUserId(userId).orElseThrow();
  var names=skills.findByJobSeekerIdOrderByIdAsc(seeker.getId()).stream().map(s->s.getName()).toList();
  if(names.isEmpty())return new Recommendations(List.of(),0,"Add profile skills to receive recommendations");
  var order=Comparator.comparingInt(RecommendedJob::matchPercentage).thenComparing(r->r.job().getId());
  var best=new PriorityQueue<RecommendedJob>(order);
  long cursor=0,considered=0;var now=LocalDateTime.now(clock);
  while(true){
   var batch=entityManager.createQuery("select j from Job j where j.id > :cursor and j.status = 'OPEN' "
     +"and j.employer.user.active = true and (j.applicationDeadline is null or j.applicationDeadline > :now) "
     +"and not exists(select a.id from JobApplication a where a.job = j and a.jobSeeker.id = :seeker) order by j.id",Job.class)
     .setParameter("cursor",cursor).setParameter("now",now).setParameter("seeker",seeker.getId()).setMaxResults(200).getResultList();
   if(batch.isEmpty())break;
   for(var job:batch){
    cursor=job.getId();considered++;var result=matcher.match(job.getSkills(),names);
    if(result.percentage()!=null && result.percentage()>0){
     best.add(new RecommendedJob(mapper.toResponse(job),result.percentage(),result.matched(),result.missing()));
     if(best.size()>limit)best.poll();
    }
   }
  }
  return new Recommendations(best.stream().sorted(order.reversed()).toList(),considered,
    "Ranked by profile skill coverage, then newest job ID; excludes applied, closed, expired and inactive-employer jobs. Not a hiring prediction.");
 }
}
