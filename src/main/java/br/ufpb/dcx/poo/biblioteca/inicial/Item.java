package br.ufpb.dcx.poo.biblioteca.inicial;

import java.util.ArrayList;
import java.util.List;

/**
 * Um item do acervo: um livro, um filme, um jogo, um instrumento.
 *
 * <p>Esta classe é ponto de partida, não modelo a seguir. Ela existe para que o
 * projeto compile e execute desde o primeiro dia. Ao longo do semestre você vai
 * decidir se ela permanece assim, se ganha invariantes, se vira uma hierarquia,
 * se delega responsabilidades ou se desaparece.</p>
 */
public class Item {

    private final String codigo;
    private String titulo;
    private String autoria;
    private String categoria;
    private int ano;
    private final List<Exemplar> exemplares = new ArrayList<>();

    public Item(String codigo, String titulo, String autoria, String categoria, int ano) {
        if (codigo == null || codigo.isBlank()) {
            throw new IllegalArgumentException("Código obrigatório");
        }
        if (titulo == null || titulo.isBlank()) {
            throw new IllegalArgumentException("Título obrigatório");
        }
        this.codigo = codigo;
        this.titulo = titulo;
        this.autoria = autoria;
        this.categoria = categoria;
        this.ano = ano;
    }

    public String getCodigo() { return codigo; }
    public String getTitulo() { return titulo; }
    public String getAutoria() { return autoria; }
    public String getCategoria() { return categoria; }
    public int getAno() { return ano; }
    public void atualizarTitulo(String novoTitulo) {
        if (novoTitulo == null || novoTitulo.isBlank()) {
            throw new IllegalArgumentException("Título obrigatório");
        }
        this.titulo = novoTitulo;
    }
    public void atualizarAutoria(String novaAutoria) {
        if (novaAutoria == null || novaAutoria.isBlank()) {
            throw new IllegalArgumentException("Autoria obrigatória");
        }
        this.autoria = novaAutoria;
    }
    public void atualizarCategoria(String novaCategoria) {
        if (novaCategoria == null || novaCategoria.isBlank()) {
            throw new IllegalArgumentException("Categoria obrigatória");
        }
        this.categoria = novaCategoria;
    }
    public void atualizarAno(int novoAno) {
        if (novoAno <= 0) {
            throw new IllegalArgumentException("Ano inválido");
        }
        this.ano = novoAno;
    }


    public List<Exemplar> getExemplares() { return exemplares; }
}
