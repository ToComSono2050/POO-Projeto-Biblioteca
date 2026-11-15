package br.ufpb.dcx.poo.biblioteca.dominio;

import java.util.ArrayList;
import java.util.List;

import br.ufpb.dcx.poo.biblioteca.contrato.UsuarioView;
import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.OperacaoNaoPermitidaException;

/**
 * Pessoa cadastrada na biblioteca. A matrícula identifica e nunca muda; o nome pode se
 * repetir. O usuário conhece os próprios empréstimos e, por isso, sabe contar os ativos.
 */
public final class Usuario {

    private final String matricula;
    private final String nome;
    private boolean ativo;
    private final List<Emprestimo> emprestimos = new ArrayList<>();

    /** Cria um usuário ativo. */
    public Usuario(String matricula, String nome) {
        this(matricula, nome, true);
    }

    /** Recria um usuário com a situação conhecida (usado ao carregar do arquivo). */
    public Usuario(String matricula, String nome, boolean ativo) {
        this.matricula = Validacao.exigirTexto(matricula, "matricula");
        this.nome = Validacao.exigirTexto(nome, "nome");
        this.ativo = ativo;
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

    public void registrarEmprestimo(Emprestimo emprestimo) {
        emprestimos.add(emprestimo);
    }

    /** Uma cópia, na ordem em que os empréstimos foram registrados. */
    public List<Emprestimo> getEmprestimos() {
        return List.copyOf(emprestimos);
    }

    public int emprestimosAtivos() {
        int ativos = 0;
        for (Emprestimo emprestimo : emprestimos) {
            if (emprestimo.estaAtivo()) {
                ativos++;
            }
        }
        return ativos;
    }

    public boolean temEmprestimoAtivo() {
        return emprestimosAtivos() > 0;
    }

    /**
     * Desativa o usuário. Desativar quem já está inativo não muda nada.
     *
     * @throws OperacaoNaoPermitidaException se houver empréstimo ativo
     */
    public void desativar() throws OperacaoNaoPermitidaException {
        if (!ativo) {
            return;
        }
        if (temEmprestimoAtivo()) {
            throw new OperacaoNaoPermitidaException(
                    "Usuário " + matricula + " tem empréstimo ativo.");
        }
        this.ativo = false;
    }

    /** Reativa o usuário. Idempotente. */
    public void reativar() {
        this.ativo = true;
    }

    public UsuarioView paraView() {
        return new UsuarioView(matricula, nome, ativo, emprestimosAtivos());
    }
}