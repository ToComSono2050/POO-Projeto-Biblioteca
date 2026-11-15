package br.ufpb.dcx.poo.biblioteca.dominio;

import br.ufpb.dcx.poo.biblioteca.contrato.ExemplarView;
import br.ufpb.dcx.poo.biblioteca.contrato.StatusExemplar;
import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.OperacaoNaoPermitidaException;

/**
 * A cópia física de um item. O que se empresta é o exemplar, não o item.
 *
 * <p>Tombo e código do item nunca mudam. O status só muda por métodos que dizem a
 * intenção ({@link #emprestar()}, {@link #devolver()}, {@link #baixar()}) e, quando o
 * exemplar está {@link StatusExemplar#RESERVADO}, ele sabe para qual {@link Reserva}.</p>
 */
public final class Exemplar {

    private final String tombo;
    private final String codigoDoItem;
    private StatusExemplar status;
    private Reserva reservaDona;

    /** Cria um exemplar {@link StatusExemplar#DISPONIVEL}. */
    public Exemplar(String tombo, String codigoDoItem) {
        this(tombo, codigoDoItem, StatusExemplar.DISPONIVEL);
    }

    /** Recria um exemplar com um status já conhecido (usado ao carregar do arquivo). */
    public Exemplar(String tombo, String codigoDoItem, StatusExemplar status) {
        this.tombo = Validacao.exigirTexto(tombo, "tombo");
        this.codigoDoItem = Validacao.exigirTexto(codigoDoItem, "codigo do item");
        this.status = Validacao.exigirPresente(status, "status");
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

    /** A reserva para a qual este exemplar está guardado, ou {@code null}. */
    public Reserva getReservaDona() {
        return reservaDona;
    }

    public boolean estaDisponivel() {
        return status == StatusExemplar.DISPONIVEL;
    }

    /**
     * Pergunta se este exemplar pode ir para o usuário: livre, ou guardado para a reserva
     * dele. Quem tem a informação (status e reserva) é o próprio exemplar.
     */
    public boolean podeSerEmprestadoPara(Usuario usuario) {
        if (status == StatusExemplar.DISPONIVEL) {
            return true;
        }
        return status == StatusExemplar.RESERVADO
                && reservaDona != null
                && reservaDona.pertenceA(usuario);
    }

    /** Marca como emprestado. Quem chama já verificou {@link #podeSerEmprestadoPara}. */
    public void emprestar() {
        this.status = StatusExemplar.EMPRESTADO;
        this.reservaDona = null;
    }

    /** Volta a ficar livre na estante. */
    public void devolver() {
        this.status = StatusExemplar.DISPONIVEL;
        this.reservaDona = null;
    }

    /**
     * Tira o exemplar de circulação. Um exemplar reservado pode ser baixado: a reserva
     * perde o exemplar separado e volta a esperar.
     *
     * @throws OperacaoNaoPermitidaException se estiver emprestado ou já baixado
     */
    public void baixar() throws OperacaoNaoPermitidaException {
        if (status == StatusExemplar.EMPRESTADO || status == StatusExemplar.INDISPONIVEL) {
            throw new OperacaoNaoPermitidaException(
                    "Exemplar " + tombo + " não pode ser baixado: " + status);
        }
        if (reservaDona != null) {
            reservaDona.liberarExemplar();
        }
        this.status = StatusExemplar.INDISPONIVEL;
    }

    public ExemplarView paraView() {
        return new ExemplarView(tombo, codigoDoItem, status);
    }

    /** Chamado por {@link Reserva#separar(Exemplar)}. */
    void reservarPara(Reserva reserva) {
        this.status = StatusExemplar.RESERVADO;
        this.reservaDona = reserva;
    }

    /** Chamado por {@link Reserva#liberarExemplar()}. Não muda o status. */
    void desvincularReserva() {
        this.reservaDona = null;
    }
}