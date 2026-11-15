package br.ufpb.dcx.poo.biblioteca.dominio;

import java.time.LocalDate;

import br.ufpb.dcx.poo.biblioteca.contrato.ReservaView;

/**
 * Um usuário na fila de um item. Quando um exemplar é devolvido, a reserva mais antiga
 * que ainda não tem exemplar separado passa a ter um, e só o dono dela pode levá-lo.
 */
public final class Reserva {

    private final String id;
    private final String codigoDoItem;
    private final Usuario usuario;
    private final LocalDate dataDaReserva;
    private Exemplar exemplarSeparado;

    public Reserva(String id, String codigoDoItem, Usuario usuario, LocalDate dataDaReserva) {
        this.id = Validacao.exigirTexto(id, "id da reserva");
        this.codigoDoItem = Validacao.exigirTexto(codigoDoItem, "codigo do item");
        this.usuario = Validacao.exigirPresente(usuario, "usuario");
        this.dataDaReserva = Validacao.exigirPresente(dataDaReserva, "data da reserva");
    }

    public String getId() {
        return id;
    }

    public String getCodigoDoItem() {
        return codigoDoItem;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public LocalDate getDataDaReserva() {
        return dataDaReserva;
    }

    public Exemplar getExemplarSeparado() {
        return exemplarSeparado;
    }

    public boolean temExemplarSeparado() {
        return exemplarSeparado != null;
    }

    public boolean pertenceA(Usuario outro) {
        return usuario.getMatricula().equals(outro.getMatricula());
    }

    /** Guarda o exemplar para esta reserva: ele passa a RESERVADO e sabe de quem é. */
    public void separar(Exemplar exemplar) {
        this.exemplarSeparado = exemplar;
        exemplar.reservarPara(this);
    }

    /**
     * Solta o exemplar separado (se houver) e o devolve, sem mexer no status dele.
     *
     * @return o exemplar que estava separado, ou {@code null}
     */
    public Exemplar liberarExemplar() {
        Exemplar liberado = this.exemplarSeparado;
        this.exemplarSeparado = null;
        if (liberado != null) {
            liberado.desvincularReserva();
        }
        return liberado;
    }

    public ReservaView paraView(int posicaoNaFila) {
        return new ReservaView(id, codigoDoItem, usuario.getMatricula(), dataDaReserva,
                posicaoNaFila);
    }
}