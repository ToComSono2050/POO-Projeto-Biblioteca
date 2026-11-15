package br.ufpb.dcx.poo.biblioteca.dominio;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import br.ufpb.dcx.poo.biblioteca.contrato.ItemView;

/**
 * Um item do acervo: um livro, um filme, um jogo, um instrumento.
 *
 * <p>O código identifica o item e nunca muda. O item se recusa a nascer sem código ou
 * sem título, conta os próprios exemplares e, como é ele quem tem exemplares e fila,
 * decide para onde vai um exemplar que acaba de ficar livre.</p>
 */
public final class Item {

    private final String codigo;
    private final String titulo;
    private final String autoria;
    private final String categoria;
    private final int ano;
    private final List<Exemplar> exemplares = new ArrayList<>();
    private final FilaDeReservas fila = new FilaDeReservas();

    /**
     * @throws br.ufpb.dcx.poo.biblioteca.contrato.excecoes.DadosInvalidosException
     *         se código ou título forem nulos ou em branco
     */
    public Item(String codigo, String titulo, String autoria, String categoria, int ano) {
        this.codigo = Validacao.exigirTexto(codigo, "codigo");
        this.titulo = Validacao.exigirTexto(titulo, "titulo");
        this.autoria = autoria;
        this.categoria = categoria;
        this.ano = ano;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getAutoria() {
        return autoria;
    }

    public String getCategoria() {
        return categoria;
    }

    public int getAno() {
        return ano;
    }

    public FilaDeReservas getFila() {
        return fila;
    }

    /** Liga o exemplar a este item. A unicidade do tombo é garantida pelo acervo. */
    public void adicionarExemplar(Exemplar exemplar) {
        exemplares.add(exemplar);
    }

    /** Devolve uma cópia: alterar a lista devolvida não altera o item. */
    public List<Exemplar> getExemplares() {
        return List.copyOf(exemplares);
    }

    public int totalDeExemplares() {
        return exemplares.size();
    }

    public int exemplaresDisponiveis() {
        int disponiveis = 0;
        for (Exemplar exemplar : exemplares) {
            if (exemplar.estaDisponivel()) {
                disponiveis++;
            }
        }
        return disponiveis;
    }

    /** A visão deste item, com os exemplares contados por ele mesmo. */
    public ItemView paraView() {
        return new ItemView(codigo, titulo, autoria, categoria, ano,
                totalDeExemplares(), exemplaresDisponiveis());
    }

    /** Verdadeiro se o título contém o trecho, ignorando maiúsculas e minúsculas. */
    public boolean tituloContem(String trecho) {
        return titulo.toLowerCase().contains(trecho.toLowerCase());
    }

    /** Verdadeiro se a categoria é igual à informada, ignorando maiúsculas e minúsculas. */
    public boolean temCategoria(String outra) {
        return categoria != null && categoria.equalsIgnoreCase(outra);
    }

    /**
     * Decide o destino de um exemplar que acabou de ficar livre (devolvido ou solto por
     * uma reserva cancelada): vai para a reserva mais antiga ainda sem exemplar; se não
     * houver, volta a ficar disponível.
     */
    public void destinarExemplarLivre(Exemplar exemplar) {
        Optional<Reserva> proxima = fila.primeiraSemExemplar();
        if (proxima.isPresent()) {
            proxima.get().separar(exemplar);
        } else {
            exemplar.devolver();
        }
    }
}