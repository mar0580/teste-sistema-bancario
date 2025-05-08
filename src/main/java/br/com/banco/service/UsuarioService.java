package br.com.banco.service;

import br.com.banco.dto.UsuarioDTO;
import br.com.banco.entity.Usuario;

import java.util.List;

public interface UsuarioService {
    void saveUser(UsuarioDTO userDto);

    Usuario findByEmail(String email);

    List<UsuarioDTO> findAllUsers();
}
