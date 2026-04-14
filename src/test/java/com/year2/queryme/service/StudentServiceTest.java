package com.year2.queryme.service;

import com.year2.queryme.model.Student;
import com.year2.queryme.model.User;
import com.year2.queryme.model.enums.UserTypes;
import com.year2.queryme.repository.ClassGroupRepository;
import com.year2.queryme.repository.CourseRepository;
import com.year2.queryme.repository.StudentRepository;
import com.year2.queryme.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private ClassGroupRepository classGroupRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private StudentService studentService;

    @Test
    void studentUpdateRejectsNameAndEmailChanges() {
        UUID userId = UUID.randomUUID();
        User user = User.builder().id(userId).email("student@example.com").role(UserTypes.STUDENT).build();
        Student student = Student.builder().id(1L).fullName("Student User").user(user).build();

        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(currentUserService.hasRole(UserTypes.STUDENT)).thenReturn(true);
        when(currentUserService.requireCurrentUserId()).thenReturn(userId);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> studentService.updateProfile(1L, Map.of(
                "fullName", "Changed Name",
                "email", "changed@example.com"
        )));

        assertEquals("Students can only change their password", exception.getMessage());
        verify(userRepository, never()).save(user);
    }
}
