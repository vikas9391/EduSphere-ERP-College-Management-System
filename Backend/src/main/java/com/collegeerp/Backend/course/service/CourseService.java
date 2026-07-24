package com.collegeerp.Backend.course.service;

import com.collegeerp.Backend.common.exception.DuplicateResourceException;
import com.collegeerp.Backend.common.exception.ResourceNotFoundException;
import com.collegeerp.Backend.course.dto.CourseRequest;
import com.collegeerp.Backend.course.dto.CourseResponse;
import com.collegeerp.Backend.course.entity.Course;
import com.collegeerp.Backend.course.repository.CourseRepository;
import com.collegeerp.Backend.department.entity.Department;
import com.collegeerp.Backend.department.repository.DepartmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional
public class CourseService {

    private static final Logger log = LoggerFactory.getLogger(CourseService.class);

    private final CourseRepository courseRepository;
    private final DepartmentRepository departmentRepository;

    public CourseService(CourseRepository courseRepository, DepartmentRepository departmentRepository) {
        this.courseRepository = courseRepository;
        this.departmentRepository = departmentRepository;
    }

    public CourseResponse createCourse(CourseRequest request) {

        if (courseRepository.existsByCourseCode(request.getCourseCode())) {
            throw new DuplicateResourceException("Course code '" + request.getCourseCode() + "' already exists");
        }

        Department department = findDepartmentOrThrow(request.getDepartmentId());

        Course course = Course.builder()
                .courseCode(request.getCourseCode())
                .courseName(request.getCourseName())
                .duration(request.getDuration())
                .description(request.getDescription())
                .department(department)
                .createdAt(LocalDateTime.now())
                .build();

        course = courseRepository.save(course);
        log.info("Created course id={} code={}", course.getId(), course.getCourseCode());

        return map(course);
    }

    @Transactional(readOnly = true)
    public Page<CourseResponse> getAllCourses(Pageable pageable) {
        return courseRepository.findAllWithDepartment(pageable).map(this::map);
    }

    @Transactional(readOnly = true)
    public CourseResponse getCourse(Long id) {
        return map(courseRepository.findByIdWithDepartment(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Course", id)));
    }

    public CourseResponse updateCourse(Long id, CourseRequest request) {

        Course course = findCourseOrThrow(id);

        if (!course.getCourseCode().equals(request.getCourseCode())
                && courseRepository.existsByCourseCode(request.getCourseCode())) {
            throw new DuplicateResourceException("Course code '" + request.getCourseCode() + "' already exists");
        }

        Department department = findDepartmentOrThrow(request.getDepartmentId());

        course.setCourseCode(request.getCourseCode());
        course.setCourseName(request.getCourseName());
        course.setDuration(request.getDuration());
        course.setDescription(request.getDescription());
        course.setDepartment(department);

        course = courseRepository.save(course);
        log.info("Updated course id={}", course.getId());

        return map(course);
    }

    public void deleteCourse(Long id) {
        Course course = findCourseOrThrow(id);
        // FK violations (e.g. Subjects/Exams still referencing this course) are translated
        // to a clean 409 by GlobalExceptionHandler#handleDataIntegrityViolation.
        courseRepository.delete(course);
        log.info("Deleted course id={}", id);
    }

    private Course findCourseOrThrow(Long id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Course", id));
    }

    private Department findDepartmentOrThrow(Long departmentId) {
        return departmentRepository.findById(departmentId)
                .orElseThrow(() -> ResourceNotFoundException.of("Department", departmentId));
    }

    private CourseResponse map(Course c) {
        return CourseResponse.builder()
                .id(c.getId())
                .courseCode(c.getCourseCode())
                .courseName(c.getCourseName())
                .duration(c.getDuration())
                .description(c.getDescription())
                .departmentId(c.getDepartment().getId())
                .departmentName(c.getDepartment().getName())
                .build();
    }
}
