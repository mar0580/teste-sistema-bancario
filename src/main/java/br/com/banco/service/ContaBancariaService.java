package br.com.banco.service;

import br.com.banco.entity.ContaBancaria;

import java.math.BigDecimal;
import java.util.List;

public interface ContaBancariaService {
    ContaBancaria criarConta(String titular, String numero, String tipoConta);
    void creditar(String numeroConta, BigDecimal valor);
    void debitar(String numeroConta, BigDecimal valor);
    void transferir(String contaOrigem, String contaDestino, BigDecimal valor);
    BigDecimal consultarSaldo(String numeroConta);
    List<ContaBancaria> listarContas();
}
