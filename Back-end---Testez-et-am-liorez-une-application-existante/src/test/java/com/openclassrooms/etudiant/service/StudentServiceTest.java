package com.openclassrooms.etudiant.service;

import com.openclassrooms.etudiant.entities.Student;
import com.openclassrooms.etudiant.repository.StudentRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Tests unitaires de StudentService (dépendances mockées, pas de base de données)
@ExtendWith(SpringExtension.class)
public class StudentServiceTest {
    private static final String FIRST_NAME = "John";
    private static final String LAST_NAME = "Doe";

    @Mock
    private StudentRepository studentRepository;
    @InjectMocks
    private StudentService studentService;

    // Id inexistant -> exception 404 avec message explicite
    @Test
    public void test_findById_not_found_throws_ResponseStatusException() {
        // GIVEN
        when(studentRepository.findById(99L)).thenReturn(Optional.empty());

        // THEN
        ResponseStatusException exception = Assertions.assertThrows(ResponseStatusException.class,
                () -> studentService.findById(99L));
        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(exception.getReason()).isEqualTo("Student not found with id: 99");
    }

    // Création avec un étudiant null -> exception
    @Test
    public void test_create_null_student_throws_IllegalArgumentException() {
        // THEN
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> studentService.create(null));
    }

    // Mise à jour d'un id inexistant -> exception 404, aucune sauvegarde effectuée
    @Test
    public void test_update_not_found_throws_ResponseStatusException() {
        // GIVEN
        Student studentDetails = new Student();
        studentDetails.setFirstName("New");
        studentDetails.setLastName("Name");
        when(studentRepository.findById(99L)).thenReturn(Optional.empty());

        // THEN
        ResponseStatusException exception = Assertions.assertThrows(ResponseStatusException.class,
                () -> studentService.update(99L, studentDetails));
        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        verify(studentRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    // Mise à jour d'un étudiant existant -> les champs sont copiés et sauvegardés
    @Test
    public void test_update_successful_copies_fields_and_saves() {
        // GIVEN
        Student existingStudent = new Student();
        existingStudent.setId(1L);
        existingStudent.setFirstName(FIRST_NAME);
        existingStudent.setLastName(LAST_NAME);
        Student studentDetails = new Student();
        studentDetails.setFirstName("New");
        studentDetails.setLastName("Name");
        when(studentRepository.findById(1L)).thenReturn(Optional.of(existingStudent));
        when(studentRepository.save(existingStudent)).thenReturn(existingStudent);

        // WHEN
        Student result = studentService.update(1L, studentDetails);

        // THEN
        ArgumentCaptor<Student> studentCaptor = ArgumentCaptor.forClass(Student.class);
        verify(studentRepository).save(studentCaptor.capture());
        assertThat(studentCaptor.getValue().getFirstName()).isEqualTo("New");
        assertThat(studentCaptor.getValue().getLastName()).isEqualTo("Name");
        assertThat(result.getFirstName()).isEqualTo("New");
        assertThat(result.getLastName()).isEqualTo("Name");
    }

    // Suppression d'un étudiant existant -> l'entité est bien supprimée
    @Test
    public void test_delete_found_deletes_entity() {
        // GIVEN
        Student student = new Student();
        student.setId(1L);
        student.setFirstName(FIRST_NAME);
        student.setLastName(LAST_NAME);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));

        // WHEN
        studentService.delete(1L);

        // THEN
        verify(studentRepository).delete(student);
    }

    // Suppression d'un id inexistant -> exception 404, aucune suppression effectuée
    @Test
    public void test_delete_not_found_throws_ResponseStatusException() {
        // GIVEN
        when(studentRepository.findById(99L)).thenReturn(Optional.empty());

        // THEN
        ResponseStatusException exception = Assertions.assertThrows(ResponseStatusException.class,
                () -> studentService.delete(99L));
        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        verify(studentRepository, never()).delete(org.mockito.ArgumentMatchers.any());
    }
}
