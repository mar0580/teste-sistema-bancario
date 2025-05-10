package br.com.banco.controller;

import br.com.banco.dto.UsuarioDTO;
import br.com.banco.entity.ContaBancaria;
import br.com.banco.entity.Usuario;
import br.com.banco.service.ContaBancariaService;
import br.com.banco.service.UsuarioService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UsuarioService usuarioService;
    private final ContaBancariaService contaBancariaService;

    public AdminController (ContaBancariaService contaBancariaService, UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
        this.contaBancariaService = contaBancariaService;}

    @GetMapping("/criar-conta")
    public String mostrarFormularioCriacao(Model model) {
        model.addAttribute("conta", new ContaBancaria());
        model.addAttribute("contaBancaria", new ContaBancaria());
        return "/admin/criar-conta";
    }

    @PostMapping("/criar-conta")
    public String criarConta(@ModelAttribute ContaBancaria conta, @RequestParam String email, @RequestParam String senha) {
        contaBancariaService.criarConta(conta.getTitular(), conta.getNumeroConta(), email, senha);
        return "admin/criar-conta";
    }

    @GetMapping("/listar-contas")
    public String listarContas(Model model) {
        model.addAttribute("contas", contaBancariaService.listarContas());
        List<ContaBancaria> contas = contaBancariaService.listarContas();
        contas.forEach(conta -> conta.setDataCriacaoFormatada(
                conta.getDataCriacao().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
        ));
        model.addAttribute("contaBancaria", new ContaBancaria());
        return "admin/listar-contas";
    }

    @PostMapping("/criar-usuario")
    public String salvarUsuario(@ModelAttribute("user") UsuarioDTO usuarioDTO, Model model) {
        Usuario existing = usuarioService.findByEmail(usuarioDTO.getEmail());
        if (existing != null) {
            model.addAttribute("user", usuarioDTO);
            model.addAttribute("error", "Já existe uma conta registrada com esse e-mail.");
            return "admin/criar-usuario";
        }
        usuarioService.saveUser(usuarioDTO);
        return "redirect:/admin/listar-usuarios";
    }

    @GetMapping("/criar-usuario")
    public String criarUsuarioForm(Model model) {
        model.addAttribute("user", new UsuarioDTO());
        return "admin/criar-usuario";
    }
}