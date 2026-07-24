package com.collegeerp.Backend.subject.service;

import com.collegeerp.Backend.common.exception.DuplicateResourceException;
import com.collegeerp.Backend.common.exception.ResourceNotFoundException;
import com.collegeerp.Backend.course.entity.Course;
import com.collegeerp.Backend.course.repository.CourseRepository;
import com.collegeerp.Backend.subject.dto.SubjectRequest;
import com.collegeerp.Backend.subject.dto.SubjectResponse;
import com.collegeerp.Backend.subject.entity.Subject;
import com.collegeerp.Backend.subject.repository.SubjectRepository;
import com.collegeerp.Backend.teacher.entity.Teacher;
import com.collegeerp.Backend.teacher.repository.TeacherRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional
public class SubjectService {

    private static final Logger log = LoggerFactory.getLogger(SubjectService.class);

    private final SubjectRepository subjectRepository;
    private final CourseRepository courseRepository;
    private final TeacherRepository teacherRepository;

    public SubjectService(SubjectRepository subjectRepository,
                           CourseRepository courseRepository,
                           TeacherRepository teacherRepository) {
        this.subjectRepository = subjectRepository;
        this.courseRepository = courseRepository;
        this.teacherRepository = teacherRepository;
    }

    public SubjectResponse createSubject(SubjectRequest request) {

        if (subjectRepository.existsBySubjectCode(request.getSubjectCode())) {
            throw new DuplicateResourceException("Subject code '" + request.getSubjectCode() + "' already exists");
        }

        Course course = findCourseOrThrow(request.getCourseId());
        Teacher teacher = findTeacherOrThrow(request.getTeacherId());

        Subject subject = Subject.builder()
                .subjectCode(request.getSubjectCode())
                .subjectName(request.getSubjectName())
                .credits(request.getCredits())
                .semester(request.getSemester())
                .course(course)
                .teacher(teacher)
                .createdAt(LocalDateTime.now())
                .build();

        subject = subjectRepository.save(subject);
        log.info("Created subject id={} code={}", subject.getId(), subject.getSubjectCode());

        return map(subject);
    }

    @Transactional(readOnly = true)
    public Page<SubjectResponse> getAllSubjects(Pageable pageable) {
        return subjectRepository.findAllWithRelations(pageable).map(this::map);
    }

    @Transactional(readOnly = true)
    public SubjectResponse getSubject(Long id) {
        return map(subjectRepository.findByIdWithRelations(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Subject", id)));
    }

    public SubjectResponse updateSubject(Long id, SubjectRequest request) {

        Subject subject = findSubjectOrThrow(id);

        if (!subject.getSubjectCode().equals(request.getSubjectCode())
                && subjectRepository.existsBySubjectCode(request.getSubjectCode())) {
            throw new DuplicateResourceException("Subject code '" + request.getSubjectCode() + "' already exists");
        }

        Course course = findCourseOrThrow(request.getCourseId());
        Teacher teacher = findTeacherOrThrow(request.getTeacherId());

        subject.setSubjectCode(request.getSubjectCode());
        subject.setSubjectName(request.getSubjectName());
        subject.setCredits(request.getCredits());
        subject.setSemester(request.getSemester());
        subject.setCourse(course);
        subject.setTeacher(teacher);

        subject = subjectRepository.save(subject);
        log.info("Updated subject id={}", subject.getId());

        return map(subject);
    }

    public void deleteSubject(Long id) {
        Subject subject = findSubjectOrThrow(id);
        // FK violations (e.g. Assignments/Enrollments/ExamSchedules still referencing this
        // subject) are translated to a clean 409 by GlobalExceptionHandler.
        subjectRepository.delete(subject);
        log.info("Deleted subject id={}", id);
    }

    private Subject findSubjectOrThrow(Long id) {
        return subjectRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Subject", id));
    }

    private Course findCourseOrThrow(Long courseId) {
        return courseRepository.findById(courseId)
                .orElseThrow(() -> ResourceNotFoundException.of("Course", courseId));
    }

    private Teacher findTeacherOrThrow(Long teacherId) {
        return teacherRepository.findById(teacherId)
                .orElseThrow(() -> ResourceNotFoundException.of("Teacher", teacherId));
    }

    private SubjectResponse map(Subject s) {
        return SubjectResponse.builder()
                .id(s.getId())
                .subjectCode(s.getSubjectCode())
                .subjectName(s.getSubjectName())
                .credits(s.getCredits())
                .semester(s.getSemester())
                .courseId(s.getCourse().getId())
                .courseName(s.getCourse().getCourseName())
                .teacherId(s.getTeacher().getId())
                .teacherName(s.getTeacher().getFirstName() + " " + s.getTeacher().getLastName())
                .build();
    }
}
