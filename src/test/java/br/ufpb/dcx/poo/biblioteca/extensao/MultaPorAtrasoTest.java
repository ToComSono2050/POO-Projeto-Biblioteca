package br.ufpb.dcx.poo.biblioteca.extensao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import br.ufpb.dcx.poo.biblioteca.Fabrica;
import br.ufpb.dcx.poo.biblioteca.contrato.Biblioteca;
import br.ufpb.dcx.poo.biblioteca.contrato.EmprestimoView;
import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.BibliotecaException;
import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.DadosInvalidosException;
import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.RecursoNaoEncontradoException;

/**
 * Extensão autoral: multa por atraso, R$ 0,50 por dia. Datas sempre fixas: o empréstimo é
 * na segunda, 2 de novembro de 2026, e o prazo de 14 dias vence na segunda, 16.
 */
class MultaPorAtrasoTest {

    private static final LocalDate SEGUNDA = LocalDate.of(2026, 11, 2);
    private static final LocalDate VENCIMENTO = LocalDate.of(2026, 11, 16);

    @TempDir
    Path pasta;

    private Biblioteca biblioteca;
    private ServicoDeMultas multas;

    @BeforeEach
    void criarBibliotecaComLivrosEUsuarios() throws BibliotecaException {
        System.setProperty("biblioteca.dados", pasta.toString());
        biblioteca = Fabrica.novaBiblioteca();
        biblioteca.acervo().cadastrarItem("L1", "Refatoração", "Fowler", "livro", 2004);
        biblioteca.acervo().cadastrarItem("L2", "Código Limpo", "Martin", "livro", 2009);
        biblioteca.acervo().adicionarExemplar("L1", "T-1");
        biblioteca.acervo().adicionarExemplar("L2", "T-2");
        biblioteca.usuarios().cadastrarUsuario("ana", "Ana");
        biblioteca.usuarios().cadastrarUsuario("bruno", "Bruno");
        multas = new ServicoDeMultas(biblioteca);
    }

    @AfterEach
    void desfazerAPropriedade() {
        System.clearProperty("biblioteca.dados");
    }

    private static BigDecimal reais(String valor) {
        return new BigDecimal(valor);
    }

    @Test
    @DisplayName("no dia previsto ainda não há multa; no dia seguinte, um dia de atraso")
    void noDiaPrevistoNaoHaMulta() throws BibliotecaException {
        biblioteca.emprestimos().emprestar("T-1", "ana", SEGUNDA);

        assertEquals(reais("0.00"), multas.totalDoUsuario("ana", VENCIMENTO));
        assertTrue(multas.multasDoUsuario("ana", VENCIMENTO).isEmpty());
        assertEquals(reais("0.50"), multas.totalDoUsuario("ana", VENCIMENTO.plusDays(1)));
    }

    @Test
    @DisplayName("a multa de um livro ainda emprestado cresce R$ 0,50 por dia de atraso")
    void multaCrescePorDia() throws BibliotecaException {
        biblioteca.emprestimos().emprestar("T-1", "ana", SEGUNDA);

        assertEquals(reais("1.50"), multas.totalDoUsuario("ana", VENCIMENTO.plusDays(3)));
        assertEquals(reais("5.00"), multas.totalDoUsuario("ana", VENCIMENTO.plusDays(10)));
    }

    @Test
    @DisplayName("a multa traz o empréstimo, o tombo, os dias de atraso e o valor")
    void multaTrazOsDetalhes() throws BibliotecaException {
        String id = biblioteca.emprestimos().emprestar("T-1", "ana", SEGUNDA);

        Multa multa = multas.multasDoUsuario("ana", VENCIMENTO.plusDays(4)).get(0);

        assertEquals(id, multa.idDoEmprestimo());
        assertEquals("T-1", multa.tombo());
        assertEquals(4L, multa.diasDeAtraso());
        assertEquals(reais("2.00"), multa.valor());
    }

    @Test
    @DisplayName("livro devolvido no prazo ou no dia previsto não gera multa")
    void devolvidoNoPrazo() throws BibliotecaException {
        biblioteca.emprestimos().emprestar("T-1", "ana", SEGUNDA);
        biblioteca.emprestimos().devolver("T-1", VENCIMENTO);

        assertEquals(reais("0.00"), multas.totalDoUsuario("ana", VENCIMENTO.plusDays(30)));
    }

    @Test
    @DisplayName("livro devolvido com atraso paga pelos dias até a devolução e a multa para de crescer")
    void devolvidoComAtraso() throws BibliotecaException {
        biblioteca.emprestimos().emprestar("T-1", "ana", SEGUNDA);
        biblioteca.emprestimos().devolver("T-1", VENCIMENTO.plusDays(3));

        assertEquals(reais("1.50"), multas.totalDoUsuario("ana", VENCIMENTO.plusDays(3)));
        assertEquals(reais("1.50"), multas.totalDoUsuario("ana", VENCIMENTO.plusDays(60)));
    }

    @Test
    @DisplayName("consultar numa data antes do vencimento nunca dá multa negativa")
    void consultaAntesDoVencimento() throws BibliotecaException {
        biblioteca.emprestimos().emprestar("T-1", "ana", SEGUNDA);

        assertEquals(reais("0.00"), multas.totalDoUsuario("ana", SEGUNDA.minusDays(5)));
    }

