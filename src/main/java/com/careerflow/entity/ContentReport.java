package com.careerflow.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
@Entity @Table(name="content_reports") @Getter @Setter
public class ContentReport {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(name="reporter_id",nullable=false) private Long reporterId;
 @Column(name="target_type",nullable=false,length=20) private String targetType;
 @Column(name="target_id",nullable=false) private Long targetId;
 @Column(nullable=false,length=2000) private String reason;
 @Column(nullable=false,length=20) private String status;
 @Column(length=2000) private String resolution;
 @Column(name="created_at",nullable=false) private LocalDateTime createdAt;
 @Column(name="updated_at",nullable=false) private LocalDateTime updatedAt;
}
