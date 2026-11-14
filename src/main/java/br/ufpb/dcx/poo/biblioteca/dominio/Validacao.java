package br.ufpb.dcx.poo.biblioteca.dominio;

import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.DadosInvalidosException;

/** Validações de entrada compartilhadas pelo domínio e pelos serviços. até o momento, ok. */


public final class Validacao {

    private Validacao() {
    }

    /**
     * Garante que o texto não é nulo, vazio nem só espaços.
     *
     * @return o próprio valor, para uso em atribuições
     * @throws DadosInvalidosException se o valor for inválido
     */
    public static String exigirTexto(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new DadosInvalidosException(campo + " é obrigatório");
        }
        return valor;
    }

    /**
     * Garante que o objeto (uma data, por exemplo) não é nulo.
     *
     * @throws DadosInvalidosException se o valor for nulo
     */
    public static <T> T exigirPresente(T valor, String campo) {
        if (valor == null) {
            throw new DadosInvalidosException(campo + " é obrigatório");
        }
        return valor;
    }
}