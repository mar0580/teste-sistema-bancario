package br.com.banco.service.impl;

import br.com.banco.entity.ContaBancaria;
import br.com.banco.entity.Role;
import br.com.banco.entity.Transacao;
import br.com.banco.entity.Usuario;
import br.com.banco.repository.ContaBancariaRepository;
import br.com.banco.repository.RoleRepository;
import br.com.banco.repository.TransacaoRepository;
import br.com.banco.repository.UsuarioRepository;
import jakarta.persistence.OptimisticLockException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ContaBancariaServiceImplTest {

    @Mock
    private ContaBancariaRepository contaBancariaRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TransacaoRepository transacaoRepository;

    @InjectMocks
    private ContaBancariaServiceImpl contaBancariaService;

    @InjectMocks
    private UsuarioServiceImpl usuarioService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @DisplayName("Verifica se a conta e o usuário são criados corretamente")
    @Test
    void testCriarConta() {
        String titular = "João";
        String numero = "12345";
        String email = "joao@email.com";
        String senha = "senha123";

        when(contaBancariaRepository.findByNumeroConta(numero)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(senha)).thenReturn("senhaCriptografada");
        when(roleRepository.findByTipoUsuario("ROLE_USER")).thenReturn(new Role());

        contaBancariaService.criarConta(titular, numero, email, senha);

        verify(usuarioRepository, times(1)).save(any(Usuario.class));
        verify(contaBancariaRepository, times(1)).save(any(ContaBancaria.class));
    }

    @DisplayName("Testa a adição de saldo à conta")
    @Test
    void testCreditar() {
        String numeroConta = "12345";
        BigDecimal valor = BigDecimal.valueOf(100);
        ContaBancaria conta = new ContaBancaria();
        conta.setSaldo(BigDecimal.ZERO);

        when(contaBancariaRepository.findByNumeroConta(numeroConta)).thenReturn(Optional.of(conta));

        contaBancariaService.creditar(numeroConta, valor);

        assertEquals(BigDecimal.valueOf(100), conta.getSaldo());
        verify(contaBancariaRepository, times(1)).save(conta);
        verify(transacaoRepository, times(1)).save(any());
    }

    @DisplayName("Testa a subtração de saldo da conta")
    @Test
    void testDebitar() {
        String numeroConta = "12345";
        BigDecimal valor = BigDecimal.valueOf(50);
        ContaBancaria conta = new ContaBancaria();
        conta.setSaldo(BigDecimal.valueOf(100));

        when(contaBancariaRepository.findByNumeroConta(numeroConta)).thenReturn(Optional.of(conta));

        contaBancariaService.debitar(numeroConta, valor);

        assertEquals(BigDecimal.valueOf(50), conta.getSaldo());
        verify(contaBancariaRepository, times(1)).save(conta);
        verify(transacaoRepository, times(1)).save(any());
    }

    @DisplayName("Garante que uma exceção é lançada ao tentar debitar mais do que o saldo disponível")
    @Test
    void testDebitarSaldoInsuficiente() {
        String numeroConta = "12345";
        BigDecimal valor = BigDecimal.valueOf(150);
        ContaBancaria conta = new ContaBancaria();
        conta.setSaldo(BigDecimal.valueOf(100));

        when(contaBancariaRepository.findByNumeroConta(numeroConta)).thenReturn(Optional.of(conta));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            contaBancariaService.debitar(numeroConta, valor);
        });

        assertEquals("Saldo insuficiente para realizar o débito.", exception.getMessage());
        verify(contaBancariaRepository, never()).save(conta);
    }

    @DisplayName("Verifica se a conta pertence ao usuário")
    @Test
    void testVerificarContaUsuario() {
        String email = "joao@email.com";
        String numeroConta = "12345";
        Usuario usuario = new Usuario();
        usuario.setNome("João");
        ContaBancaria conta = new ContaBancaria();
        conta.setTitular("João");

        when(usuarioRepository.findByEmail(email)).thenReturn(usuario);
        when(contaBancariaRepository.findByNumeroConta(numeroConta)).thenReturn(Optional.of(conta));

        boolean resultado = contaBancariaService.verificarContaUsuario(email, numeroConta);

        assertTrue(resultado);
    }

    @DisplayName("Verifica se a transferência entre contas é realizada corretamente")
    @Test
    void testTransferir() {
        String contaOrigemNumero = "12345";
        String contaDestinoNumero = "67890";
        BigDecimal valor = BigDecimal.valueOf(50);

        ContaBancaria contaOrigem = new ContaBancaria();
        contaOrigem.setSaldo(BigDecimal.valueOf(100));
        ContaBancaria contaDestino = new ContaBancaria();
        contaDestino.setSaldo(BigDecimal.valueOf(200));

        when(contaBancariaRepository.findByNumeroConta(contaOrigemNumero)).thenReturn(Optional.of(contaOrigem));
        when(contaBancariaRepository.findByNumeroConta(contaDestinoNumero)).thenReturn(Optional.of(contaDestino));

        contaBancariaService.transferir(contaOrigemNumero, contaDestinoNumero, valor);

        assertEquals(BigDecimal.valueOf(50), contaOrigem.getSaldo());
        assertEquals(BigDecimal.valueOf(250), contaDestino.getSaldo());
        verify(contaBancariaRepository, times(1)).save(contaOrigem);
        verify(contaBancariaRepository, times(1)).save(contaDestino);
        verify(transacaoRepository, times(2)).save(any());
    }

    @DisplayName("Garante que uma exceção é lançada ao tentar transferir mais do que o saldo disponível")
    @Test
    void testTransferirSaldoInsuficiente() {
        String contaOrigemNumero = "12345";
        String contaDestinoNumero = "67890";
        BigDecimal valor = BigDecimal.valueOf(150);

        ContaBancaria contaOrigem = new ContaBancaria();
        contaOrigem.setSaldo(BigDecimal.valueOf(100));
        ContaBancaria contaDestino = new ContaBancaria();
        contaDestino.setSaldo(BigDecimal.valueOf(200));

        when(contaBancariaRepository.findByNumeroConta(contaOrigemNumero)).thenReturn(Optional.of(contaOrigem));
        when(contaBancariaRepository.findByNumeroConta(contaDestinoNumero)).thenReturn(Optional.of(contaDestino));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            contaBancariaService.transferir(contaOrigemNumero, contaDestinoNumero, valor);
        });

        assertEquals("Saldo insuficiente para realizar a transferência.", exception.getMessage());
        verify(contaBancariaRepository, never()).save(contaOrigem);
        verify(contaBancariaRepository, never()).save(contaDestino);
    }

    @DisplayName("Testa a consulta de uma conta bancária pelo e-mail do titular")
    @Test
    void testConsultarEmail() {
        String email = "joao@email.com";
        Usuario usuario = new Usuario();
        usuario.setNome("João");
        ContaBancaria conta = new ContaBancaria();
        conta.setTitular("João");

        when(usuarioRepository.findByEmail(email)).thenReturn(usuario);
        when(contaBancariaRepository.findByTitular("João")).thenReturn(Optional.of(conta));

        ContaBancaria resultado = contaBancariaService.consultarEmail(email);

        assertNotNull(resultado);
        assertEquals("João", resultado.getTitular());
        verify(usuarioRepository, times(1)).findByEmail(email);
        verify(contaBancariaRepository, times(1)).findByTitular("João");
    }

    @DisplayName("Garante que uma exceção é lançada ao tentar criar uma conta com número já existente")
    @Test
    void testValidarNumeroContaUnico() {
        String numeroContaExistente = "12345";
        when(contaBancariaRepository.findByNumeroConta(numeroContaExistente)).thenReturn(Optional.of(new ContaBancaria()));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            contaBancariaService.criarConta("João", numeroContaExistente, "joao@email.com", "senha123");
        });

        assertEquals("Número da conta já está em uso", exception.getMessage());
        verify(contaBancariaRepository, times(1)).findByNumeroConta(numeroContaExistente);
        verify(contaBancariaRepository, never()).save(any(ContaBancaria.class));
    }

    @DisplayName("Deve lançar IllegalArgumentException quando saldo é insuficiente")
    @Test
    void testDebitarSaldoInsuficienteException() {
        String numeroConta = "12345";
        BigDecimal valor = BigDecimal.valueOf(100);
        ContaBancaria conta = new ContaBancaria();
        conta.setSaldo(BigDecimal.valueOf(50));

        when(contaBancariaRepository.findByNumeroConta(numeroConta)).thenReturn(Optional.of(conta));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            contaBancariaService.debitar(numeroConta, valor);
        });

        assertEquals("Saldo insuficiente para realizar o débito.", exception.getMessage());
        verify(contaBancariaRepository, never()).save(conta);
        verify(transacaoRepository, never()).save(any(Transacao.class));
    }

    @DisplayName("Deve lançar IllegalStateException em caso de OptimisticLockException")
    @Test
    void testDebitarOptimisticLockException() {
        String numeroConta = "12345";
        BigDecimal valor = BigDecimal.valueOf(50);
        ContaBancaria conta = new ContaBancaria();
        conta.setSaldo(BigDecimal.valueOf(100));

        when(contaBancariaRepository.findByNumeroConta(numeroConta)).thenReturn(Optional.of(conta));
        doThrow(new OptimisticLockException("Conflito de versão")).when(contaBancariaRepository).save(conta);

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            contaBancariaService.debitar(numeroConta, valor);
        });

        assertEquals("Conflito de versão detectado. Tente novamente.", exception.getMessage());
        verify(contaBancariaRepository, times(1)).save(conta);
        verify(transacaoRepository, never()).save(any(Transacao.class));
    }
