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
}