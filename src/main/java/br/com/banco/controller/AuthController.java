package br.com.banco.controller;

import br.com.banco.dto.UsuarioDTO;
import br.com.banco.entity.ContaBancaria;
import br.com.banco.entity.Usuario;
import org.springframework.security.core.Authentication;
import br.com.banco.service.ContaBancariaService;
import br.com.banco.service.UsuarioService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;

@Controller
@PreAuthorize("hasRole('USER')")
public class AuthController {

    private final UsuarioService usuarioService;
    private final ContaBancariaService contaBancariaService;

    public AuthController(UsuarioService usuarioService, ContaBancariaService contaBancariaService) {
        this.usuarioService = usuarioService;
        this.contaBancariaService = contaBancariaService;
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
        UsuarioDTO usuarioDTO = new UsuarioDTO();
        model.addAttribute("user", usuarioDTO);
        return "register_";
    }

//    @PostMapping("/register/save")
//    public String registration(@Valid @ModelAttribute("user") UsuarioDTO usuarioDTO,
//                               BindingResult result,
//                               Model model){
//        Usuario existing = usuarioService.findByEmail(usuarioDTO.getEmail());
//        if (existing != null) {
//            result.rejectValue("email", null, "Já existe uma conta registrada com esse e-mail");
//        }
//        if (result.hasErrors()) {
//            model.addAttribute("user", usuarioDTO);
//            return "register_";
//        }
//        usuarioService.saveUser(usuarioDTO);
//        return "redirect:/register?success";
//    }

    @PostMapping("/usuario/creditar")
    public String creditar(@RequestParam String numeroConta, @RequestParam BigDecimal valor, Authentication authentication) {
        String emailUsuarioLogado = authentication.getName();
        if (!contaBancariaService.verificarContaUsuario(emailUsuarioLogado, numeroConta)) {
            throw new SecurityException("Operação não permitida para esta conta.");
        }
        contaBancariaService.creditar(numeroConta, valor);
        return "redirect:/usuario/dashboard";
    }

    @PostMapping("/usuario/debitar")
    public String debitar(@RequestParam String numeroConta, @RequestParam BigDecimal valor, Authentication authentication) {
        String emailUsuarioLogado = authentication.getName();
        if (!contaBancariaService.verificarContaUsuario(emailUsuarioLogado, numeroConta)) {
            throw new SecurityException("Operação não permitida para esta conta.");
        }
        contaBancariaService.debitar(numeroConta, valor);
        return "redirect:/usuario/dashboard";
    }

    @GetMapping("/usuario/dashboard")
    public String dashboardUsuario(Authentication authentication, Model model) {
        String emailUsuarioLogado = authentication.getName();
        Usuario usuario = usuarioService.findByEmail(emailUsuarioLogado);
        ContaBancaria conta = contaBancariaService.consultarEmail(usuario.getEmail());
        model.addAttribute("titular", usuario.getNome());
        model.addAttribute("email", usuario.getEmail());
        model.addAttribute("numeroConta", conta.getNumeroConta());
        return "usuario/dashboard";
    }
}
