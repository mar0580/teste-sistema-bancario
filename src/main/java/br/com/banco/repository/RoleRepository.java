package br.com.banco.repository;

import br.com.banco.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Role findByTipoUsuario(String tipoUsuario);
}
