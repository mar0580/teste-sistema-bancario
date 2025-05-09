package br.com.banco.service.impl;

import br.com.banco.entity.ContaBancaria;
import br.com.banco.repository.ContaBancariaRepository;
import br.com.banco.service.ContaBancariaService;
import jakarta.persistence.OptimisticLockException;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ContaBancariaServiceImpl implements ContaBancariaService {

    private final ContaBancariaRepository contaBancariaRepository;

    public ContaBancariaServiceImpl(ContaBancariaRepository contaBancariaRepository) {
        this.contaBancariaRepository = contaBancariaRepository;
    }

    @Override
    @Transactional
    public ContaBancaria criarConta(String titular, String numero, String tipoConta) {
        if (contaBancariaRepository.findByNumeroConta(numero).isPresent()) {
            throw new IllegalArgumentException("Número da conta já está em uso");
        }
        ContaBancaria conta = new ContaBancaria();
        conta.setTitular(titular);
        conta.setNumeroConta(numero);
        conta.setTipoConta(tipoConta);
        conta.setDataCriacao(LocalDateTime.now());
        conta.setSaldo(BigDecimal.ZERO);
        return contaBancariaRepository.save(conta);
    }

    @Override
    @Transactional
    public void creditar(String numeroConta, BigDecimal valor) {
        ContaBancaria conta = contaBancariaRepository.findByNumeroConta(numeroConta)
                .orElseThrow(() -> new IllegalArgumentException("Conta não encontrada"));

        conta.lock();
        try {
            conta.creditar(valor);
            contaBancariaRepository.save(conta);
        } finally {
            conta.unlock();
        }
    }

    @Override
    @Transactional
    public void debitar(String numeroConta, BigDecimal valor) {

        ContaBancaria conta = contaBancariaRepository.findByNumeroContaWithLock(numeroConta)
                .orElseThrow(() -> new IllegalArgumentException("Conta não encontrada"));

        conta.debitar(valor);
        contaBancariaRepository.save(conta); // O lock é liberado após o commit
    }

    @Override
    @Transactional
    public void transferir(String contaOrigem, String contaDestino, BigDecimal valor) {
        try {
        ContaBancaria origem = contaBancariaRepository.findByNumeroContaWithLock(contaOrigem)
                .orElseThrow(() -> new IllegalArgumentException("Conta origem não encontrada"));

        ContaBancaria destino = contaBancariaRepository.findByNumeroContaWithLock(contaDestino)
                .orElseThrow(() -> new IllegalArgumentException("Conta destino não encontrada"));

        origem.debitar(valor);
        destino.creditar(valor);

        contaBancariaRepository.save(origem);
        contaBancariaRepository.save(destino);
        } catch (OptimisticLockException ex) {
            throw new IllegalArgumentException("Conta atualizada por outro usuário. Tente novamente.");
        }
    }

    @Override
    public BigDecimal consultarSaldo(String numeroConta) {
        return contaBancariaRepository.findByNumeroConta(numeroConta)
                .map(ContaBancaria::getSaldo)
                .orElseThrow(() -> new IllegalArgumentException("Conta não encontrada"));
    }

    @Override
    @Transactional
    public List<ContaBancaria> listarContas() {
        return contaBancariaRepository.findAll();
    }
}
