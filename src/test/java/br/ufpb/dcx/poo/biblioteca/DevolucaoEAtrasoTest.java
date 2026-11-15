package br.ufpb.dcx.poo.biblioteca;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import br.ufpb.dcx.poo.biblioteca.contrato.Biblioteca;
import br.ufpb.dcx.poo.biblioteca.contrato.EmprestimoView;
import br.ufpb.dcx.poo.biblioteca.contrato.StatusEmprestimo;
import br.ufpb.dcx.poo.biblioteca.contrato.StatusExemplar;
import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.BibliotecaException;
import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.DadosInvalidosException;
import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.OperacaoNaoPermitidaException;
import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.RecursoNaoEncontradoException;

/** Devolução, listagens de empréstimos e atraso, sempre com datas fixas. */
class DevolucaoEAtrasoTest {

    private static final LocalDate SEGUNDA = LocalDate.of(2026, 11, 2);

    private Biblioteca biblioteca;

    @BeforeEach
    void criarBibliotecaComUmExemplarEUmUsuario() throws BibliotecaException {
        biblioteca = Fabrica.novaBiblioteca();
        biblioteca.acervo().cadastrarItem("L1", "Refatoração", "Fowler", "livro", 2004);
        biblioteca.acervo().adicionarExemplar("L1", "T-1");
        biblioteca.acervo().adicionarExemplar("L1", "T-2");
        biblioteca.usuarios().cadastrarUsuario("2026001", "Ana");
    }

    @Test
    @DisplayName("devolver marca o empréstimo DEVOLVIDO e o exemplar DISPONIVEL")
    void devolverCaminhoFeliz() throws BibliotecaException {
        biblioteca.emprestimos().emprestar("T-1", "2026001", SEGUNDA);

        biblioteca.emprestimos().devolver("T-1", SEGUNDA.plusDays(3));

        EmprestimoView emprestimo = biblioteca.emprestimos().listarEmprestimosDoUsuario("2026001").get(0);
        assertEquals(StatusEmprestimo.DEVOLVIDO, emprestimo.status());
        assertEquals(SEGUNDA.plusDays(3), emprestimo.dataDaDevolucao());
        assertEquals(StatusExemplar.DISPONIVEL, biblioteca.acervo().listarExemplares("L1").get(0).status());
        assertEquals(0, biblioteca.usuarios().buscarUsuario("2026001").emprestimosAtivos());
    }

    @Test
    @DisplayName("devolver no mesmo dia do empréstimo é permitido")
    void devolverNoMesmoDia() throws BibliotecaException {
        biblioteca.emprestimos().emprestar("T-1", "2026001", SEGUNDA);

        biblioteca.emprestimos().devolver("T-1", SEGUNDA);

        assertEquals(SEGUNDA, biblioteca.emprestimos().listarEmprestimosDoUsuario("2026001").get(0).dataDaDevolucao());
    }

    @Test
    @DisplayName("devolver com parâmetro inválido é DadosInvalidos")
    void devolverParametroInvalido() {
        assertThrows(DadosInvalidosException.class, () -> biblioteca.emprestimos().devolver(null, SEGUNDA));
        assertThrows(DadosInvalidosException.class, () -> biblioteca.emprestimos().devolver(" ", SEGUNDA));
        assertThrows(DadosInvalidosException.class, () -> biblioteca.emprestimos().devolver("T-1", null));
    }

    @Test
    @DisplayName("devolver tombo inexistente lança RecursoNaoEncontrado")
    void devolverTomboInexistente() {
        assertThrows(RecursoNaoEncontradoException.class, () -> biblioteca.emprestimos().devolver("T-99", SEGUNDA));
    }

    @Test
    @DisplayName("devolver exemplar sem empréstimo ativo lança OperacaoNaoPermitida")
    void devolverSemEmprestimoAtivo() {
        assertThrows(OperacaoNaoPermitidaException.class, () -> biblioteca.emprestimos().devolver("T-1", SEGUNDA));
    }

    @Test
    @DisplayName("devolver duas vezes: a segunda é recusada")
    void devolverDuasVezes() throws BibliotecaException {
        biblioteca.emprestimos().emprestar("T-1", "2026001", SEGUNDA);
        biblioteca.emprestimos().devolver("T-1", SEGUNDA);

        assertThrows(OperacaoNaoPermitidaException.class, () -> biblioteca.emprestimos().devolver("T-1", SEGUNDA));
    }

    @Test
    @DisplayName("data de devolução anterior ao empréstimo é recusada e o empréstimo segue ativo")
    void devolucaoAnteriorAoEmprestimo() throws BibliotecaException {
        biblioteca.emprestimos().emprestar("T-1", "2026001", SEGUNDA);

        assertThrows(DadosInvalidosException.class,
                () -> biblioteca.emprestimos().devolver("T-1", SEGUNDA.minusDays(1)));

        assertEquals(StatusEmprestimo.ATIVO, biblioteca.emprestimos().listarEmprestimosDoUsuario("2026001").get(0).status());
        assertEquals(StatusExemplar.EMPRESTADO, biblioteca.acervo().listarExemplares("L1").get(0).status());
        assertEquals(1, biblioteca.usuarios().buscarUsuario("2026001").emprestimosAtivos());
    }

