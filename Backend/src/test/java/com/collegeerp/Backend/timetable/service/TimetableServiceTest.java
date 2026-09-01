package com.collegeerp.Backend.timetable.service;

import com.collegeerp.Backend.common.User;
import com.collegeerp.Backend.common.exception.DuplicateResourceException;
import com.collegeerp.Backend.schoolclass.entity.ClassSubject;
import com.collegeerp.Backend.schoolclass.entity.SchoolClass;
import com.collegeerp.Backend.schoolclass.repository.ClassSubjectRepository;
import com.collegeerp.Backend.security.UserPrincipal;
import com.collegeerp.Backend.timetable.dto.TimetableEntryRequest;
import com.collegeerp.Backend.timetable.repository.TimetableEntryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TimetableServiceTest {

    @Mock
    private TimetableEntryRepository timetableEntryRepository;
    @Mock
    private ClassSubjectRepository classSubjectRepository;

    @Test
    void rejectsTeacherCollisionAcrossClasses() {
        User teacher = User.builder().id(7L).firstName("T").lastName("One").build();
        SchoolClass schoolClass = SchoolClass.builder().id(11L).name("CSE-A").build();
        ClassSubject classSubject = ClassSubject.builder()
                .id(21L).schoolClass(schoolClass).teacher(teacher).subjectName("Java").build();

        TimetableEntryRequest request = TimetableEntryRequest.builder()
                .classSubjectId(21L)
                .dayOfWeek(DayOfWeek.MONDAY)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(10, 0))
                .build();

        when(classSubjectRepository.findByIdWithRelations(21L)).thenReturn(Optional.of(classSubject));
        when(timetableEntryRepository.hasClassConflict(
                11L, DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(10, 0), -1L))
                .thenReturn(false);
        when(timetableEntryRepository.hasTeacherConflict(
                7L, DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(10, 0), -1L))
                .thenReturn(true);

        TimetableService service = new TimetableService(timetableEntryRepository, classSubjectRepository);

        assertThrows(DuplicateResourceException.class,
                () -> service.create(request, new UserPrincipal(7L, "teacher@example.com", "TEACHER")));
        verify(timetableEntryRepository, never()).save(any());
    }

    @Test
    void teacherCannotScheduleAnotherTeachersClassSubject() {
        User owner = User.builder().id(8L).firstName("Owner").lastName("Teacher").build();
        SchoolClass schoolClass = SchoolClass.builder().id(12L).name("CSE-B").build();
        ClassSubject classSubject = ClassSubject.builder()
                .id(22L).schoolClass(schoolClass).teacher(owner).subjectName("DBMS").build();

        TimetableEntryRequest request = TimetableEntryRequest.builder()
                .classSubjectId(22L)
                .dayOfWeek(DayOfWeek.TUESDAY)
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(11, 0))
                .build();

        when(classSubjectRepository.findByIdWithRelations(22L)).thenReturn(Optional.of(classSubject));

        TimetableService service = new TimetableService(timetableEntryRepository, classSubjectRepository);

        assertThrows(AccessDeniedException.class,
                () -> service.create(request, new UserPrincipal(7L, "other@example.com", "TEACHER")));
        verify(timetableEntryRepository, never()).hasClassConflict(any(), any(), any(), any(), any());
        verify(timetableEntryRepository, never()).save(any());
    }
}
