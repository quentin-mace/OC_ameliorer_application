# Plan de tests

## Back end

### User Tests

- [x] Completer les tests d'authentification en mettant en place des tests sur le login
- [x] Mettre en place des tests unitaires pour les fonctions de login du userService

#### Cas de tests unitaires — `UserService.login(String login, String password)`

| # | Cas | Entrées | Mocks | Sortie attendue | Statut |
|---|-----|---------|-------|------------------|--------|
| 1 | Utilisateur introuvable | `login="unknown"`, `password="password"` | `userRepository.findByLogin("unknown")` → `Optional.empty()` | `IllegalArgumentException` : "Invalid credentials" | ✅ Fait |
| 2 | Mot de passe incorrect | `login="login"`, `password="wrong"` | `findByLogin` → `Optional.of(user)` ; `passwordEncoder.matches("wrong", user.getPassword())` → `false` | `IllegalArgumentException` : "Invalid credentials" | ✅ Fait |
| 3 | Login réussi | `login="login"`, `password="password"` | `findByLogin` → `Optional.of(user)` ; `passwordEncoder.matches(...)` → `true` ; `jwtService.generateToken(any())` → `"jwt-token"` | Retourne `"jwt-token"` | ✅ Fait — renforcé par `ArgumentCaptor` sur `UserDetails` transmis à `jwtService` (sinon l'assertion sur le retour ne faisait que répéter la valeur du mock) |

#### Cas de tests d'intégration — `POST /api/login` (MockMvc + Testcontainers)

| # | Cas | Entrées (body JSON) | Précondition | Sortie attendue | Statut |
|---|-----|----------------------|--------------|------------------|--------|
| 1 | Login réussi | `{"login": "existingUser", "password": "validPassword"}` | Utilisateur préalablement créé via `userService.register(...)` | `200 OK`, corps `{"token": "<jwt non vide>"}` | ✅ Fait |
| 2 | Login inexistant | `{"login": "doesNotExist", "password": "anyPassword"}` | Aucun utilisateur avec ce login | `400 BAD_REQUEST` (via `RestExceptionHandler` sur `IllegalArgumentException`), message `"Invalid credentials"` | ✅ Fait |
| 3 | Mauvais mot de passe | `{"login": "existingUser", "password": "wrongPassword"}` | Utilisateur existant | `400 BAD_REQUEST`, message `"Invalid credentials"` | ✅ Fait |

### Student Tests

- [ ] Mettre en place tout les tests pour le StudentController
- [ ] Faire de même pour le StudenService

#### Cas de tests unitaires — `StudentService` (Mockito, `@Mock StudentRepository`)

| # | Méthode | Cas | Entrées | Mocks | Sortie attendue | Statut |
|---|---------|-----|---------|-------|------------------|--------|
| 1 | `findAll()` | Liste non vide | — | `studentRepository.findAll()` → `List.of(student1, student2)` | Retourne la liste telle quelle | ⏭️ Non pertinent (délégation pure, testerait le mock plutôt que la logique) |
| 2 | `findAll()` | Liste vide | — | `studentRepository.findAll()` → `List.of()` | Retourne une liste vide | ⏭️ Non pertinent (idem) |
| 3 | `findById(id)` | Id trouvé | `id=1L` | `findById(1L)` → `Optional.of(student)` | Retourne l'entité `student` | ⏭️ Non retenu (moins intéressant que le cas 4) |
| 4 | `findById(id)` | Id introuvable | `id=99L` | `findById(99L)` → `Optional.empty()` | `ResponseStatusException` 404 NOT_FOUND, message `"Student not found with id: 99"` | ✅ Fait |
| 5 | `create(student)` | Création valide | `student` avec `firstName`/`lastName` renseignés | `studentRepository.save(student)` → `student` (avec id généré) | Retourne l'entité sauvegardée | ⏭️ Remplacé par un test sur `Assert.notNull` (seule vraie logique de `create`, non couverte par le plan) |
| 5bis | `create(student)` | Student `null` | `student=null` | — | `IllegalArgumentException` (`Assert.notNull`) | ✅ Fait |
| 6 | `update(id, studentDetails)` | Id introuvable | `id=99L`, `studentDetails` valide | `findById(99L)` → `Optional.empty()` | `ResponseStatusException` 404 NOT_FOUND (levée par l'appel interne à `findById`) ; `save()` jamais appelé | ✅ Fait |
| 7 | `update(id, studentDetails)` | Mise à jour valide | `id=1L`, `studentDetails={firstName:"New", lastName:"Name"}` | `findById(1L)` → `Optional.of(existingStudent)` ; `save(existingStudent)` → entité mise à jour | Retourne l'entité avec `firstName="New"`, `lastName="Name"` | ✅ Fait — vérifié via `ArgumentCaptor` que les champs sont bien copiés avant `save()` |
| 8 | `save(student)` | Sauvegarde directe | `student` valide | `studentRepository.save(student)` → `student` | Retourne l'entité sauvegardée | ⏭️ Non pertinent (délégation pure, comme `findAll()`) |
| 9 | `delete(id)` | Id trouvé | `id=1L` | `findById(1L)` → `Optional.of(student)` | Pas d'exception | ✅ Fait — vérifié via `verify` que `studentRepository.delete(student)` reçoit bien l'entité trouvée |
| 10 | `delete(id)` | Id introuvable | `id=99L` | `findById(99L)` → `Optional.empty()` | `ResponseStatusException` 404 NOT_FOUND | ✅ Fait — vérifié que `delete()` n'est jamais appelé |

#### Cas de tests d'intégration — `StudentController` (MockMvc + Testcontainers, JWT requis sauf mention contraire)

| # | Endpoint | Cas | Entrées | Précondition | Sortie attendue | Statut |
|---|----------|-----|---------|--------------|------------------|--------|
| 1 | `GET /api/students` | Sans token | — | — | `401 UNAUTHORIZED` (toutes les routes `/api/students/**` sont `authenticated()`) | ✅ Fait |
| 2 | `GET /api/students` | Liste vide | Header `Authorization: Bearer <token valide>` | Aucun étudiant en base | `200 OK`, corps `[]` | ✅ Fait |
| 3 | `GET /api/students` | Liste avec données | idem | 2 étudiants créés en base | `200 OK`, corps = liste de 2 `StudentResponseDTO` | ✅ Fait |
| 4 | `GET /api/students/{id}` | Id existant | `id` d'un étudiant créé | Étudiant existant | `200 OK`, corps = `StudentResponseDTO` correspondant | ✅ Fait |
| 5 | `GET /api/students/{id}` | Id inexistant | `id=99999` | — | `404 NOT_FOUND` | ✅ Fait |
| 6 | `POST /api/students` | Création valide | `{"firstName":"John","lastName":"Doe"}` | — | `200 OK` (⚠️ pas 201), corps = `StudentResponseDTO` avec `id`, `createdAt`, `updatedAt` renseignés | ✅ Fait |
| 7 | `PUT /api/students/{id}` | Mise à jour valide | `id` existant, `{"firstName":"Jane","lastName":"Doe"}` | Étudiant existant | `200 OK`, corps mis à jour | ✅ Fait |
| 8 | `PUT /api/students/{id}` | Id inexistant | `id=99999`, body valide | — | `404 NOT_FOUND` | ✅ Fait |
| 9 | `PATCH /api/students/{id}` | Patch partiel (un seul champ) | `id` existant, `{"firstName":"OnlyFirst"}` | Étudiant existant avec `lastName="Doe"` | `200 OK`, `firstName` mis à jour, `lastName` inchangé (`NullValuePropertyMappingStrategy.IGNORE`) | ✅ Fait |
| 10 | `PATCH /api/students/{id}` | Id inexistant | `id=99999`, body `{}` | — | `404 NOT_FOUND` | ✅ Fait |
| 11 | `DELETE /api/students/{id}` | Id existant | `id` existant | Étudiant existant | `204 NO_CONTENT` | ✅ Fait |
| 12 | `DELETE /api/students/{id}` | Id inexistant | `id=99999` | — | `404 NOT_FOUND` | ✅ Fait |