package com.openclassrooms.etudiant.service;

import com.openclassrooms.etudiant.entities.User;
import com.openclassrooms.etudiant.repository.UserRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Tests unitaires de UserService (dépendances mockées, pas de base de données)
@ExtendWith(SpringExtension.class)
public class UserServiceTest {
    private static final String FIRST_NAME = "John";
    private static final String LAST_NAME = "Doe";
    private static final String LOGIN = "LOGIN";
    private static final String PASSWORD = "PASSWORD";
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @InjectMocks
    private UserService userService;

    // Inscription avec un utilisateur null -> exception
    @Test
    public void test_create_null_user_throws_IllegalArgumentException() {
        // GIVEN

        // THEN
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> userService.register(null));
    }

    // Inscription avec un login déjà existant en base -> exception
    @Test
    public void test_create_already_exist_user_throws_IllegalArgumentException() {
        // GIVEN
        User user = new User();
        user.setFirstName(FIRST_NAME);
        user.setLastName(LAST_NAME);
        user.setLogin(LOGIN);
        user.setPassword(PASSWORD);
        when(passwordEncoder.encode(PASSWORD)).thenReturn(PASSWORD);
        when(userRepository.findByLogin(any())).thenReturn(Optional.of(user));

        // THEN
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> userService.register(user));
    }

    // Inscription valide -> l'utilisateur encodé est bien sauvegardé
    @Test
    public void test_create_user() {
        // GIVEN
        User user = new User();
        user.setFirstName(FIRST_NAME);
        user.setLastName(LAST_NAME);
        user.setLogin(LOGIN);
        user.setPassword(PASSWORD);
        when(passwordEncoder.encode(PASSWORD)).thenReturn(PASSWORD);
        when(userRepository.findByLogin(any())).thenReturn(Optional.empty());

        // WHEN
        userService.register(user);

        // THEN
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue()).isEqualTo(user);
    }

    // Connexion avec un login inconnu -> exception "Invalid credentials"
    @Test
    public void test_login_unknown_user_throws_IllegalArgumentException() {
        // GIVEN
        when(userRepository.findByLogin("unknown")).thenReturn(Optional.empty());

        // THEN
        IllegalArgumentException exception = Assertions.assertThrows(IllegalArgumentException.class,
                () -> userService.login("unknown", PASSWORD));
        assertThat(exception.getMessage()).isEqualTo("Invalid credentials");
    }

    // Connexion avec un mauvais mot de passe -> exception "Invalid credentials"
    @Test
    public void test_login_wrong_password_throws_IllegalArgumentException() {
        // GIVEN
        User user = new User();
        user.setFirstName(FIRST_NAME);
        user.setLastName(LAST_NAME);
        user.setLogin(LOGIN);
        user.setPassword(PASSWORD);
        when(userRepository.findByLogin(LOGIN)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", user.getPassword())).thenReturn(false);

        // THEN
        IllegalArgumentException exception = Assertions.assertThrows(IllegalArgumentException.class,
                () -> userService.login(LOGIN, "wrong"));
        assertThat(exception.getMessage()).isEqualTo("Invalid credentials");
    }

    // Connexion valide -> un token JWT est généré pour l'utilisateur
    @Test
    public void test_login_successful_returns_jwtToken() {
        // GIVEN
        User user = new User();
        user.setFirstName(FIRST_NAME);
        user.setLastName(LAST_NAME);
        user.setLogin(LOGIN);
        user.setPassword(PASSWORD);
        when(userRepository.findByLogin(LOGIN)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(PASSWORD, user.getPassword())).thenReturn(true);
        when(jwtService.generateToken(any())).thenReturn("jwt-token");

        // WHEN
        String token = userService.login(LOGIN, PASSWORD);

        // THEN
        ArgumentCaptor<UserDetails> userDetailsCaptor = ArgumentCaptor.forClass(UserDetails.class);
        verify(jwtService).generateToken(userDetailsCaptor.capture());
        assertThat(userDetailsCaptor.getValue().getUsername()).isEqualTo(LOGIN);
        assertThat(userDetailsCaptor.getValue().getPassword()).isEqualTo(user.getPassword());
        assertThat(token).isEqualTo("jwt-token");
    }
}
