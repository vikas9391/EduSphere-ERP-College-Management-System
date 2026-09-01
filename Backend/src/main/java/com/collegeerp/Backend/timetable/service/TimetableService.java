package com.collegeerp.Backend.timetable.service;

import com.collegeerp.Backend.common.exception.BadRequestException;
import com.collegeerp.Backend.common.exception.DuplicateResourceException;
import com.collegeerp.Backend.common.exception.ResourceNotFoundException;
import com.collegeerp.Backend.schoolclass.entity.ClassSubject;
import com.collegeerp.Backend.schoolclass.repository.ClassSubjectRepository;
import com.collegeerp.Backend.security.UserPrincipal;
import com.collegeerp.Backend.timetable.dto.TimetableEntryRequest;
import com.collegeerp.Backend.timetable.dto.TimetableEntryResponse;
import com.collegeerp.Backend.timetable.entity.TimetableEntry;
import com.collegeerp.Backend.timetable.repository.TimetableEntryRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@Transactional
public class TimetableService {

    private final TimetableEntryRepository timetableEntryRepository;
    private final ClassSubjectRepository classSubjectRepository;

    public TimetableService(
            TimetableEntryRepository timetableEntryRepository,
            ClassSubjectRepository classSubjectRepository) {
        this.timetableEntryRepository = timetableEntryRepository;
        this.classSubjectRepository = classSubjectRepository;
    }

    public TimetableEntryResponse create(TimetableEntryRequest request, UserPrincipal principal) {
        validateRequest(request);
        ClassSubject classSubject = requireClassSubject(request.getClassSubjectId());
        requireCanManage(classSubject, principal);
        validateConflicts(classSubject, request, null);

        LocalDateTime now = LocalDateTime.now();
        TimetableEntry entry = TimetableEntry.builder()
                .classSubject(classSubject)
                .dayOfWeek(request.getDayOfWeek())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .room(normalizeRoom(request.getRoom()))
                .createdAt(now)
                .updatedAt(now)
                .build();
        return map(timetableEntryRepository.save(entry));
    }

    public TimetableEntryResponse update(Long id, TimetableEntryRequest request, UserPrincipal principal) {
        validateRequest(request);
        TimetableEntry entry = requireEntry(id);
        requireCanManage(entry.getClassSubject(), principal);

        ClassSubject classSubject = requireClassSubject(request.getClassSubjectId());
        requireCanManage(classSubject, principal);
        validateConflicts(classSubject, request, id);

        entry.setClassSubject(classSubject);
        entry.setDayOfWeek(request.getDayOfWeek());
        entry.setStartTime(request.getStartTime());
        entry.setEndTime(request.getEndTime());
        entry.setRoom(normalizeRoom(request.getRoom()));
        entry.setUpdatedAt(LocalDateTime.now());
        return map(timetableEntryRepository.save(entry));
    }

    public void delete(Long id, UserPrincipal principal) {
        TimetableEntry entry = requireEntry(id);
        requireCanManage(entry.getClassSubject(), principal);
        timetableEntryRepository.delete(entry);
    }

    @Transactional(readOnly = true)
    public List<TimetableEntryResponse> getForClassSubject(Long classSubjectId, UserPrincipal principal) {
        ClassSubject classSubject = requireClassSubject(classSubjectId);
        requireCanView(classSubject, principal);
        return timetableEntryRepository.findAllByClassSubjectIdWithDetails(classSubjectId)
                .stream().map(this::map).toList();
    }

    @Transactional(readOnly = true)
    public List<TimetableEntryResponse> getMine(UserPrincipal principal) {
        if (!"TEACHER".equalsIgnoreCase(principal.getRole())) {
            throw new AccessDeniedException("Only teachers can view their teaching timetable");
        }
        return timetableEntryRepository.findAllForTeacher(principal.getId())
                .stream().map(this::map).toList();
    }

    private void validateRequest(TimetableEntryRequest request) {
        if (request.getClassSubjectId() == null || request.getDayOfWeek() == null
                || request.getStartTime() == null || request.getEndTime() == null) {
            throw new BadRequestException("Class subject, day, start time and end time are required");
        }
        if (!request.getEndTime().isAfter(request.getStartTime())) {
            throw new BadRequestException("End time must be after start time");
        }
    }

    private void validateConflicts(ClassSubject classSubject, TimetableEntryRequest request, Long currentId) {
        long excludeId = currentId == null ? -1L : currentId;
        Long schoolClassId = classSubject.getSchoolClass().getId();
        Long teacherId = classSubject.getTeacher().getId();

        if (timetableEntryRepository.hasClassConflict(
                schoolClassId, request.getDayOfWeek(), request.getStartTime(), request.getEndTime(), excludeId)) {
            throw new DuplicateResourceException("This class already has another subject during the selected time");
        }
        if (timetableEntryRepository.hasTeacherConflict(
                teacherId, request.getDayOfWeek(), request.getStartTime(), request.getEndTime(), excludeId)) {
            throw new DuplicateResourceException("The assigned teacher already has another class during the selected time");
        }
    }

    private ClassSubject requireClassSubject(Long id) {
        return classSubjectRepository.findByIdWithRelations(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Class subject", id));
    }

    private TimetableEntry requireEntry(Long id) {
        return timetableEntryRepository.findByIdWithDetails(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Timetable entry", id));
    }

    private void requireCanView(ClassSubject classSubject, UserPrincipal principal) {
        if (isAdmin(principal)) return;
        if (!"TEACHER".equalsIgnoreCase(principal.getRole())
                || classSubject.getTeacher() == null
                || !Objects.equals(classSubject.getTeacher().getId(), principal.getId())) {
            throw new AccessDeniedException("You can view timetable slots only for class subjects assigned to you");
        }
    }

    private void requireCanManage(ClassSubject classSubject, UserPrincipal principal) {
        requireCanView(classSubject, principal);
    }

    private boolean isAdmin(UserPrincipal principal) {
        return "ADMIN".equalsIgnoreCase(principal.getRole())
                || "SUPER_ADMIN".equalsIgnoreCase(principal.getRole());
    }

    private String normalizeRoom(String room) {
        return room == null || room.isBlank() ? null : room.trim();
    }

    public TimetableEntryResponse map(TimetableEntry entry) {
        ClassSubject cs = entry.getClassSubject();
        var schoolClass = cs.getSchoolClass();
        var teacher = cs.getTeacher();
        return TimetableEntryResponse.builder()
                .id(entry.getId())
                .classSubjectId(cs.getId())
                .schoolClassId(schoolClass.getId())
                .schoolClassName(schoolClass.getName())
                .academicYear(schoolClass.getAcademicYear())
                .semester(schoolClass.getSemester())
                .subjectId(cs.getSubject() != null ? cs.getSubject().getId() : cs.getId())
                .subjectCode(cs.getSubjectCode())
                .subjectName(cs.getSubjectName())
                .teacherId(teacher != null ? teacher.getId() : null)
                .teacherName(teacher != null
                        ? (teacher.getFirstName() + " " + teacher.getLastName()).trim() : null)
                .dayOfWeek(entry.getDayOfWeek())
                .startTime(entry.getStartTime())
                .endTime(entry.getEndTime())
                .room(entry.getRoom())
                .build();
    }
}
