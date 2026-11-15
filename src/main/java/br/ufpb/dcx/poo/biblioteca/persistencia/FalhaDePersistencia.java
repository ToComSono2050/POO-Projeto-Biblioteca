package br.ufpb.dcx.poo.biblioteca.persistencia;

import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.PersistenciaException;

/**
 * Cria {@link PersistenciaException} preservando a causa. O contrato só tem o construtor
 * com mensagem, então a causa entra por {@code initCause}: é ela que diz o que aconteceu.
 */
public final class FalhaDePersistencia {

    private FalhaDePersistencia() {
    }

    public static PersistenciaException de(String mensagem, Throwable causa) {
        PersistenciaException excecao = new PersistenciaException(mensagem);
        excecao.initCause(causa);
        return excecao;
    }
}