/*
    @DisplayName("Deve retornar a role existente ou criar uma nova se não existir")
    @Test
    void testObterRoleUsuario() {
        // Arrange
        Role roleExistente = new Role(1L, "ROLE_USER", Collections.emptyList());
        when(roleRepository.findByTipoUsuario("ROLE_USER")).thenReturn(null, roleExistente);
        when(roleRepository.save(any(Role.class))).thenReturn(roleExistente);

        // Act
        Role roleCriada = usuarioService.obterRoleUsuario();
        Role roleExistenteRetornada = usuarioService.obterRoleUsuario();

        // Assert
        assertNotNull(roleCriada);
        assertEquals("ROLE_USER", roleCriada.getTipoUsuario());
        verify(roleRepository, times(1)).save(any(Role.class));

        assertNotNull(roleExistenteRetornada);
        assertEquals("ROLE_USER", roleExistenteRetornada.getTipoUsuario());
        verify(roleRepository, times(1)).findByTipoUsuario("ROLE_USER");
    }
*/
    @DisplayName("Deve listar todas as contas bancárias")
    @Test
    void testListarContas() {
        // Arrange
        ContaBancaria conta1 = new ContaBancaria(1L, "12345", "João", BigDecimal.valueOf(100), LocalDateTime.now(), null, null);
        ContaBancaria conta2 = new ContaBancaria(2L, "67890", "Maria", BigDecimal.valueOf(200), LocalDateTime.now(), null, null);
        when(contaBancariaRepository.findAll()).thenReturn(List.of(conta1, conta2));

        // Act
        List<ContaBancaria> contas = contaBancariaService.listarContas();

        // Assert
        assertNotNull(contas);
        assertEquals(2, contas.size());
        verify(contaBancariaRepository, times(1)).findAll();
    }

    @DisplayName("Deve lançar IllegalStateException ao ocorrer OptimisticLockException durante transferência")
    @Test
    void testTransferirOptimisticLockException() {
        // Arrange
        String contaOrigem = "12345";
        String contaDestino = "67890";
        BigDecimal valor = BigDecimal.valueOf(100);

        ContaBancaria origem = new ContaBancaria(1L, contaOrigem, "João", BigDecimal.valueOf(200), LocalDateTime.now(), null, null);
        ContaBancaria destino = new ContaBancaria(2L, contaDestino, "Maria", BigDecimal.valueOf(300), LocalDateTime.now(), null, null);

        when(contaBancariaRepository.findByNumeroConta(contaOrigem)).thenReturn(Optional.of(origem));
        when(contaBancariaRepository.findByNumeroConta(contaDestino)).thenReturn(Optional.of(destino));
        doThrow(new OptimisticLockException("Conflito de versão")).when(contaBancariaRepository).save(origem);

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            contaBancariaService.transferir(contaOrigem, contaDestino, valor);
        });

        assertEquals("Conflito de versão detectado. Tente novamente.", exception.getMessage());
        verify(contaBancariaRepository, times(1)).findByNumeroConta(contaOrigem);
        verify(contaBancariaRepository, times(1)).findByNumeroConta(contaDestino);
        verify(contaBancariaRepository, times(1)).save(origem);
        verify(contaBancariaRepository, never()).save(destino);
        verify(transacaoRepository, never()).save(any(Transacao.class));
    }
/*
    @DisplayName("Deve retornar o usuário pelo e-mail ou lançar exceção se não encontrado")
    @Test
    void testBuscarUsuarioPorEmail() {
        // Arrange
        String email = "joao@email.com";
        Usuario usuario = new Usuario(1L, "João", email, "senha123", LocalDateTime.now(), Collections.emptyList());
        when(usuarioRepository.findByEmail(email)).thenReturn(usuario);

        // Act & Assert
        Usuario usuarioEncontrado = usuarioService.buscarUsuarioPorEmail(email);
        assertNotNull(usuarioEncontrado);
        assertEquals(email, usuarioEncontrado.getEmail());
        verify(usuarioRepository, times(1)).findByEmail(email);

        // Teste para exceção
        when(usuarioRepository.findByEmail("naoexiste@email.com")).thenReturn(null);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            usuarioService.buscarUsuarioPorEmail("naoexiste@email.com");
        });
        assertEquals("Usuário não encontrado.", exception.getMessage());
        verify(usuarioRepository, times(1)).findByEmail("naoexiste@email.com");
    }*/
}