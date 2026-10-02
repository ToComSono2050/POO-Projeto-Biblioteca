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

    public void setStatus(StatusExemplar status) { this.status = status; }
}
