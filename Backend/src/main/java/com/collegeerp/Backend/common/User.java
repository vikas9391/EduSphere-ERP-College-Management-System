package com.collegeerp.Backend.common;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @Column(name = "is_active")
    private boolean isActive = true;

    @Column(name = "is_email_verified")
    private boolean isEmailVerified = false;

    /**
     * True whenever an admin sets this account's password for it (initial creation,
     * or a future "reset password" action) - forces the frontend to route the user to
     * a change-password screen before the dashboard. Flipped to false by
     * {@code UserService#changePassword} once the user picks their own password.
     */
    @Column(name = "must_change_password", nullable = false)
    @Builder.Default
    private boolean mustChangePassword = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // --- Teacher-only profile fields below. Null for every non-teacher role - kept
    // here rather than a separate table now that Teacher has been folded into User;
    // TeacherService is the only thing that ever sets these. ---

    @Column(name = "employee_id", unique = true)
    private String employeeId;

    private String phone;

    private String gender;

    private String qualification;

    private String specialization;

    private Integer experience;

    @Column(name = "joining_date")
    private java.time.LocalDate joiningDate;
}