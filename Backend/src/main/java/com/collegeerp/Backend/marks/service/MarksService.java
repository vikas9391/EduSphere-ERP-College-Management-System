package com.collegeerp.Backend.marks.service;

import com.collegeerp.Backend.examination.entity.ExamSchedule;
import com.collegeerp.Backend.examination.repository.ExamScheduleRepository;
import com.collegeerp.Backend.marks.dto.MarksRequest;
import com.collegeerp.Backend.marks.dto.MarksResponse;
import com.collegeerp.Backend.marks.entity.Marks;
import com.collegeerp.Backend.marks.repository.MarksRepository;
import com.collegeerp.Backend.student.entity.Student;
import com.collegeerp.Backend.student.repository.StudentRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MarksService {

    private final MarksRepository marksRepository;
    private final ExamScheduleRepository examScheduleRepository;
    private final StudentRepository studentRepository;

    public MarksService(MarksRepository marksRepository,
                         ExamScheduleRepository examScheduleRepository,
                         StudentRepository studentRepository) {
        this.marksRepository = marksRepository;
        this.examScheduleRepository = examScheduleRepository;
        this.studentRepository = studentRepository;
    }

    public MarksResponse enterMarks(MarksRequest request) {

        ExamSchedule examSchedule = examScheduleRepository.findById(request.getExamScheduleId())
                .orElseThrow(() -> new RuntimeException("Exam schedule not found"));

        Student student = studentRepository.findById(request.getStudentId())
                .orElseThrow(() -> new RuntimeException("Student not found"));

        if (marksRepository.existsByExamScheduleIdAndStudentId(examSchedule.getId(), student.getId())) {
            throw new RuntimeException("Marks already entered for this student in this exam schedule");
        }

        validateMarks(request, examSchedule);

        int total = request.getInternalMarks() + request.getExternalMarks();
        double percentage = (total * 100.0) / examSchedule.getMaxMarks();
        String grade = GradeUtil.gradeFor(percentage);

        Marks marks = Marks.builder()
                .examSchedule(examSchedule)
                .student(student)
                .internalMarks(request.getInternalMarks())
                .externalMarks(request.getExternalMarks())
                .totalMarks(total)
                .grade(grade)
                .gradePoint(GradeUtil.gradePointFor(grade))
                .status("DRAFT")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        marks = marksRepository.save(marks);

        return map(marks);
    }

    public MarksResponse updateMarks(Long id, MarksRequest request) {

        Marks marks = marksRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Marks record not found"));

        if ("PUBLISHED".equals(marks.getStatus())) {
            throw new RuntimeException("Published marks cannot be edited");
        }

        validateMarks(request, marks.getExamSchedule());

        int total = request.getInternalMarks() + request.getExternalMarks();
        double percentage = (total * 100.0) / marks.getExamSchedule().getMaxMarks();
        String grade = GradeUtil.gradeFor(percentage);

        marks.setInternalMarks(request.getInternalMarks());
        marks.setExternalMarks(request.getExternalMarks());
        marks.setTotalMarks(total);
        marks.setGrade(grade);
        marks.setGradePoint(GradeUtil.gradePointFor(grade));
        marks.setUpdatedAt(LocalDateTime.now());

        marks = marksRepository.save(marks);

        return map(marks);
    }

    public List<MarksResponse> getMarksByExamSchedule(Long examScheduleId) {

        return marksRepository.findByExamScheduleIdWithDetails(examScheduleId)
                .stream()
                .map(this::map)
                .toList();
    }

    public MarksResponse getMarks(Long id) {

        return map(marksRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new RuntimeException("Marks record not found")));
    }

    public MarksResponse publishMarks(Long id) {

        Marks marks = marksRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Marks record not found"));

        marks.setStatus("PUBLISHED");
        marks.setUpdatedAt(LocalDateTime.now());

        marks = marksRepository.save(marks);

        return map(marks);
    }

    public List<MarksResponse> publishMarksForExamSchedule(Long examScheduleId) {

        List<Marks> marksList = marksRepository.findByExamScheduleIdWithDetails(examScheduleId);

        if (marksList.isEmpty()) {
            throw new RuntimeException("No marks found for this exam schedule");
        }

        marksList.forEach(m -> {
            m.setStatus("PUBLISHED");
            m.setUpdatedAt(LocalDateTime.now());
        });

        return marksRepository.saveAll(marksList)
                .stream()
                .map(this::map)
                .toList();
    }

    public void deleteMarks(Long id) {

        Marks marks = marksRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Marks record not found"));

        if ("PUBLISHED".equals(marks.getStatus())) {
            throw new RuntimeException("Published marks cannot be deleted");
        }

        marksRepository.deleteById(id);
    }

    private void validateMarks(MarksRequest request, ExamSchedule examSchedule) {

        if (request.getInternalMarks() < 0 || request.getExternalMarks() < 0) {
            throw new RuntimeException("Marks cannot be negative");
        }

        int total = request.getInternalMarks() + request.getExternalMarks();

        if (total > examSchedule.getMaxMarks()) {
            throw new RuntimeException("Total marks cannot exceed the maximum marks for this exam");
        }
    }

    private MarksResponse map(Marks m) {

        return MarksResponse.builder()
                .id(m.getId())
                .examScheduleId(m.getExamSchedule().getId())
                .examId(m.getExamSchedule().getExam().getId())
                .examName(m.getExamSchedule().getExam().getExamName())
                .subjectId(m.getExamSchedule().getSubject().getId())
                .subjectName(m.getExamSchedule().getSubject().getSubjectName())
                .studentId(m.getStudent().getId())
                .studentName(m.getStudent().getFirstName() + " " + m.getStudent().getLastName())
                .internalMarks(m.getInternalMarks())
                .externalMarks(m.getExternalMarks())
                .totalMarks(m.getTotalMarks())
                .maxMarks(m.getExamSchedule().getMaxMarks())
                .grade(m.getGrade())
                .gradePoint(m.getGradePoint())
                .status(m.getStatus())
                .build();
    }
}
