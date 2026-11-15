package br.ufpb.dcx.poo.biblioteca;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import br.ufpb.dcx.poo.biblioteca.contrato.Biblioteca;
import br.ufpb.dcx.poo.biblioteca.contrato.EmprestimoService;
import br.ufpb.dcx.poo.biblioteca.contrato.EmprestimoView;
import br.ufpb.dcx.poo.biblioteca.contrato.StatusEmprestimo;
import br.ufpb.dcx.poo.biblioteca.contrato.StatusExemplar;
import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.BibliotecaException;
import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.DadosInvalidosException;
import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.ExemplarIndisponivelException;
import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.LimiteDeEmprestimosExcedidoException;
import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.RecursoNaoEncontradoException;
import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.UsuarioInativoException;

/** Regras de {@code emprestar}, na ordem de verificação da especificação. */
class EmprestimoTest {

    private static final LocalDate SEGUNDA = LocalDate.of(2026, 11, 2);

    private Biblioteca biblioteca;

    @BeforeEach
    void criarBibliotecaComDoisExemplaresEDoisUsuarios() throws BibliotecaException {
        biblioteca = Fabrica.novaBiblioteca();
        biblioteca.acervo().cadastrarItem("L1", "Refatoração", "Fowler", "livro", 2004);
        biblioteca.acervo().adicionarExemplar("L1", "T-1");
        biblioteca.acervo().adicionarExemplar("L1", "T-2");
        biblioteca.usuarios().cadastrarUsuario("2026001", "Ana");
        biblioteca.usuarios().cadastrarUsuario("2026002", "Bruno");
    }

    private void conferirQueNadaMudou() throws BibliotecaException {
        assertEquals(StatusExemplar.DISPONIVEL, biblioteca.acervo().listarExemplares("L1").get(0).status());
        assertEquals(2, biblioteca.acervo().buscarItem("L1").exemplaresDisponiveis());
        assertEquals(0, biblioteca.usuarios().buscarUsuario("2026001").emprestimosAtivos());
        assertEquals(0, biblioteca.emprestimos().listarEmprestimosDoUsuario("2026001").size());
    }

    @Test
    @DisplayName("empréstimo nasce ATIVO com prazo de quatorze dias e exemplar EMPRESTADO")
    void emprestimoTemPrazoDeQuatorzeDias() throws BibliotecaException {
        String id = biblioteca.emprestimos().emprestar("T-1", "2026001", SEGUNDA);

        EmprestimoView emprestimo = biblioteca.emprestimos().listarEmprestimosDoUsuario("2026001").get(0);
        assertEquals(id, emprestimo.id());
        assertEquals("T-1", emprestimo.tombo());
        assertEquals("2026001", emprestimo.matricula());
        assertEquals(SEGUNDA, emprestimo.dataDoEmprestimo());
        assertEquals(LocalDate.of(2026, 11, 16), emprestimo.dataPrevistaDeDevolucao());
        assertEquals(SEGUNDA.plusDays(EmprestimoService.PRAZO_EM_DIAS), emprestimo.dataPrevistaDeDevolucao());
        assertNull(emprestimo.dataDaDevolucao());
        assertEquals(StatusEmprestimo.ATIVO, emprestimo.status());
    }

    @Test
    @DisplayName("emprestar muda o exemplar, o item e o usuário nas visões")
    void emprestarAtualizaAsVisoes() throws BibliotecaException {
        biblioteca.emprestimos().emprestar("T-1", "2026001", SEGUNDA);

        assertEquals(StatusExemplar.EMPRESTADO, biblioteca.acervo().listarExemplares("L1").get(0).status());
        assertEquals(StatusExemplar.DISPONIVEL, biblioteca.acervo().listarExemplares("L1").get(1).status());
        assertEquals(2, biblioteca.acervo().buscarItem("L1").totalDeExemplares());
        assertEquals(1, biblioteca.acervo().buscarItem("L1").exemplaresDisponiveis());
        assertEquals(1, biblioteca.usuarios().buscarUsuario("2026001").emprestimosAtivos());
    }

    @Test
    @DisplayName("cada empréstimo recebe um id não vazio e diferente dos outros")
    void idsDeEmprestimoSaoUnicos() throws BibliotecaException {
        String primeiro = biblioteca.emprestimos().emprestar("T-1", "2026001", SEGUNDA);
        String segundo = biblioteca.emprestimos().emprestar("T-2", "2026002", SEGUNDA);

        assertFalse(primeiro.isBlank());
        assertNotEquals(primeiro, segundo);
    }

    @Test
    @DisplayName("parâmetro nulo, vazio ou em branco é DadosInvalidos e não deixa resíduo")
    void parametroInvalido() throws BibliotecaException {
        assertThrows(DadosInvalidosException.class, () -> biblioteca.emprestimos().emprestar(null, "2026001", SEGUNDA));
        assertThrows(DadosInvalidosException.class, () -> biblioteca.emprestimos().emprestar("  ", "2026001", SEGUNDA));
        assertThrows(DadosInvalidosException.class, () -> biblioteca.emprestimos().emprestar("T-1", "", SEGUNDA));
        assertThrows(DadosInvalidosException.class, () -> biblioteca.emprestimos().emprestar("T-1", "2026001", null));
        conferirQueNadaMudou();
    }

    @Test
    @DisplayName("tombo inexistente lança RecursoNaoEncontrado")
    void tomboInexistente() throws BibliotecaException {
        assertThrows(RecursoNaoEncontradoException.class,
                () -> biblioteca.emprestimos().emprestar("T-99", "2026001", SEGUNDA));
        conferirQueNadaMudou();
    }

