package br.ufpb.dcx.poo.biblioteca;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import br.ufpb.dcx.poo.biblioteca.contrato.Biblioteca;
import br.ufpb.dcx.poo.biblioteca.contrato.ItemView;
import br.ufpb.dcx.poo.biblioteca.contrato.StatusExemplar;
import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.BibliotecaException;
import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.DadosInvalidosException;
import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.OperacaoNaoPermitidaException;
import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.RecursoNaoEncontradoException;

/** buscarPorCategoria, baixarExemplar, desativar e reativar usuário. */
class AcervoEUsuariosEntrega2Test {

    private static final LocalDate DIA = LocalDate.of(2026, 11, 2);

    private Biblioteca biblioteca;

    @BeforeEach
    void criarCenario() throws BibliotecaException {
        biblioteca = Fabrica.novaBiblioteca();
        biblioteca.acervo().cadastrarItem("L1", "Refatoração", "Fowler", "livro", 2004);
        biblioteca.acervo().cadastrarItem("L2", "código limpo", "Martin", "Livro", 2009);
        biblioteca.acervo().cadastrarItem("J1", "Carcassonne", "Wrede", "jogo", 2000);
        biblioteca.acervo().cadastrarItem("S1", "Sem categoria", "Alguém", null, 2001);
        biblioteca.acervo().adicionarExemplar("L1", "T-1");
        biblioteca.acervo().adicionarExemplar("L1", "T-2");
        biblioteca.usuarios().cadastrarUsuario("ana", "Ana");
    }

    // ----------------------------------------------------- buscarPorCategoria

    @Test
    @DisplayName("buscar por categoria ignora maiúsculas e ordena por título ignorando maiúsculas")
    void buscarPorCategoria() {
        assertEquals(2, biblioteca.acervo().buscarPorCategoria("LIVRO").size());
        assertEquals("código limpo", biblioteca.acervo().buscarPorCategoria("livro").get(0).titulo());
        assertEquals("Refatoração", biblioteca.acervo().buscarPorCategoria("livro").get(1).titulo());
    }

    @Test
    @DisplayName("categoria é igualdade, não trecho")
    void categoriaNaoEhTrecho() {
        assertTrue(biblioteca.acervo().buscarPorCategoria("liv").isEmpty());
    }

    @Test
    @DisplayName("busca por categoria sem resultado devolve lista vazia")
    void buscarPorCategoriaSemResultado() {
        assertTrue(biblioteca.acervo().buscarPorCategoria("filme").isEmpty());
    }

    @Test
    @DisplayName("categoria nula, vazia ou em branco é DadosInvalidos")
    void categoriaInvalida() {
        assertThrows(DadosInvalidosException.class, () -> biblioteca.acervo().buscarPorCategoria(null));
        assertThrows(DadosInvalidosException.class, () -> biblioteca.acervo().buscarPorCategoria(""));
        assertThrows(DadosInvalidosException.class, () -> biblioteca.acervo().buscarPorCategoria("  "));
    }

    // -------------------------------------------------------- baixarExemplar

    @Test
    @DisplayName("baixar exemplar: continua listado e no total, mas deixa de ser disponível")
    void baixarExemplar() throws BibliotecaException {
        biblioteca.acervo().baixarExemplar("T-1");

        ItemView item = biblioteca.acervo().buscarItem("L1");
        assertEquals(2, item.totalDeExemplares());
        assertEquals(1, item.exemplaresDisponiveis());
        assertEquals(StatusExemplar.INDISPONIVEL, biblioteca.acervo().listarExemplares("L1").get(0).status());
    }

    @Test
    @DisplayName("baixarExemplar: parâmetro inválido e tombo inexistente")
    void baixarComErro() {
        assertThrows(DadosInvalidosException.class, () -> biblioteca.acervo().baixarExemplar(null));
        assertThrows(DadosInvalidosException.class, () -> biblioteca.acervo().baixarExemplar(" "));
        assertThrows(RecursoNaoEncontradoException.class, () -> biblioteca.acervo().baixarExemplar("T-99"));
    }

