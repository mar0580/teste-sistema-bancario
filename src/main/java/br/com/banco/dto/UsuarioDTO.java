package br.com.banco.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioDTO
{
    private Long id;
    @NotEmpty
    private String firstName;
    @NotEmpty(message = "O email não deve estar vazio")
//    @NotEmpty
//    private String lastName;
    @Email
    private String email;
    @NotEmpty(message = "Senha não deve estar vazia")
    private String password;
}
