package br.ufpb.dcx.poo.biblioteca;

import br.ufpb.dcx.poo.biblioteca.contrato.Biblioteca;
import br.ufpb.dcx.poo.biblioteca.servico.BibliotecaDaEquipe;

/**
 * Ponto de entrada do sistema.
 *
 * <p><strong>O nome da classe, o pacote e a assinatura do método são fixos.</strong>
 * É por aqui que os testes de correção obtêm a implementação da sua equipe.</p>
 */
public final class Fabrica {

    private Fabrica() {
    }

    /**
     * Devolve uma biblioteca nova, vazia e independente.
     *
     * <p>Não carrega nada do disco: quem carrega é quem chama {@code carregar()}.
     * Não há estado {@code static} mutável em nenhuma classe do projeto.</p>
     */
    public static Biblioteca novaBiblioteca() {
        return new BibliotecaDaEquipe();
    }
}