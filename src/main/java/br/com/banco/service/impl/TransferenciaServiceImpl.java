package br.com.banco.service.impl;

import br.com.banco.entity.ContaBancaria;
import br.com.banco.entity.Transacao;
import br.com.banco.model.TipoTransacao;
import br.com.banco.repository.ContaBancariaRepository;
import br.com.banco.repository.RoleRepository;
import br.com.banco.repository.TransacaoRepository;
import br.com.banco.repository.UsuarioRepository;
import br.com.banco.service.TransferenciaService;
import jakarta.persistence.OptimisticLockException;
import jakarta.transaction.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class TransferenciaServiceImpl implements TransferenciaService {

    private final ContaBancariaRepository contaBancariaRepository;
    private final UsuarioRepository usuarioRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final TransacaoRepository transacaoRepository;

    public TransferenciaServiceImpl(ContaBancariaRepository contaBancariaRepository, UsuarioRepository usuarioRepository,
                                    RoleRepository roleRepository, PasswordEncoder passwordEncoder, TransacaoRepository transacaoRepository) {
        this.contaBancariaRepository = contaBancariaRepository;
        this.usuarioRepository = usuarioRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.transacaoRepository = transacaoRepository;
    }

    @Transactional
    public void transferir(String contaOrigem, String contaDestino, BigDecimal valor) {

        if (contaOrigem.equals(contaDestino)) {
            throw new IllegalArgumentException("A conta de destino não pode ser a mesma que a conta de origem.");
        }

        ContaBancaria origem = buscarContaPorNumero(contaOrigem);
        ContaBancaria destino = buscarContaPorNumero(contaDestino);

        try {
            if (origem.getSaldo().compareTo(valor) < 0) {
                throw new IllegalArgumentException("Saldo insuficiente para realizar a transferência.");
            }

            origem.debitar(valor);
            destino.creditar(valor);

            contaBancariaRepository.save(origem);
            contaBancariaRepository.save(destino);

            // Registra a transação de débito para a conta de origem
            Transacao transacaoDebito = Transacao.builder()
                    .tipoTransacao(TipoTransacao.TRANSFERENCIA)
                    .valor(valor)
                    .dataHora(LocalDateTime.now())
                    .contaOrigem(origem)
                    .contaDestino(destino)
                    .build();
            transacaoRepository.save(transacaoDebito);

            // Registra a transação de crédito para a conta de destino
            Transacao transacaoCredito = Transacao.builder()
                    .tipoTransacao(TipoTransacao.TRANSFERENCIA)
                    .valor(valor)
                    .dataHora(LocalDateTime.now())
                    .contaOrigem(origem)
                    .contaDestino(destino)
                    .build();
            transacaoRepository.save(transacaoCredito);
        } catch (OptimisticLockException e) {
            throw new IllegalStateException("Conflito de versão detectado. Tente novamente.", e);
        }
    }

    private ContaBancaria buscarContaPorNumero(String numeroConta) {
        return contaBancariaRepository.findByNumeroConta(numeroConta)
                .orElseThrow(() -> new IllegalArgumentException("Conta bancária não encontrada: " + numeroConta));
    }
}
