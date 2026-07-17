package com.openclassrooms.etudiant.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openclassrooms.etudiant.dto.StudentDTO;
import com.openclassrooms.etudiant.dto.StudentPatchDTO;
import com.openclassrooms.etudiant.entities.Student;
import com.openclassrooms.etudiant.entities.User;
import com.openclassrooms.etudiant.repository.StudentRepository;
import com.openclassrooms.etudiant.repository.UserRepository;
import com.openclassrooms.etudiant.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

// Tests d'intégration du CRUD /api/students (authentification requise via token JWT)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
public class StudentControllerTest {

    private static final String URL = "/api/students";
    private static final String LOGIN = "login";
    private static final String PASSWORD = "password";

    @Container
    static MySQLContainer mySQLContainer = new MySQLContainer("mysql:8.0");

    @Autowired
    private StudentRepository studentRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserService userService;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private MockMvc mockMvc;

    private String token;

    @DynamicPropertySource
    static void configureTestProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> mySQLContainer.getJdbcUrl());
        registry.add("spring.datasource.username", () -> mySQLContainer.getUsername());
        registry.add("spring.datasource.password", () -> mySQLContainer.getPassword());
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create");
    }

    // Crée un utilisateur et récupère un token avant chaque test
    @BeforeEach
    public void beforeEach() {
        User user = new User();
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setLogin(LOGIN);
        user.setPassword(PASSWORD);
        userService.register(user);
        token = userService.login(LOGIN, PASSWORD);
    }

    @AfterEach
    public void afterEach() {
        studentRepository.deleteAll();
        userRepository.deleteAll();
    }

    // Appel sans token -> 401 Unauthorized
    @Test
    public void findAllWithoutTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get(URL))
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isUnauthorized());
    }

    // Aucun étudiant en base -> liste vide
    @Test
    public void findAllReturnsEmptyList() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get(URL)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$").isArray())
                .andExpect(MockMvcResultMatchers.jsonPath("$").isEmpty());
    }

    // Deux étudiants en base -> liste de taille 2
    @Test
    public void findAllReturnsStudentsList() throws Exception {
        Student student1 = new Student();
        student1.setFirstName("Alice");
        student1.setLastName("Martin");
        Student student2 = new Student();
        student2.setFirstName("Bob");
        student2.setLastName("Durand");
        studentRepository.save(student1);
        studentRepository.save(student2);

        mockMvc.perform(MockMvcRequestBuilders.get(URL)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.length()").value(2));
    }

    // Etudiant existant -> retourne ses détails
    @Test
    public void findByIdExistingReturnsStudent() throws Exception {
        Student student = new Student();
        student.setFirstName("Alice");
        student.setLastName("Martin");
        Student saved = studentRepository.save(student);

        mockMvc.perform(MockMvcRequestBuilders.get(URL + "/" + saved.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(saved.getId()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.firstName").value("Alice"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.lastName").value("Martin"));
    }

    // Id inexistant -> 404 Not Found
    @Test
    public void findByIdNotFoundReturnsNotFound() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get(URL + "/99999")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    // Création avec des données valides -> étudiant créé avec id et dates
    @Test
    public void createValidReturnsCreatedStudent() throws Exception {
        StudentDTO studentDTO = new StudentDTO();
        studentDTO.setFirstName("John");
        studentDTO.setLastName("Doe");

        mockMvc.perform(MockMvcRequestBuilders.post(URL)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .content(objectMapper.writeValueAsString(studentDTO))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("$.firstName").value("John"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.lastName").value("Doe"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.createdAt").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("$.updatedAt").isNotEmpty());
    }

    // Mise à jour d'un étudiant existant -> nouvelles valeurs retournées
    @Test
    public void updateExistingReturnsUpdatedStudent() throws Exception {
        Student student = new Student();
        student.setFirstName("John");
        student.setLastName("Doe");
        Student saved = studentRepository.save(student);

        StudentDTO studentDTO = new StudentDTO();
        studentDTO.setFirstName("Jane");
        studentDTO.setLastName("Doe");

        mockMvc.perform(MockMvcRequestBuilders.put(URL + "/" + saved.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .content(objectMapper.writeValueAsString(studentDTO))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(saved.getId()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.firstName").value("Jane"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.lastName").value("Doe"));
    }

    // Mise à jour d'un id inexistant -> 404 Not Found
    @Test
    public void updateNotFoundReturnsNotFound() throws Exception {
        StudentDTO studentDTO = new StudentDTO();
        studentDTO.setFirstName("Jane");
        studentDTO.setLastName("Doe");

        mockMvc.perform(MockMvcRequestBuilders.put(URL + "/99999")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .content(objectMapper.writeValueAsString(studentDTO))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    // Patch partiel -> seul le champ fourni change, le reste est conservé
    @Test
    public void patchPartialUpdatesOnlyGivenField() throws Exception {
        Student student = new Student();
        student.setFirstName("John");
        student.setLastName("Doe");
        Student saved = studentRepository.save(student);

        StudentPatchDTO patchDTO = new StudentPatchDTO();
        patchDTO.setFirstName("OnlyFirst");

        mockMvc.perform(MockMvcRequestBuilders.patch(URL + "/" + saved.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .content(objectMapper.writeValueAsString(patchDTO))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.firstName").value("OnlyFirst"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.lastName").value("Doe"));
    }

    // Patch d'un id inexistant -> 404 Not Found
    @Test
    public void patchNotFoundReturnsNotFound() throws Exception {
        StudentPatchDTO patchDTO = new StudentPatchDTO();

        mockMvc.perform(MockMvcRequestBuilders.patch(URL + "/99999")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .content(objectMapper.writeValueAsString(patchDTO))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    // Suppression d'un étudiant existant -> 204 No Content
    @Test
    public void deleteExistingReturnsNoContent() throws Exception {
        Student student = new Student();
        student.setFirstName("John");
        student.setLastName("Doe");
        Student saved = studentRepository.save(student);

        mockMvc.perform(MockMvcRequestBuilders.delete(URL + "/" + saved.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isNoContent());
    }

    // Suppression d'un id inexistant -> 404 Not Found
    @Test
    public void deleteNotFoundReturnsNotFound() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete(URL + "/99999")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andDo(print())
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }
}