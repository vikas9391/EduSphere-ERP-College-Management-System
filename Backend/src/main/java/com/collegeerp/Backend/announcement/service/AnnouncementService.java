package com.collegeerp.Backend.announcement.service;

import com.collegeerp.Backend.announcement.dto.*;
import com.collegeerp.Backend.announcement.entity.Announcement;
import com.collegeerp.Backend.announcement.entity.Announcement.AudienceType;
import com.collegeerp.Backend.announcement.entity.AnnouncementRecipient;
import com.collegeerp.Backend.announcement.entity.AnnouncementRecipient.RecipientType;
import com.collegeerp.Backend.announcement.repository.*;
import com.collegeerp.Backend.common.User;
import com.collegeerp.Backend.common.UserRepository;
import com.collegeerp.Backend.common.exception.BadRequestException;
import com.collegeerp.Backend.common.exception.ResourceNotFoundException;
import com.collegeerp.Backend.department.entity.Department;
import com.collegeerp.Backend.department.repository.DepartmentRepository;
import com.collegeerp.Backend.schoolclass.entity.ClassStudent;
import com.collegeerp.Backend.schoolclass.entity.SchoolClass;
import com.collegeerp.Backend.schoolclass.repository.ClassStudentRepository;
import com.collegeerp.Backend.schoolclass.repository.ClassSubjectRepository;
import com.collegeerp.Backend.schoolclass.repository.SchoolClassRepository;
import com.collegeerp.Backend.student.entity.Student;
import com.collegeerp.Backend.student.repository.StudentRepository;
import com.collegeerp.Backend.subject.entity.Subject;
import com.collegeerp.Backend.subject.repository.SubjectRepository;
import com.collegeerp.Backend.security.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AnnouncementService {
    private static final Logger log = LoggerFactory.getLogger(AnnouncementService.class);

    private final AnnouncementRepository announcementRepository;
    private final AnnouncementRecipientRepository recipientRepository;
    private final AnnouncementRecipientReadRepository recipientReadRepository;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final ClassStudentRepository classStudentRepository;
    private final ClassSubjectRepository classSubjectRepository;
    private final SubjectRepository subjectRepository;
    private final DepartmentRepository departmentRepository;

    public AnnouncementService(AnnouncementRepository announcementRepository, AnnouncementRecipientRepository recipientRepository,
                               AnnouncementRecipientReadRepository recipientReadRepository, UserRepository userRepository,
                               StudentRepository studentRepository, SchoolClassRepository schoolClassRepository,
                               ClassStudentRepository classStudentRepository, ClassSubjectRepository classSubjectRepository,
                               SubjectRepository subjectRepository, DepartmentRepository departmentRepository) {
        this.announcementRepository = announcementRepository; this.recipientRepository = recipientRepository;
        this.recipientReadRepository = recipientReadRepository;
        this.userRepository = userRepository; this.studentRepository = studentRepository;
        this.schoolClassRepository = schoolClassRepository; this.classStudentRepository = classStudentRepository;
        this.classSubjectRepository = classSubjectRepository; this.subjectRepository = subjectRepository;
        this.departmentRepository = departmentRepository;
    }

    @Transactional
    public AnnouncementResponse create(UserPrincipal principal, AnnouncementCreateRequest request) {
        String role = principal.getRole();
        boolean teacher = "TEACHER".equalsIgnoreCase(role);
        boolean staffCanCreate = "ADMIN".equalsIgnoreCase(role) || principal.getPermissions().contains("CREATE_ANNOUNCEMENT");
        if (!teacher && !staffCanCreate) throw new BadRequestException("You are not allowed to send announcements");
        if (teacher && !isStudentAudience(request.getAudienceType())) throw new BadRequestException("Teachers can send announcements only to students");
        if (requiresAudienceId(request.getAudienceType()) && request.getAudienceId() == null) throw new BadRequestException("A class or department must be selected");
        User sender = userRepository.findById(principal.getId()).orElseThrow(() -> ResourceNotFoundException.of("User", principal.getId()));
        validateAudienceAccess(principal, request);
        Set<Recipient> recipients = resolveRecipients(request.getAudienceType(), request.getAudienceId());
        log.info("Announcement recipient resolution: user={} role={} audienceType={} audienceId={} recipients={}",
                principal.getId(), role, request.getAudienceType(), request.getAudienceId(), recipients.size());
        if (recipients.isEmpty()) throw new BadRequestException("No recipients were found for the selected audience");
        Announcement announcement = Announcement.builder().sender(sender).title(request.getTitle().trim()).message(request.getMessage().trim())
                .audienceType(request.getAudienceType()).audienceId(request.getAudienceId()).createdAt(LocalDateTime.now()).build();
        announcementRepository.save(announcement);
        recipientRepository.saveAll(recipients.stream().map(r -> AnnouncementRecipient.builder().announcement(announcement).recipientType(r.type()).recipientId(r.id()).build()).toList());
        log.info("Announcement persisted: id={} recipients={} audienceType={} audienceId={}",
                announcement.getId(), recipients.size(), request.getAudienceType(), request.getAudienceId());
        return toResponse(announcement, false);
    }

    @Transactional(readOnly = true)
    public List<AnnouncementResponse> received(UserPrincipal principal) {
        Recipient recipient = currentRecipient(principal);
        log.info("Loading received announcements: user={} role={} recipientType={} recipientId={}",
                principal.getId(), principal.getRole(), recipient.type(), recipient.id());
        List<AnnouncementResponse> result = recipientReadRepository.findReceived(recipient.type(), recipient.id()).stream()
                .map(r -> toResponse(r.getAnnouncement(), r.getReadAt() != null)).toList();
        log.info("Loaded {} received announcement(s): user={} role={} recipientType={} recipientId={}",
                result.size(), principal.getId(), principal.getRole(), recipient.type(), recipient.id());
        return result;
    }

    @Transactional
    public void markRead(UserPrincipal principal, Long announcementId) {
        Recipient recipient = currentRecipient(principal);
        int updated = recipientRepository.markRead(announcementId, recipient.type(), recipient.id(), LocalDateTime.now());
        if (updated == 0) throw new ResourceNotFoundException("Announcement not found for this user");
    }

    @Transactional(readOnly = true)
    public long unreadCount(UserPrincipal principal) {
        Recipient recipient = currentRecipient(principal);
        return recipientRepository.countUnread(recipient.type(), recipient.id());
    }

    @Transactional
    public void markAllRead(UserPrincipal principal) {
        Recipient recipient = currentRecipient(principal);
        recipientRepository.markAllRead(recipient.type(), recipient.id(), LocalDateTime.now());
    }

    private Recipient currentRecipient(UserPrincipal principal) {
        String role = principal.getRole() == null ? "" : principal.getRole().trim();
        if ("STUDENT".equalsIgnoreCase(role) || "STUDENTS".equalsIgnoreCase(role)) {
            Student student = studentRepository.findByEmail(principal.getEmail())
                    .orElseThrow(() -> new ResourceNotFoundException("Student profile not found for authenticated user"));
            log.info("Resolved student recipient: authUserId={} email={} role={} studentId={}",
                    principal.getId(), principal.getEmail(), principal.getRole(), student.getId());
            return new Recipient(RecipientType.STUDENT, student.getId());
        }
        return new Recipient(RecipientType.USER, principal.getId());
    }

    @Transactional(readOnly = true)
    public List<AnnouncementAudienceOption> audienceOptions(UserPrincipal principal) {
        List<AnnouncementAudienceOption> options = new ArrayList<>();
        boolean teacher = "TEACHER".equalsIgnoreCase(principal.getRole());
        boolean canCreate = "ADMIN".equalsIgnoreCase(principal.getRole()) || principal.getPermissions().contains("CREATE_ANNOUNCEMENT");
        if (!teacher && !canCreate) return options;
        if (!teacher) {
            options.add(option(AudienceType.ALL_STUDENTS, null, "All students"));
            options.add(option(AudienceType.ALL_TEACHERS, null, "All teachers"));
        }
        List<SchoolClass> classes = "ADMIN".equalsIgnoreCase(principal.getRole()) ? schoolClassRepository.findAllWithTeacher() : schoolClassRepository.findAllByTeacherId(principal.getId());
        classes.forEach(c -> {
            options.add(option(AudienceType.CLASS_STUDENTS, c.getId(), "Students · " + c.getName()));
            if (!teacher) options.add(option(AudienceType.CLASS_TEACHERS, c.getId(), "Teachers · " + c.getName()));
        });
        Set<Long> departmentIds = new LinkedHashSet<>();
        if ("ADMIN".equalsIgnoreCase(principal.getRole())) departmentIds.addAll(departmentRepository.findAll().stream().map(Department::getId).toList());
        else departmentIds.addAll(subjectRepository.findByTeacherIdWithRelations(principal.getId()).stream().map(s -> s.getCourse().getDepartment()).filter(Objects::nonNull).map(Department::getId).toList());
        for (Long departmentId : departmentIds) {
            Department d = departmentRepository.findById(departmentId).orElse(null);
            if (d != null) {
                options.add(option(AudienceType.DEPARTMENT_STUDENTS, d.getId(), "Students · " + d.getName()));
                if (!teacher) options.add(option(AudienceType.DEPARTMENT_TEACHERS, d.getId(), "Teachers · " + d.getName()));
            }
        }
        return options;
    }

    @Transactional(readOnly = true)
    public List<AnnouncementContact> contacts(UserPrincipal principal) {
        Set<Long> teacherIds = new LinkedHashSet<>();
        if ("ADMIN".equalsIgnoreCase(principal.getRole())) {
            teacherIds.addAll(userRepository.findAll().stream().filter(u -> u.getRole() != null && "TEACHER".equalsIgnoreCase(u.getRole().getName())).map(User::getId).toList());
        } else if ("STUDENT".equalsIgnoreCase(principal.getRole()) || "STUDENTS".equalsIgnoreCase(principal.getRole())) {
            Long studentId = currentRecipient(principal).id();
            classStudentRepository.findAllByStudentId(studentId).forEach(cs -> { teacherIds.add(cs.getSchoolClass().getTeacher().getId()); classSubjectRepository.findAllByClassId(cs.getSchoolClass().getId()).forEach(s -> teacherIds.add(s.getTeacher().getId())); });
            Student student = studentRepository.findByIdWithCourse(studentId).orElse(null);
            if (student != null && student.getCourse() != null && student.getCourse().getDepartment() != null) {
                Long dept = student.getCourse().getDepartment().getId();
                subjectRepository.findAll().stream().filter(s -> s.getCourse() != null && s.getCourse().getDepartment() != null && Objects.equals(s.getCourse().getDepartment().getId(), dept)).forEach(s -> teacherIds.add(s.getTeacher().getId()));
            }
        } else if ("TEACHER".equalsIgnoreCase(principal.getRole())) {
            schoolClassRepository.findAllByTeacherId(principal.getId()).forEach(c -> { teacherIds.add(c.getTeacher().getId()); classSubjectRepository.findAllByClassId(c.getId()).forEach(s -> teacherIds.add(s.getTeacher().getId())); });
            subjectRepository.findByTeacherIdWithRelations(principal.getId()).forEach(s -> { if (s.getCourse() != null && s.getCourse().getDepartment() != null) { Long dept = s.getCourse().getDepartment().getId(); subjectRepository.findAll().stream().filter(other -> other.getCourse() != null && other.getCourse().getDepartment() != null && Objects.equals(other.getCourse().getDepartment().getId(), dept)).forEach(other -> teacherIds.add(other.getTeacher().getId())); } });
        }
        return userRepository.findAll().stream().filter(u -> teacherIds.contains(u.getId())).map(u -> AnnouncementContact.builder().id(u.getId()).name((u.getFirstName() + " " + u.getLastName()).trim()).email(u.getEmail()).phone(u.getPhone()).role(u.getRole() == null ? "" : u.getRole().getName()).build()).sorted(Comparator.comparing(AnnouncementContact::getName, String.CASE_INSENSITIVE_ORDER)).toList();
    }

    private Set<Recipient> resolveRecipients(AudienceType type, Long id) {
        return switch (type) {
            case ALL_STUDENTS -> studentRepository.findAll().stream().map(s -> new Recipient(RecipientType.STUDENT, s.getId())).collect(Collectors.toCollection(LinkedHashSet::new));
            case ALL_TEACHERS -> userRepository.findAll().stream().filter(u -> u.getRole() != null && "TEACHER".equalsIgnoreCase(u.getRole().getName())).map(u -> new Recipient(RecipientType.USER, u.getId())).collect(Collectors.toCollection(LinkedHashSet::new));
            case CLASS_STUDENTS -> classStudentRepository.findAllByClassId(requireClass(id).getId()).stream().map(ClassStudent::getStudent).map(s -> new Recipient(RecipientType.STUDENT, s.getId())).collect(Collectors.toCollection(LinkedHashSet::new));
            case CLASS_TEACHERS -> { SchoolClass c = requireClass(id); Set<Recipient> result = new LinkedHashSet<>(); result.add(new Recipient(RecipientType.USER, c.getTeacher().getId())); classSubjectRepository.findAllByClassId(id).forEach(cs -> result.add(new Recipient(RecipientType.USER, cs.getTeacher().getId()))); yield result; }
            case DEPARTMENT_STUDENTS -> studentRepository.findAll().stream().filter(s -> s.getCourse() != null && s.getCourse().getDepartment() != null && Objects.equals(s.getCourse().getDepartment().getId(), id)).map(s -> new Recipient(RecipientType.STUDENT, s.getId())).collect(Collectors.toCollection(LinkedHashSet::new));
            case DEPARTMENT_TEACHERS -> subjectRepository.findAll().stream().filter(s -> s.getCourse() != null && s.getCourse().getDepartment() != null && Objects.equals(s.getCourse().getDepartment().getId(), id)).map(Subject::getTeacher).filter(Objects::nonNull).map(u -> new Recipient(RecipientType.USER, u.getId())).collect(Collectors.toCollection(LinkedHashSet::new));
        };
    }

    private void validateAudienceAccess(UserPrincipal principal, AnnouncementCreateRequest request) {
        if (!"TEACHER".equalsIgnoreCase(principal.getRole())) return;
        if (request.getAudienceType() == AudienceType.CLASS_STUDENTS && schoolClassRepository.findAllByTeacherId(principal.getId()).stream().noneMatch(c -> Objects.equals(c.getId(), request.getAudienceId()))) throw new BadRequestException("Teachers can only announce to their own classes");
        if (request.getAudienceType() == AudienceType.DEPARTMENT_STUDENTS && subjectRepository.findByTeacherIdWithRelations(principal.getId()).stream().noneMatch(s -> s.getCourse() != null && s.getCourse().getDepartment() != null && Objects.equals(s.getCourse().getDepartment().getId(), request.getAudienceId()))) throw new BadRequestException("Teachers can only announce to their own departments");
    }
    private boolean isStudentAudience(AudienceType type) { return type == AudienceType.ALL_STUDENTS || type == AudienceType.CLASS_STUDENTS || type == AudienceType.DEPARTMENT_STUDENTS; }
    private boolean requiresAudienceId(AudienceType type) { return type == AudienceType.CLASS_STUDENTS || type == AudienceType.CLASS_TEACHERS || type == AudienceType.DEPARTMENT_STUDENTS || type == AudienceType.DEPARTMENT_TEACHERS; }
    private SchoolClass requireClass(Long id) { return schoolClassRepository.findByIdWithTeacher(id).orElseThrow(() -> ResourceNotFoundException.of("Class", id)); }
    private AnnouncementAudienceOption option(AudienceType type, Long id, String label) { return AnnouncementAudienceOption.builder().type(type).id(id).label(label).build(); }
    private AnnouncementResponse toResponse(Announcement a, boolean read) { return AnnouncementResponse.builder().id(a.getId()).title(a.getTitle()).message(a.getMessage()).audienceType(a.getAudienceType()).audienceId(a.getAudienceId()).senderName((a.getSender().getFirstName() + " " + a.getSender().getLastName()).trim()).createdAt(a.getCreatedAt()).read(read).build(); }
    private record Recipient(RecipientType type, Long id) {}
}
