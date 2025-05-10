package br.com.banco.service.impl;

import br.com.banco.dto.UsuarioDTO;
import br.com.banco.entity.Role;
import br.com.banco.entity.Usuario;
import br.com.banco.repository.RoleRepository;
import br.com.banco.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UsuarioServiceImplTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UsuarioServiceImpl usuarioService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @DisplayName("Verifica se o usuário é salvo corretamente com os dados fornecidos")
    @Test
    void testSaveUser() {
        UsuarioDTO usuarioDTO = new UsuarioDTO(null, "João", "joao@email.com", "senha123");
        Role role = new Role(1L, "ROLE_ADMIN", Collections.emptyList());
        when(roleRepository.findByTipoUsuario("ROLE_ADMIN")).thenReturn(role);
        when(passwordEncoder.encode("senha123")).thenReturn("senhaCriptografada");

        usuarioService.saveUser(usuarioDTO);

        verify(usuarioRepository, times(1)).save(any(Usuario.class));
    }

    @DisplayName("Testa a busca de um usuário pelo e-mail")
    @Test
    void testFindByEmail() {
        String email = "joao@email.com";
        Usuario usuario = new Usuario(1L, "João", email, "senha123", LocalDateTime.now(), Collections.emptyList());
        when(usuarioRepository.findByEmail(email)).thenReturn(usuario);

        Usuario result = usuarioService.findByEmail(email);

        assertNotNull(result);
        assertEquals(email, result.getEmail());
        verify(usuarioRepository, times(1)).findByEmail(email);
    }

    @DisplayName("Verifica se todos os usuários são retornados corretamente")
    @Test
    void testFindAllUsers() {
        Usuario usuario1 = new Usuario(1L, "João", "joao@email.com", "senha123",
                LocalDateTime.now(), List.of());
        Usuario usuario2 = new Usuario(2L, "Maria", "maria@email.com", "senha456",
                LocalDateTime.now(), List.of());

        when(usuarioRepository.findAll()).thenReturn(List.of(usuario1, usuario2));

        List<UsuarioDTO> usuarios = usuarioService.findAllUsers();

        assertNotNull(usuarios);
        assertEquals(2, usuarios.size());
        verify(usuarioRepository, times(1)).findAll();
    }

    @DisplayName("Deve criar uma nova role quando não existir e salvar o usuário")
    @Test
    void testSaveUserWhenRoleIsNull() {
        // Arrange
        UsuarioDTO usuarioDTO = new UsuarioDTO(null, "João", "joao@email.com", "senha123");
        Role novaRole = new Role(1L, "ROLE_ADMIN", Collections.emptyList());
        when(roleRepository.findByTipoUsuario("ROLE_ADMIN")).thenReturn(null);
        when(roleRepository.save(any(Role.class))).thenReturn(novaRole);
        when(passwordEncoder.encode("senha123")).thenReturn("senhaCriptografada");

        // Act
        usuarioService.saveUser(usuarioDTO);

        // Assert
        verify(roleRepository, times(1)).findByTipoUsuario("ROLE_ADMIN");
        verify(roleRepository, times(1)).save(any(Role.class));
        verify(usuarioRepository, times(1)).save(any(Usuario.class));
    }
}