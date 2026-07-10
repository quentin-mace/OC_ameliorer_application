package com.openclassrooms.etudiant.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class StudentDTO {
    @NotBlank
    private String firstName;
    @NotBlank
    private String lastName;
}
