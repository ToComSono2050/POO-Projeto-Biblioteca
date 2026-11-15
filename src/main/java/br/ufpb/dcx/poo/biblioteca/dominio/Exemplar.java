package br.ufpb.dcx.poo.biblioteca.dominio;

import br.ufpb.dcx.poo.biblioteca.contrato.ExemplarView;
import br.ufpb.dcx.poo.biblioteca.contrato.StatusExemplar;

/**
 * A cópia física de um item. O que se empresta é o exemplar, não o item.
 *
 * <p>Tombo e código do item nunca mudam; o exemplar nasce
 * {@link StatusExemplar#DISPONIVEL}.</p>
 */
public final class Exemplar {

    private final String tombo;
    private final String codigoDoItem;
    private final StatusExemplar status;

    /**
     * @throws br.ufpb.dcx.poo.biblioteca.contrato.excecoes.DadosInvalidosException
     *         se o tombo ou o código do item forem nulos ou em branco
     */
    public Exemplar(String tombo, String codigoDoItem) {
        this.tombo = Validacao.exigirTexto(tombo, "tombo");
        this.codigoDoItem = Validacao.exigirTexto(codigoDoItem, "codigo do item");
        this.status = StatusExemplar.DISPONIVEL;
    }

    public String getTombo() {
        return tombo;
    }

    public String getCodigoDoItem() {
        return codigoDoItem;
    }

    public StatusExemplar getStatus() {
        return status;
    }

    public boolean estaDisponivel() {
        return status == StatusExemplar.DISPONIVEL;
    }

    public ExemplarView paraView() {
        return new ExemplarView(tombo, codigoDoItem, status);
    }
}