    @Test
    @DisplayName("o total soma as multas de todos os livros do usuário")
    void totalSomaVariosLivros() throws BibliotecaException {
        biblioteca.emprestimos().emprestar("T-1", "ana", SEGUNDA);
        biblioteca.emprestimos().emprestar("T-2", "ana", SEGUNDA.plusDays(2));
        biblioteca.emprestimos().devolver("T-1", VENCIMENTO.plusDays(2));

        // T-1: 2 dias de atraso (devolvido); T-2: vence no dia 18, consulta no dia 21 = 3 dias
        assertEquals(reais("2.50"), multas.totalDoUsuario("ana", LocalDate.of(2026, 11, 21)));
        assertEquals(2, multas.multasDoUsuario("ana", LocalDate.of(2026, 11, 21)).size());
    }

    @Test
    @DisplayName("a multa de um usuário não afeta a de outro")
    void multaPorUsuario() throws BibliotecaException {
        biblioteca.emprestimos().emprestar("T-1", "ana", SEGUNDA);

        assertEquals(reais("0.00"), multas.totalDoUsuario("bruno", VENCIMENTO.plusDays(10)));
    }

    @Test
    @DisplayName("usuariosComMulta lista só quem deve, com o total de cada um")
    void usuariosComMulta() throws BibliotecaException {
        biblioteca.emprestimos().emprestar("T-1", "ana", SEGUNDA);
        biblioteca.emprestimos().emprestar("T-2", "bruno", SEGUNDA.plusDays(13));

        Map<String, BigDecimal> devedores = multas.usuariosComMulta(VENCIMENTO.plusDays(2));

        assertEquals(1, devedores.size());
        assertEquals(reais("1.00"), devedores.get("ana"));
    }

    @Test
    @DisplayName("o valor por dia pode ser configurado, e vem com duas casas decimais")
    void valorPorDiaConfiguravel() throws BibliotecaException {
        biblioteca.emprestimos().emprestar("T-1", "ana", SEGUNDA);
        ServicoDeMultas maisCara = new ServicoDeMultas(biblioteca, new BigDecimal("2"));

        assertEquals(reais("6.00"), maisCara.totalDoUsuario("ana", VENCIMENTO.plusDays(3)));
    }

    @Test
    @DisplayName("valor por dia nulo ou negativo, e biblioteca nula, são DadosInvalidos")
    void configuracaoInvalida() {
        assertThrows(DadosInvalidosException.class, () -> new ServicoDeMultas(biblioteca, null));
        assertThrows(DadosInvalidosException.class, () -> new ServicoDeMultas(biblioteca, new BigDecimal("-0.01")));
        assertThrows(DadosInvalidosException.class, () -> new ServicoDeMultas(null));
    }

    @Test
    @DisplayName("matrícula ou data inválidas são DadosInvalidos; usuário inexistente lança RecursoNaoEncontrado")
    void consultaInvalida() {
        assertThrows(DadosInvalidosException.class, () -> multas.totalDoUsuario(null, VENCIMENTO));
        assertThrows(DadosInvalidosException.class, () -> multas.totalDoUsuario(" ", VENCIMENTO));
        assertThrows(DadosInvalidosException.class, () -> multas.totalDoUsuario("ana", null));
        assertThrows(DadosInvalidosException.class, () -> multas.usuariosComMulta(null));
        assertThrows(RecursoNaoEncontradoException.class, () -> multas.totalDoUsuario("zzz", VENCIMENTO));
    }

    @Test
    @DisplayName("consultar multas não altera nada: o contrato segue igual")
    void consultaNaoAlteraOEstado() throws BibliotecaException {
        biblioteca.emprestimos().emprestar("T-1", "ana", SEGUNDA);
        LocalDate longe = VENCIMENTO.plusDays(30);
        List<EmprestimoView> atrasadosAntes = biblioteca.emprestimos().listarAtrasados(longe);
        List<EmprestimoView> emprestimosAntes = biblioteca.emprestimos().listarEmprestimosDoUsuario("ana");

        multas.totalDoUsuario("ana", longe);
        multas.usuariosComMulta(longe);

        assertEquals(atrasadosAntes, biblioteca.emprestimos().listarAtrasados(longe));
        assertEquals(emprestimosAntes, biblioteca.emprestimos().listarEmprestimosDoUsuario("ana"));
        assertEquals(1, biblioteca.usuarios().buscarUsuario("ana").emprestimosAtivos());
    }

    @Test
    @DisplayName("o prazo do contrato continua sendo de quatorze dias, com ou sem a extensão")
    void prazoDoContratoNaoMuda() throws BibliotecaException {
        biblioteca.emprestimos().emprestar("T-1", "ana", SEGUNDA);

        EmprestimoView emprestimo = biblioteca.emprestimos().listarEmprestimosDoUsuario("ana").get(0);

        assertEquals(VENCIMENTO, emprestimo.dataPrevistaDeDevolucao());
    }

    @Test
    @DisplayName("a multa sobrevive a salvar e carregar, sem a persistência saber dela")
    void multaDepoisDeCarregar() throws BibliotecaException {
        biblioteca.emprestimos().emprestar("T-1", "ana", SEGUNDA);
        biblioteca.emprestimos().emprestar("T-2", "ana", SEGUNDA);
        biblioteca.emprestimos().devolver("T-2", VENCIMENTO.plusDays(1));
        LocalDate consulta = VENCIMENTO.plusDays(5);
        BigDecimal antes = multas.totalDoUsuario("ana", consulta);
        biblioteca.salvar();

        Biblioteca recarregada = Fabrica.novaBiblioteca();
        recarregada.carregar();

        assertEquals(reais("3.00"), antes);
        assertEquals(antes, new ServicoDeMultas(recarregada).totalDoUsuario("ana", consulta));
    }
}