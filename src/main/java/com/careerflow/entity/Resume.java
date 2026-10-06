package com.careerflow.entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
@Entity @Table(name="resumes") @Getter @Setter
public class Resume {
 @Id @Column(name="job_seeker_id") private Long jobSeekerId;
 @Lob @Column(nullable=false,columnDefinition="MEDIUMBLOB") private byte[] content;
 @Column(name="updated_at",nullable=false) private LocalDateTime updatedAt;
}