    @Test
    @DisplayName("matrícula inexistente lança RecursoNaoEncontrado")
    void matriculaInexistente() throws BibliotecaException {
        assertThrows(RecursoNaoEncontradoException.class,
                () -> biblioteca.emprestimos().emprestar("T-1", "0000", SEGUNDA));
        conferirQueNadaMudou();
    }

    @Test
    @DisplayName("usuário inativo não pode pegar emprestado")
    void usuarioInativo() throws BibliotecaException {
        biblioteca.usuarios().desativarUsuario("2026001");

        assertThrows(UsuarioInativoException.class,
                () -> biblioteca.emprestimos().emprestar("T-1", "2026001", SEGUNDA));
        conferirQueNadaMudou();
    }

    @Test
    @DisplayName("exemplar já emprestado não pode ser emprestado de novo")
    void exemplarEmprestadoNaoPodeSerEmprestadoDeNovo() throws BibliotecaException {
        biblioteca.emprestimos().emprestar("T-1", "2026001", SEGUNDA);

        assertThrows(ExemplarIndisponivelException.class,
                () -> biblioteca.emprestimos().emprestar("T-1", "2026002", SEGUNDA));
        assertEquals(0, biblioteca.usuarios().buscarUsuario("2026002").emprestimosAtivos());
        assertEquals(StatusExemplar.EMPRESTADO, biblioteca.acervo().listarExemplares("L1").get(0).status());
    }

    @Test
    @DisplayName("exemplar baixado (INDISPONIVEL) não pode ser emprestado")
    void exemplarBaixadoNaoPodeSerEmprestado() throws BibliotecaException {
        biblioteca.acervo().baixarExemplar("T-1");

        assertThrows(ExemplarIndisponivelException.class,
                () -> biblioteca.emprestimos().emprestar("T-1", "2026001", SEGUNDA));
    }

    @Test
    @DisplayName("o quarto empréstimo ativo do mesmo usuário é recusado pelo limite")
    void limiteDeEmprestimos() throws BibliotecaException {
        biblioteca.acervo().adicionarExemplar("L1", "T-3");
        biblioteca.acervo().adicionarExemplar("L1", "T-4");
        biblioteca.emprestimos().emprestar("T-1", "2026001", SEGUNDA);
        biblioteca.emprestimos().emprestar("T-2", "2026001", SEGUNDA);
        biblioteca.emprestimos().emprestar("T-3", "2026001", SEGUNDA);

        assertThrows(LimiteDeEmprestimosExcedidoException.class,
                () -> biblioteca.emprestimos().emprestar("T-4", "2026001", SEGUNDA));

        assertEquals(EmprestimoService.LIMITE_DE_EMPRESTIMOS, biblioteca.usuarios().buscarUsuario("2026001").emprestimosAtivos());
        assertEquals(StatusExemplar.DISPONIVEL, biblioteca.acervo().listarExemplares("L1").get(3).status());
    }

    @Test
    @DisplayName("devolver libera uma vaga no limite do usuário")
    void devolverLiberaVagaNoLimite() throws BibliotecaException {
        biblioteca.acervo().adicionarExemplar("L1", "T-3");
        biblioteca.acervo().adicionarExemplar("L1", "T-4");
        biblioteca.emprestimos().emprestar("T-1", "2026001", SEGUNDA);
        biblioteca.emprestimos().emprestar("T-2", "2026001", SEGUNDA);
        biblioteca.emprestimos().emprestar("T-3", "2026001", SEGUNDA);
        biblioteca.emprestimos().devolver("T-1", SEGUNDA);

        biblioteca.emprestimos().emprestar("T-4", "2026001", SEGUNDA);

        assertEquals(3, biblioteca.usuarios().buscarUsuario("2026001").emprestimosAtivos());
    }

    @Test
    @DisplayName("ordem: usuário inativo vence exemplar indisponível")
    void usuarioInativoVenceExemplarIndisponivel() throws BibliotecaException {
        biblioteca.emprestimos().emprestar("T-1", "2026001", SEGUNDA);
        biblioteca.usuarios().desativarUsuario("2026002");

        assertThrows(UsuarioInativoException.class,
                () -> biblioteca.emprestimos().emprestar("T-1", "2026002", SEGUNDA));
    }

    @Test
    @DisplayName("ordem: exemplar indisponível vence limite de empréstimos")
    void exemplarIndisponivelVenceLimite() throws BibliotecaException {
        biblioteca.acervo().adicionarExemplar("L1", "T-3");
        biblioteca.acervo().adicionarExemplar("L1", "T-4");
        biblioteca.emprestimos().emprestar("T-1", "2026002", SEGUNDA);
        biblioteca.emprestimos().emprestar("T-2", "2026001", SEGUNDA);
        biblioteca.emprestimos().emprestar("T-3", "2026001", SEGUNDA);
        biblioteca.emprestimos().emprestar("T-4", "2026001", SEGUNDA);

        assertThrows(ExemplarIndisponivelException.class,
                () -> biblioteca.emprestimos().emprestar("T-1", "2026001", SEGUNDA));
    }

    @Test
    @DisplayName("ordem: parâmetro inválido vence tombo inexistente")
    void parametroInvalidoVenceInexistente() {
        assertThrows(DadosInvalidosException.class,
                () -> biblioteca.emprestimos().emprestar("T-99", "2026001", null));
    }
}