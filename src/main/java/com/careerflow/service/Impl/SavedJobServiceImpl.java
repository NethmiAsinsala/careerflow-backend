package com.careerflow.service.impl;

import com.careerflow.dto.request.SavedJobRequest;
import com.careerflow.dto.response.SavedJobResponse;
import com.careerflow.entity.Job;
import com.careerflow.entity.JobSeeker;
import com.careerflow.entity.SavedJob;
import com.careerflow.mapper.SavedJobMapper;
import com.careerflow.repository.JobRepository;
import com.careerflow.repository.JobSeekerRepository;
import com.careerflow.repository.SavedJobRepository;
import com.careerflow.service.SavedJobService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SavedJobServiceImpl implements SavedJobService {

    private final SavedJobRepository savedJobRepository;
    private final JobRepository jobRepository;
    private final JobSeekerRepository jobSeekerRepository;
    private final SavedJobMapper savedJobMapper;

    @Override
    public SavedJobResponse saveJob(SavedJobRequest request) {

        if (savedJobRepository.existsByJobIdAndJobSeekerId(
                request.getJobId(),
                request.getJobSeekerId())) {

            throw new RuntimeException("Job is already saved");
        }

        Job job = jobRepository.findById(request.getJobId())
                .orElseThrow(() -> new RuntimeException("Job not found"));

        JobSeeker jobSeeker = jobSeekerRepository.findById(request.getJobSeekerId())
                .orElseThrow(() -> new RuntimeException("Job seeker not found"));

        SavedJob savedJob = SavedJob.builder()
                .job(job)
                .jobSeeker(jobSeeker)
                .build();

        return savedJobMapper.toResponse(
                savedJobRepository.save(savedJob)
        );
    }

    @Override
    public List<SavedJobResponse> getSavedJobs(Long jobSeekerId) {

        return savedJobRepository.findByJobSeekerId(jobSeekerId)
                .stream()
                .map(savedJobMapper::toResponse)
                .toList();
    }

    @Override
    public void removeSavedJob(Long jobId, Long jobSeekerId) {

        SavedJob savedJob = savedJobRepository
                .findByJobIdAndJobSeekerId(jobId, jobSeekerId)
                .orElseThrow(() -> new RuntimeException("Saved job not found"));

        savedJobRepository.delete(savedJob);
    }
}