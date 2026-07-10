package com.openclassrooms.etudiant.controller;

import com.openclassrooms.etudiant.dto.StudentDTO;
import com.openclassrooms.etudiant.dto.StudentPatchDTO;
import com.openclassrooms.etudiant.dto.StudentResponseDTO;
import com.openclassrooms.etudiant.entities.Student;
import com.openclassrooms.etudiant.mapper.StudentDtoMapper;
import com.openclassrooms.etudiant.service.StudentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
public class StudentController {
    private final StudentService studentService;
    private final StudentDtoMapper studentDtoMapper;

    @GetMapping
    public ResponseEntity<List<StudentResponseDTO>> findAll() {
        List<StudentResponseDTO> students = studentService.findAll().stream()
                .map(studentDtoMapper::toDto)
                .toList();
        return ResponseEntity.ok(students);
    }

    @GetMapping("/{id}")
    public ResponseEntity<StudentResponseDTO> findById(@PathVariable Long id) {
        Student student = studentService.findById(id);
        return ResponseEntity.ok(studentDtoMapper.toDto(student));
    }

    @PostMapping
    public ResponseEntity<StudentResponseDTO> create(@Valid @RequestBody StudentDTO studentDTO) {
        Student student = studentService.create(studentDtoMapper.toEntity(studentDTO));
        return ResponseEntity.ok(studentDtoMapper.toDto(student));
    }

    @PutMapping("/{id}")
    public ResponseEntity<StudentResponseDTO> update(@PathVariable Long id, @Valid @RequestBody StudentDTO studentDTO) {
        Student student = studentService.update(id, studentDtoMapper.toEntity(studentDTO));
        return ResponseEntity.ok(studentDtoMapper.toDto(student));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<StudentResponseDTO> patch(@PathVariable Long id, @RequestBody StudentPatchDTO studentPatchDTO) {
        Student student = studentService.findById(id);
        studentDtoMapper.patch(studentPatchDTO, student);
        Student updated = studentService.save(student);
        return ResponseEntity.ok(studentDtoMapper.toDto(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        studentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}