    @Test
    @DisplayName("exemplar emprestado não pode ser baixado")
    void baixarEmprestado() throws BibliotecaException {
        biblioteca.emprestimos().emprestar("T-1", "ana", DIA);

        assertThrows(OperacaoNaoPermitidaException.class, () -> biblioteca.acervo().baixarExemplar("T-1"));
        assertEquals(StatusExemplar.EMPRESTADO, biblioteca.acervo().listarExemplares("L1").get(0).status());
    }

    @Test
    @DisplayName("exemplar já baixado não pode ser baixado de novo")
    void baixarDuasVezes() throws BibliotecaException {
        biblioteca.acervo().baixarExemplar("T-1");

        assertThrows(OperacaoNaoPermitidaException.class, () -> biblioteca.acervo().baixarExemplar("T-1"));
    }

    // ---------------------------------------------- desativar e reativar usuário

    @Test
    @DisplayName("desativar e reativar mudam a situação na visão")
    void desativarEReativar() throws BibliotecaException {
        biblioteca.usuarios().desativarUsuario("ana");
        assertFalse(biblioteca.usuarios().buscarUsuario("ana").ativo());

        biblioteca.usuarios().reativarUsuario("ana");
        assertTrue(biblioteca.usuarios().buscarUsuario("ana").ativo());
    }

    @Test
    @DisplayName("desativar e reativar são idempotentes")
    void idempotencia() throws BibliotecaException {
        biblioteca.usuarios().desativarUsuario("ana");
        biblioteca.usuarios().desativarUsuario("ana");
        assertFalse(biblioteca.usuarios().buscarUsuario("ana").ativo());

        biblioteca.usuarios().reativarUsuario("ana");
        biblioteca.usuarios().reativarUsuario("ana");
        assertTrue(biblioteca.usuarios().buscarUsuario("ana").ativo());
    }

    @Test
    @DisplayName("usuário com empréstimo ativo não pode ser desativado")
    void desativarComEmprestimoAtivo() throws BibliotecaException {
        biblioteca.emprestimos().emprestar("T-1", "ana", DIA);

        assertThrows(OperacaoNaoPermitidaException.class, () -> biblioteca.usuarios().desativarUsuario("ana"));
        assertTrue(biblioteca.usuarios().buscarUsuario("ana").ativo());
    }

    @Test
    @DisplayName("depois de devolver tudo, o usuário pode ser desativado")
    void desativarDepoisDeDevolver() throws BibliotecaException {
        biblioteca.emprestimos().emprestar("T-1", "ana", DIA);
        biblioteca.emprestimos().devolver("T-1", DIA);

        biblioteca.usuarios().desativarUsuario("ana");

        assertFalse(biblioteca.usuarios().buscarUsuario("ana").ativo());
    }

    @Test
    @DisplayName("desativar e reativar: matrícula inválida ou inexistente")
    void desativarComErro() {
        assertThrows(DadosInvalidosException.class, () -> biblioteca.usuarios().desativarUsuario(null));
        assertThrows(DadosInvalidosException.class, () -> biblioteca.usuarios().reativarUsuario(" "));
        assertThrows(RecursoNaoEncontradoException.class, () -> biblioteca.usuarios().desativarUsuario("zzz"));
        assertThrows(RecursoNaoEncontradoException.class, () -> biblioteca.usuarios().reativarUsuario("zzz"));
    }

    @Test
    @DisplayName("emprestimosAtivos reflete só os empréstimos não devolvidos")
    void emprestimosAtivosDoUsuario() throws BibliotecaException {
        biblioteca.emprestimos().emprestar("T-1", "ana", DIA);
        biblioteca.emprestimos().emprestar("T-2", "ana", DIA);
        assertEquals(2, biblioteca.usuarios().buscarUsuario("ana").emprestimosAtivos());

        biblioteca.emprestimos().devolver("T-1", DIA);

        assertEquals(1, biblioteca.usuarios().buscarUsuario("ana").emprestimosAtivos());
        assertEquals(1, biblioteca.usuarios().listarUsuarios().get(0).emprestimosAtivos());
    }
}