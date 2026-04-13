package com.careerflow.service;

import com.careerflow.dto.request.SavedJobRequest;
import com.careerflow.dto.response.SavedJobResponse;

import java.util.List;

public interface SavedJobService {

    SavedJobResponse saveJob(SavedJobRequest request);

    List<SavedJobResponse> getSavedJobs(Long jobSeekerId);

    void removeSavedJob(Long jobId, Long jobSeekerId);
}