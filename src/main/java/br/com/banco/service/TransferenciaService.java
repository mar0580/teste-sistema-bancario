package br.com.banco.service;

import java.math.BigDecimal;

public interface TransferenciaService {
    void transferir(String contaOrigem, String contaDestino, BigDecimal valor) throws IllegalArgumentException;
}
