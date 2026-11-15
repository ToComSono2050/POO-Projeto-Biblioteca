package br.ufpb.dcx.poo.biblioteca.dominio;

import br.ufpb.dcx.poo.biblioteca.contrato.UsuarioView;

/**
 * Pessoa cadastrada na biblioteca. A matrícula identifica e nunca muda; o nome pode se
 * repetir entre usuários diferentes.
 */
public final class Usuario {

    private final String matricula;
    private final String nome;
    private final boolean ativo = true;

    /**
     * @throws br.ufpb.dcx.poo.biblioteca.contrato.excecoes.DadosInvalidosException
     *         se matrícula ou nome forem nulos ou em branco
     */
    public Usuario(String matricula, String nome) {
        this.matricula = Validacao.exigirTexto(matricula, "matricula");
        this.nome = Validacao.exigirTexto(nome, "nome");
    }

    public String getMatricula() {
        return matricula;
    }

    public String getNome() {
        return nome;
    }

    public boolean isAtivo() {
        return ativo;
    }

    /** Ainda sem empréstimos: a Entrega 1 só cadastra e consulta. */
    public UsuarioView paraView() {
        return new UsuarioView(matricula, nome, ativo, 0);
    }
}