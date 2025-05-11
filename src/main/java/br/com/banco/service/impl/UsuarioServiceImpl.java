package br.com.banco.service.impl;

import br.com.banco.dto.UsuarioDTO;
import br.com.banco.entity.Role;
import br.com.banco.entity.Usuario;
import br.com.banco.repository.RoleRepository;
import br.com.banco.repository.UsuarioRepository;
import br.com.banco.service.UsuarioService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioServiceImpl(UsuarioRepository userRepository,
                              RoleRepository roleRepository,
                              PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void saveUser(UsuarioDTO usuarioDTO) {
        Usuario usuario = new Usuario();
        usuario.setNome(usuarioDTO.getFirstName());
        usuario.setEmail(usuarioDTO.getEmail());
        usuario.setDataCriacao(LocalDateTime.now());
        //Para criptografar a senha depois de integrar ao Spring Security
        //user.setPassword(userDto.getPassword());
        usuario.setPassword(passwordEncoder.encode(usuarioDTO.getPassword()));
        Role role = roleRepository.findByTipoUsuario("ROLE_ADMIN");
        if(role == null){
            role = checkRoleExist();
        }
        usuario.setRoles(Arrays.asList(role));
        userRepository.save(usuario);
    }

    @Override
    public Usuario findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public List<UsuarioDTO> findAllUsers() {
        List<Usuario> usuarios = userRepository.findAll();
        return usuarios.stream().map((usuario) -> converterEntityToDto(usuario))
                .collect(Collectors.toList());
    }

    private UsuarioDTO converterEntityToDto(Usuario usuario){
        UsuarioDTO userDto = new UsuarioDTO();
        String[] name = usuario.getNome().split(" ");
        userDto.setFirstName(name[0]);
        userDto.setEmail(usuario.getEmail());
        return userDto;
    }

    private Role checkRoleExist() {
        Role role = new Role();
        role.setTipoUsuario("ROLE_ADMIN");
        return roleRepository.save(role);
    }
}
