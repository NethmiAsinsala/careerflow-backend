package com.careerflow.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "employers")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Employer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @OneToOne(mappedBy = "employer", fetch = FetchType.LAZY)
    private Company company;
}