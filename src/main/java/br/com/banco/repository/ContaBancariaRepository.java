package br.com.banco.repository;

import br.com.banco.entity.ContaBancaria;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ContaBancariaRepository extends JpaRepository<ContaBancaria, Long> {

    Optional<ContaBancaria> findByNumeroConta(String numeroConta);

    @Query("SELECT c FROM ContaBancaria c WHERE c.titular = :titular")
    Optional<ContaBancaria> findByTitular(@Param("titular") String titular);

//    @Lock(LockModeType.PESSIMISTIC_WRITE)
//    @Query("SELECT c FROM ContaBancaria c WHERE c.numeroConta = :numeroConta")
//    Optional<ContaBancaria> findByNumeroContaWithLock(@Param("numeroConta") String numeroConta);
}