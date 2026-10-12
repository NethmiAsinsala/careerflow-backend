package com.careerflow.entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
@Entity @Table(name="certifications") @Getter @Setter
public class Certification {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(name="job_seeker_id",nullable=false) private Long jobSeekerId;
 @Column(nullable=false,length=200) private String name;
 @Column(nullable=false,length=200) private String issuer;
 @Column(name="issued_on") private LocalDate issuedOn;
 @Column(name="expires_on") private LocalDate expiresOn;
 @Column(name="credential_url",length=500) private String credentialUrl;
}
