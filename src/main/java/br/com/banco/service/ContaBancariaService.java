package br.com.banco.service;

import br.com.banco.entity.ContaBancaria;

import java.math.BigDecimal;
import java.util.List;

public interface ContaBancariaService {
    void criarConta(String titular, String numero, String email, String senha);
    void creditar(String numeroConta, BigDecimal valor);
    void debitar(String numeroConta, BigDecimal valor);
    List<ContaBancaria> listarContas();
    boolean verificarContaUsuario(String email, String numeroConta);
    ContaBancaria consultarEmail(String email);
}
