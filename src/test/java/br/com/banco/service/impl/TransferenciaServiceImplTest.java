package br.com.banco.service.impl;

import br.com.banco.entity.ContaBancaria;
import br.com.banco.entity.Transacao;
import br.com.banco.repository.ContaBancariaRepository;
import br.com.banco.repository.TransacaoRepository;
import jakarta.persistence.OptimisticLockException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.never;

class TransferenciaServiceImplTest {

    @Mock
    private ContaBancariaRepository contaBancariaRepository;

    @Mock
    private TransacaoRepository transacaoRepository;

    @InjectMocks
    private TransferenciaServiceImpl transferenciaService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
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

        transferenciaService.transferir(contaOrigemNumero, contaDestinoNumero, valor);

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
            transferenciaService.transferir(contaOrigemNumero, contaDestinoNumero, valor);
        });

        assertEquals("Saldo insuficiente para realizar a transferência.", exception.getMessage());
        verify(contaBancariaRepository, never()).save(contaOrigem);
        verify(contaBancariaRepository, never()).save(contaDestino);
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
            transferenciaService.transferir(contaOrigem, contaDestino, valor);
        });

        assertEquals("Conflito de versão detectado. Tente novamente.", exception.getMessage());
        verify(contaBancariaRepository, times(1)).findByNumeroConta(contaOrigem);
        verify(contaBancariaRepository, times(1)).findByNumeroConta(contaDestino);
        verify(contaBancariaRepository, times(1)).save(origem);
        verify(contaBancariaRepository, never()).save(destino);
        verify(transacaoRepository, never()).save(any(Transacao.class));
    }
}