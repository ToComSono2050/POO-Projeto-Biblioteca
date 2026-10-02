package br.ufpb.dcx.poo.biblioteca.inicial;

import br.ufpb.dcx.poo.biblioteca.contrato.StatusExemplar;

/**
 * A cópia física de um item. O que se empresta é o exemplar, não o item.
 *
 * <p>Ponto de partida, como {@link Item}.</p>
 */
public class Exemplar {

    private final String tombo;
    private final Item item;
    private StatusExemplar status;

    public Exemplar(String tombo, Item item) {
        this.tombo = tombo;
        this.item = item;
        this.status = StatusExemplar.DISPONIVEL;
    }

    public String getTombo() { return tombo; }
    public Item getItem() { return item; }
    public StatusExemplar getStatus() { return status; }

    public void emprestar() {
        if (status != StatusExemplar.DISPONIVEL) {
            throw new IllegalStateException("Exemplar não está disponível para empréstimo");
        }
        this.status = StatusExemplar.EMPRESTADO;
    }

    public void devolver() {
        if (status != StatusExemplar.EMPRESTADO) {
            throw new IllegalStateException("Exemplar não está emprestado");
        }
        this.status = StatusExemplar.DISPONIVEL;
    }

    public void reservar() {
        if (status != StatusExemplar.DISPONIVEL) {
            throw new IllegalStateException("Exemplar não pode ser reservado");
        }
        this.status = StatusExemplar.RESERVADO;
    }

    public void cancelarReserva() {
        if (status != StatusExemplar.RESERVADO) {
            throw new IllegalStateException("Exemplar não está reservado");
        }
        this.status = StatusExemplar.DISPONIVEL;
    }

    public void marcarComoIndisponivel() {
        this.status = StatusExemplar.INDISPONIVEL;
    }
}
