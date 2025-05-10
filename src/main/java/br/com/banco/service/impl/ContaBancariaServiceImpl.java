package br.com.banco.service.impl;

import br.com.banco.entity.ContaBancaria;
import br.com.banco.entity.Role;
import br.com.banco.entity.Usuario;
import br.com.banco.repository.ContaBancariaRepository;
import br.com.banco.repository.RoleRepository;
import br.com.banco.repository.UsuarioRepository;
import br.com.banco.service.ContaBancariaService;
import jakarta.transaction.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ContaBancariaServiceImpl implements ContaBancariaService {

    private final ContaBancariaRepository contaBancariaRepository;
    private final UsuarioRepository usuarioRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public ContaBancariaServiceImpl(ContaBancariaRepository contaBancariaRepository, UsuarioRepository usuarioRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.contaBancariaRepository = contaBancariaRepository;
        this.usuarioRepository = usuarioRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void criarConta(String titular, String numero, String email, String senha) {
        validarNumeroContaUnico(numero);

        Usuario usuario = criarUsuario(titular, email, senha);
        ContaBancaria conta = criarContaBancaria(titular, numero);

        usuarioRepository.save(usuario);
        contaBancariaRepository.save(conta);
    }

    private void validarNumeroContaUnico(String numero) {
        if (contaBancariaRepository.findByNumeroConta(numero).isPresent()) {
            throw new IllegalArgumentException("Número da conta já está em uso");
        }
    }

    private Usuario criarUsuario(String titular, String email, String senha) {
        Usuario usuario = new Usuario();
        usuario.setNome(titular);
        usuario.setEmail(email);
        usuario.setPassword(passwordEncoder.encode(senha));
        usuario.setDataCriacao(LocalDateTime.now());
        usuario.setRoles(List.of(obterRoleUsuario()));
        return usuario;
    }

    private Role obterRoleUsuario() {
        Role role = roleRepository.findByTipoUsuario("ROLE_USER");
        if (role == null) {
            role = new Role();
            role.setTipoUsuario("ROLE_USER");
            role = roleRepository.save(role);
        }
        return role;
    }

    private ContaBancaria criarContaBancaria(String titular, String numero) {
        ContaBancaria conta = new ContaBancaria();
        conta.setTitular(titular);
        conta.setNumeroConta(numero);
        conta.setDataCriacao(LocalDateTime.now());
        conta.setSaldo(BigDecimal.ZERO);
        return conta;
    }

    @Override
    @Transactional
    public void creditar(String numeroConta, BigDecimal valor) {
        ContaBancaria conta = buscarContaPorNumero(numeroConta);
        synchronized (conta) {
            conta.creditar(valor);
            contaBancariaRepository.save(conta);
        }
    }

    @Override
    @Transactional
    public void debitar(String numeroConta, BigDecimal valor) {
        ContaBancaria conta = buscarContaComLock(numeroConta);
        if (conta.getSaldo().compareTo(valor) < 0) {
            throw new IllegalArgumentException("Saldo insuficiente para realizar o débito.");
        }
        conta.debitar(valor);
        contaBancariaRepository.save(conta);
    }

    @Override
    @Transactional
    public void transferir(String contaOrigem, String contaDestino, BigDecimal valor) {
        ContaBancaria origem = buscarContaComLock(contaOrigem);
        ContaBancaria destino = buscarContaComLock(contaDestino);

        if (origem.getSaldo().compareTo(valor) < 0) {
            throw new IllegalArgumentException("Saldo insuficiente para realizar a transferência.");
        }

        origem.debitar(valor);
        destino.creditar(valor);

        contaBancariaRepository.save(origem);
        contaBancariaRepository.save(destino);
    }

    private ContaBancaria buscarContaPorNumero(String numeroConta) {
        return contaBancariaRepository.findByNumeroConta(numeroConta)
                .orElseThrow(() -> new IllegalArgumentException("Conta não encontrada"));
    }

    private ContaBancaria buscarContaComLock(String numeroConta) {
        return contaBancariaRepository.findByNumeroContaWithLock(numeroConta)
                .orElseThrow(() -> new IllegalArgumentException("Conta não encontrada"));
    }

    @Override
    public BigDecimal consultarSaldo(String numeroConta) {
        return buscarContaPorNumero(numeroConta).getSaldo();
    }

    @Override
    @Transactional
    public List<ContaBancaria> listarContas() {
        return contaBancariaRepository.findAll();
    }

    @Override
    public boolean verificarContaUsuario(String email, String numeroConta) {
        Usuario usuario = buscarUsuarioPorEmail(email);
        return contaBancariaRepository.findByNumeroConta(numeroConta)
                .map(conta -> conta.getTitular().equals(usuario.getNome()))
                .orElse(false);
    }

    @Override
    public ContaBancaria consultarEmail(String email) {
        Usuario usuario = buscarUsuarioPorEmail(email);
        return contaBancariaRepository.findByTitular(usuario.getNome())
                .orElseThrow(() -> new IllegalArgumentException("Conta não encontrada para o titular: " + usuario.getNome()));
    }

    private Usuario buscarUsuarioPorEmail(String email) {
        Usuario usuario = usuarioRepository.findByEmail(email);
        if (usuario == null) {
            throw new IllegalArgumentException("Usuário não encontrado.");
        }
        return usuario;
    }
}