    @Test
    @DisplayName("exemplar devolvido pode ser emprestado de novo")
    void exemplarDevolvidoPodeSerEmprestadoDeNovo() throws BibliotecaException {
        biblioteca.emprestimos().emprestar("T-1", "2026001", SEGUNDA);
        biblioteca.emprestimos().devolver("T-1", SEGUNDA);

        biblioteca.emprestimos().emprestar("T-1", "2026001", SEGUNDA.plusDays(1));

        assertEquals(2, biblioteca.emprestimos().listarEmprestimosDoUsuario("2026001").size());
    }

    @Test
    @DisplayName("no dia previsto ainda não há atraso; no dia seguinte há")
    void noDiaPrevistoAindaNaoHaAtraso() throws BibliotecaException {
        biblioteca.emprestimos().emprestar("T-1", "2026001", SEGUNDA);

        List<EmprestimoView> noDia = biblioteca.emprestimos().listarAtrasados(LocalDate.of(2026, 11, 16));
        List<EmprestimoView> noDiaSeguinte = biblioteca.emprestimos().listarAtrasados(LocalDate.of(2026, 11, 17));

        assertEquals(0, noDia.size());
        assertEquals(1, noDiaSeguinte.size());
        assertEquals(StatusEmprestimo.ATRASADO, noDiaSeguinte.get(0).status());
    }

    @Test
    @DisplayName("empréstimo devolvido não aparece entre os atrasados")
    void devolvidoNaoEstaAtrasado() throws BibliotecaException {
        biblioteca.emprestimos().emprestar("T-1", "2026001", SEGUNDA);
        biblioteca.emprestimos().devolver("T-1", SEGUNDA.plusDays(20));

        assertTrue(biblioteca.emprestimos().listarAtrasados(LocalDate.of(2027, 1, 1)).isEmpty());
    }

    @Test
    @DisplayName("atrasados saem do prazo mais antigo para o mais recente")
    void atrasadosOrdenadosPelaDataPrevista() throws BibliotecaException {
        biblioteca.emprestimos().emprestar("T-2", "2026001", SEGUNDA.plusDays(2));
        biblioteca.emprestimos().emprestar("T-1", "2026001", SEGUNDA);

        List<EmprestimoView> atrasados = biblioteca.emprestimos().listarAtrasados(LocalDate.of(2026, 12, 1));

        assertEquals("T-1", atrasados.get(0).tombo());
        assertEquals("T-2", atrasados.get(1).tombo());
    }

    @Test
    @DisplayName("listarAtrasados com data nula é DadosInvalidos")
    void atrasadosComDataNula() {
        assertThrows(DadosInvalidosException.class, () -> biblioteca.emprestimos().listarAtrasados(null));
    }

    @Test
    @DisplayName("empréstimos do usuário: o mais recente primeiro, e no empate o registrado por último")
    void listagemDoUsuarioOrdenada() throws BibliotecaException {
        biblioteca.emprestimos().emprestar("T-1", "2026001", SEGUNDA);
        biblioteca.emprestimos().emprestar("T-2", "2026001", SEGUNDA.plusDays(5));
        biblioteca.emprestimos().devolver("T-1", SEGUNDA.plusDays(6));
        biblioteca.emprestimos().emprestar("T-1", "2026001", SEGUNDA.plusDays(5));

        List<EmprestimoView> lista = biblioteca.emprestimos().listarEmprestimosDoUsuario("2026001");

        assertEquals(3, lista.size());
        assertEquals("T-1", lista.get(0).tombo());              // dia 5, registrado por último
        assertEquals(StatusEmprestimo.ATIVO, lista.get(0).status());
        assertEquals("T-2", lista.get(1).tombo());              // dia 5, registrado antes
        assertEquals(SEGUNDA, lista.get(2).dataDoEmprestimo()); // o mais antigo, já devolvido
        assertEquals(StatusEmprestimo.DEVOLVIDO, lista.get(2).status());
    }

    @Test
    @DisplayName("listar empréstimos de usuário inválido ou inexistente")
    void listagemDoUsuarioComErro() {
        assertThrows(DadosInvalidosException.class, () -> biblioteca.emprestimos().listarEmprestimosDoUsuario(" "));
        assertThrows(RecursoNaoEncontradoException.class, () -> biblioteca.emprestimos().listarEmprestimosDoUsuario("0000"));
    }

    @Test
    @DisplayName("usuário sem empréstimos tem lista vazia, não null")
    void usuarioSemEmprestimos() throws BibliotecaException {
        assertEquals(0, biblioteca.emprestimos().listarEmprestimosDoUsuario("2026001").size());
    }
}