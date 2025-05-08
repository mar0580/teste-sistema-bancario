package br.com.banco.controller;

import br.com.banco.dto.UsuarioDTO;
import br.com.banco.entity.Usuario;
import br.com.banco.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;

@Controller
public class AuthController {

    private UsuarioService usuarioService;

    public AuthController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping("index")
    public String home(){
        return "index";
    }

    @GetMapping("/login")
    public String loginForm() {
        return "login";
    }

    @GetMapping("register")
    public String showRegistrationForm(Model model){
        UsuarioDTO user = new UsuarioDTO();
        model.addAttribute("user", user);
        return "register";
    }

    @PostMapping("/register/save")
    public String registration(@Valid @ModelAttribute("user") UsuarioDTO usuarioDTO,
                               BindingResult result,
                               Model model){
        Usuario existing = usuarioService.findByEmail(usuarioDTO.getEmail());
        if (existing != null) {
            result.rejectValue("email", null, "There is already an account registered with that email");
        }
        if (result.hasErrors()) {
            model.addAttribute("user", usuarioDTO);
            return "register";
        }
        usuarioService.saveUser(usuarioDTO);
        return "redirect:/register?success";
    }

    @GetMapping("/users")
    public String listRegisteredUsers(Model model){
        List<UsuarioDTO> usuarios = usuarioService.findAllUsers();
        model.addAttribute("users", usuarios);
        return "users";
    }
}
