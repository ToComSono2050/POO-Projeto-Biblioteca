package br.ufpb.dcx.poo.biblioteca.dominio;

import java.util.ArrayList;
import java.util.List;

import br.ufpb.dcx.poo.biblioteca.contrato.ItemView;

/**
 * Um item do acervo: um livro, um filme, um jogo, um instrumento.
 *
 * <p>O código identifica o item e nunca muda. O item se recusa a nascer sem código ou
 * sem título e conta os próprios exemplares. A lista interna de exemplares nunca é
 * exposta: só cópias.</p>
 */
public final class Item {

    private final String codigo;
    private final String titulo;
    private final String autoria;
    private final String categoria;
    private final int ano;
    private final List<Exemplar> exemplares = new ArrayList<>();

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
}