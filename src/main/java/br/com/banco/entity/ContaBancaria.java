package br.com.banco.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContaBancaria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String numeroConta;

    @Column(nullable = false)
    private String titular;

    @Column(nullable = false)
    private BigDecimal saldo;

    @Column(nullable = false)
    private LocalDateTime dataCriacao;

    @Version
    private Long versao; // Controle otimista (JPA incrementa automaticamente)

    @Transient
    private final Lock lock = new ReentrantLock();

    @Transient
    private String dataCriacaoFormatada;

    public void creditar(BigDecimal valor) {
        this.saldo = this.saldo.add(valor);
    }

    public void debitar(BigDecimal valor) {
        if (this.saldo.compareTo(valor) < 0) {
            throw new IllegalArgumentException("Saldo insuficiente");
        }
        this.saldo = this.saldo.subtract(valor);
    }

    public void lock() {
        this.lock.lock();
    }

    public void unlock() {
        this.lock.unlock();
    }
}