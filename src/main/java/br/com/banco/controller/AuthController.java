package br.com.banco.controller;

import br.com.banco.dto.UsuarioDTO;
import br.com.banco.entity.ContaBancaria;
import br.com.banco.entity.Usuario;
import br.com.banco.service.TransferenciaService;
import org.springframework.security.core.Authentication;
import br.com.banco.service.ContaBancariaService;
import br.com.banco.service.UsuarioService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;

@Controller
@PreAuthorize("hasRole('USER')")
public class AuthController {

    private final UsuarioService usuarioService;
    private final ContaBancariaService contaBancariaService;
    private final TransferenciaService transferenciaService;

    public AuthController(UsuarioService usuarioService, ContaBancariaService contaBancariaService, TransferenciaService transferenciaService) {
        this.usuarioService = usuarioService;
        this.contaBancariaService = contaBancariaService;
        this.transferenciaService = transferenciaService;
    }

    @GetMapping("index")
    public String home(){
        return "index";
    }

    @GetMapping("/login")
    public String loginForm() {
        return "login";
    }

//    @GetMapping("register")
//    public String showRegistrationForm(Model model){
//        UsuarioDTO usuarioDTO = new UsuarioDTO();
//        model.addAttribute("user", usuarioDTO);
//        return "register_";
//    }

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

    @PostMapping("/usuario/operacao")
    public String realizarOperacao(@RequestParam String numeroConta, @RequestParam BigDecimal valor, @RequestParam String tipoOperacao,
                                   Authentication authentication, RedirectAttributes redirectAttributes, Model model) {
        String emailUsuarioLogado = authentication.getName();
        try {
            if (!contaBancariaService.verificarContaUsuario(emailUsuarioLogado, numeroConta)) {
                throw new SecurityException("Operação não permitida para esta conta.");
            }
            if ("CREDITO".equals(tipoOperacao)) {
                contaBancariaService.creditar(numeroConta, valor);
                redirectAttributes.addFlashAttribute("success", "Crédito realizado com sucesso");
            } else if ("DEBITO".equals(tipoOperacao)) {
                contaBancariaService.debitar(numeroConta, valor);
                redirectAttributes.addFlashAttribute("success", "Débito realizado com sucesso");
            }
            return "redirect:/usuario/dashboard";
        } catch (IllegalArgumentException | SecurityException e) {
            model.addAttribute("error", e.getMessage());
            ContaBancaria conta = contaBancariaService.consultarEmail(emailUsuarioLogado);
            model.addAttribute("titular", conta.getTitular());
            model.addAttribute("email", emailUsuarioLogado);
            model.addAttribute("numeroConta", numeroConta);
            return "usuario/dashboard";
        }
    }

    @PostMapping("/usuario/transferir")
    public String transferir(@RequestParam String numeroContaDestino, @RequestParam BigDecimal valor,
                             Authentication authentication, RedirectAttributes redirectAttributes, Model model) {
        String emailUsuarioLogado = authentication.getName();
        try {
            ContaBancaria contaOrigem = contaBancariaService.consultarEmail(emailUsuarioLogado);
            transferenciaService.transferir(contaOrigem.getNumeroConta(), numeroContaDestino, valor);
            redirectAttributes.addFlashAttribute("success", "Transferência realizada com sucesso.");
            return "redirect:/usuario/dashboard";
        } catch (IllegalArgumentException | IllegalStateException e) {
            model.addAttribute("error", e.getMessage());
            ContaBancaria conta = contaBancariaService.consultarEmail(emailUsuarioLogado);
            model.addAttribute("titular", conta.getTitular());
            model.addAttribute("email", emailUsuarioLogado);
            model.addAttribute("numeroConta", conta.getNumeroConta());
            return "usuario/dashboard";
        }
    }
}
