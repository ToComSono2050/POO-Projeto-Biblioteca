package br.ufpb.dcx.poo.biblioteca;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import br.ufpb.dcx.poo.biblioteca.contrato.Biblioteca;
import br.ufpb.dcx.poo.biblioteca.contrato.EmprestimoView;
import br.ufpb.dcx.poo.biblioteca.contrato.ReservaView;
import br.ufpb.dcx.poo.biblioteca.contrato.StatusEmprestimo;
import br.ufpb.dcx.poo.biblioteca.contrato.StatusExemplar;
import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.BibliotecaException;
import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.ExemplarIndisponivelException;
import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.PersistenciaException;

/** salvar e carregar pelo contrato, sempre em pasta temporária. */
class PersistenciaDaBibliotecaTest {

    private static final LocalDate DIA = LocalDate.of(2026, 11, 2);

    @TempDir
    Path pasta;

    @BeforeEach
    void apontarOsDadosParaAPastaTemporaria() {
        System.setProperty("biblioteca.dados", pasta.toString());
    }

    @AfterEach
    void desfazerAPropriedade() {
        System.clearProperty("biblioteca.dados");
    }

    /** Uma biblioteca com de tudo: itens, exemplares em todos os status, empréstimos e reservas. */
    private Biblioteca bibliotecaCompleta() throws BibliotecaException {
        Biblioteca b = Fabrica.novaBiblioteca();
        b.acervo().cadastrarItem("L1", "Refatoração; edição \"2\"", "Fowler", "livro", 2004);
        b.acervo().cadastrarItem("J1", "Carcassonne", null, null, 2000);
        b.acervo().adicionarExemplar("L1", "T-1");
        b.acervo().adicionarExemplar("L1", "T-2");
        b.acervo().adicionarExemplar("L1", "T-3");
        b.acervo().adicionarExemplar("J1", "J-1");
        b.usuarios().cadastrarUsuario("ana", "Ana Souza");
        b.usuarios().cadastrarUsuario("bruno", "Bruno");
        b.usuarios().cadastrarUsuario("carla", "Carla");
        b.usuarios().desativarUsuario("carla");
        b.acervo().baixarExemplar("T-3");                                  // INDISPONIVEL
        b.emprestimos().emprestar("T-1", "ana", DIA);                      // EMPRESTADO
        b.emprestimos().emprestar("T-2", "ana", DIA);
        b.emprestimos().devolver("T-2", DIA.plusDays(3));                  // empréstimo devolvido
        b.emprestimos().emprestar("T-2", "bruno", DIA.plusDays(4));
        b.emprestimos().reservar("L1", "ana", DIA.plusDays(5));
        b.emprestimos().devolver("T-1", DIA.plusDays(6));                  // T-1 fica RESERVADO para ana
        b.emprestimos().emprestar("J-1", "bruno", DIA);
        b.emprestimos().reservar("J1", "ana", DIA.plusDays(7));            // reserva sem exemplar
        return b;
    }

    private void conferirIguais(Biblioteca esperada, Biblioteca lida) throws BibliotecaException {
        assertEquals(esperada.acervo().listarItens(), lida.acervo().listarItens());
        for (String codigo : new String[] {"L1", "J1"}) {
            assertEquals(esperada.acervo().listarExemplares(codigo), lida.acervo().listarExemplares(codigo));
            assertEquals(esperada.emprestimos().listarReservas(codigo), lida.emprestimos().listarReservas(codigo));
        }
        assertEquals(esperada.usuarios().listarUsuarios(), lida.usuarios().listarUsuarios());
        for (String matricula : new String[] {"ana", "bruno", "carla"}) {
            assertEquals(esperada.emprestimos().listarEmprestimosDoUsuario(matricula),
                    lida.emprestimos().listarEmprestimosDoUsuario(matricula));
        }
        LocalDate longe = LocalDate.of(2027, 6, 1);
        assertEquals(esperada.emprestimos().listarAtrasados(longe), lida.emprestimos().listarAtrasados(longe));
    }

    @Test
    @DisplayName("estado salvo volta igual numa biblioteca nova")
    void estadoSalvoVoltaNumaBibliotecaNova() throws BibliotecaException {
        Biblioteca original = bibliotecaCompleta();
        original.salvar();

        Biblioteca recarregada = Fabrica.novaBiblioteca();
        recarregada.carregar();

        conferirIguais(original, recarregada);
        assertEquals("Refatoração; edição \"2\"", recarregada.acervo().buscarItem("L1").titulo());
        assertNull(recarregada.acervo().buscarItem("J1").autoria());
        assertFalse(recarregada.usuarios().buscarUsuario("carla").ativo());
    }

    @Test
    @DisplayName("depois de carregar, o exemplar reservado continua guardado para o dono da reserva")
    void exemplarReservadoSobreviveAoCarregar() throws BibliotecaException {
        bibliotecaCompleta().salvar();
        Biblioteca recarregada = Fabrica.novaBiblioteca();
        recarregada.carregar();

        assertEquals(StatusExemplar.RESERVADO, recarregada.acervo().listarExemplares("L1").get(0).status());
        recarregada.usuarios().cadastrarUsuario("diego", "Diego");
        assertThrows(ExemplarIndisponivelException.class,
                () -> recarregada.emprestimos().emprestar("T-1", "diego", DIA.plusDays(8)));
        recarregada.emprestimos().emprestar("T-1", "ana", DIA.plusDays(8));
        assertTrue(recarregada.emprestimos().listarReservas("L1").isEmpty());
    }

    @Test
    @DisplayName("depois de carregar, novos ids continuam únicos")
    void novosIdsContinuamUnicos() throws BibliotecaException {
        Biblioteca original = bibliotecaCompleta();
        Set<String> idsAntigos = new HashSet<>();
        for (String matricula : new String[] {"ana", "bruno"}) {
            for (EmprestimoView e : original.emprestimos().listarEmprestimosDoUsuario(matricula)) {
                idsAntigos.add(e.id());
            }
        }
        for (String codigo : new String[] {"L1", "J1"}) {
            for (ReservaView r : original.emprestimos().listarReservas(codigo)) {
                idsAntigos.add(r.id());
            }
        }
        original.salvar();
        Biblioteca recarregada = Fabrica.novaBiblioteca();
        recarregada.carregar();
        recarregada.acervo().adicionarExemplar("L1", "T-9");

        String novoEmprestimo = recarregada.emprestimos().emprestar("T-9", "bruno", DIA.plusDays(9));
        String novaReserva = recarregada.emprestimos().reservar("L1", "bruno", DIA.plusDays(9));

        assertFalse(idsAntigos.contains(novoEmprestimo));
        assertFalse(idsAntigos.contains(novaReserva));
        assertNotEquals(novoEmprestimo, novaReserva);
    }

    @Test
    @DisplayName("a fábrica não carrega nada do disco sozinha")
    void fabricaNaoCarregaSozinha() throws BibliotecaException {
        bibliotecaCompleta().salvar();

        Biblioteca nova = Fabrica.novaBiblioteca();

        assertTrue(nova.acervo().listarItens().isEmpty());
        assertTrue(nova.usuarios().listarUsuarios().isEmpty());
    }

    @Test
    @DisplayName("diretório ausente: carregar deixa o estado vazio, mesmo que houvesse dados")
    void diretorioAusenteDaEstadoVazio() throws BibliotecaException {
        System.setProperty("biblioteca.dados", pasta.resolve("nao/existe").toString());
        Biblioteca b = bibliotecaCompleta();

        b.carregar();

        assertTrue(b.acervo().listarItens().isEmpty());
        assertTrue(b.usuarios().listarUsuarios().isEmpty());
    }

    @Test
    @DisplayName("salvar cria o diretório e não mexe em outros arquivos, como o itens-exemplo.csv")
    void salvarNaoSobrescreveOExemplo() throws BibliotecaException, IOException {
        Path exemplo = Files.writeString(pasta.resolve("itens-exemplo.csv"), "L0;Exemplo;Autor;livro;1999\n");
        System.setProperty("biblioteca.dados", pasta.resolve("sub").toString());
        Files.createDirectories(pasta.resolve("sub"));
        Files.copy(exemplo, pasta.resolve("sub/itens-exemplo.csv"));

        bibliotecaCompleta().salvar();

        assertEquals("L0;Exemplo;Autor;livro;1999\n", Files.readString(pasta.resolve("sub/itens-exemplo.csv")));
        assertTrue(Files.exists(pasta.resolve("sub/itens.txt")));
    }

    @Test
    @DisplayName("ano inválido no arquivo: PersistenciaException com a causa, e o estado não muda")
    void anoInvalidoNoArquivo() throws BibliotecaException, IOException {
        Biblioteca b = Fabrica.novaBiblioteca();
        b.acervo().cadastrarItem("A1", "Já na memória", "X", "livro", 2020);
        Files.writeString(pasta.resolve("itens.txt"), "L1;Refatoração;Fowler;livro;dois mil\n");

        PersistenciaException erro = assertThrows(PersistenciaException.class, b::carregar);

        assertNotNull(erro.getCause());
        assertTrue(erro.getCause() instanceof NumberFormatException);
        assertEquals(1, b.acervo().listarItens().size());
        assertEquals("Já na memória", b.acervo().buscarItem("A1").titulo());
    }

    @Test
    @DisplayName("linha mal formada num arquivo posterior: nada do que já foi lido entra no estado")
    void arquivoCorrompidoNaoDeixaEstadoPelaMetade() throws BibliotecaException, IOException {
        bibliotecaCompleta().salvar();
        Files.writeString(pasta.resolve("reservas.txt"), "R1;L1;ana\n");
        Biblioteca b = Fabrica.novaBiblioteca();
        b.usuarios().cadastrarUsuario("zeca", "Zeca");

        assertThrows(PersistenciaException.class, b::carregar);

        assertTrue(b.acervo().listarItens().isEmpty());
        assertEquals(1, b.usuarios().listarUsuarios().size());
    }

    @Test
    @DisplayName("exemplar que referencia item inexistente é arquivo corrompido")
    void exemplarSemItem() throws BibliotecaException, IOException {
        Files.writeString(pasta.resolve("itens.txt"), "L1;Refatoração;Fowler;livro;2004\n");
        Files.writeString(pasta.resolve("exemplares.txt"), "T-1;L-NAO-EXISTE;DISPONIVEL\n");

        assertThrows(PersistenciaException.class, () -> Fabrica.novaBiblioteca().carregar());
    }

    @Test
    @DisplayName("empréstimo que referencia tombo inexistente é arquivo corrompido")
    void emprestimoComTomboInexistente() throws BibliotecaException, IOException {
        Files.writeString(pasta.resolve("itens.txt"), "L1;Refatoração;Fowler;livro;2004\n");
        Files.writeString(pasta.resolve("exemplares.txt"), "T-1;L1;EMPRESTADO\n");
        Files.writeString(pasta.resolve("usuarios.txt"), "ana;Ana;true\n");
        Files.writeString(pasta.resolve("emprestimos.txt"), "E1;T-99;ana;2026-11-02;\\N\n");

        PersistenciaException erro = assertThrows(PersistenciaException.class, () -> Fabrica.novaBiblioteca().carregar());

        assertTrue(erro.getMessage().contains("T-99"));
    }

    @Test
    @DisplayName("status desconhecido, data inválida e tombo repetido são arquivo corrompido")
    void outrosValoresInvalidos() throws IOException {
        Files.writeString(pasta.resolve("itens.txt"), "L1;Refatoração;Fowler;livro;2004\n");
        Files.writeString(pasta.resolve("exemplares.txt"), "T-1;L1;QUEBRADO\n");
        assertThrows(PersistenciaException.class, () -> Fabrica.novaBiblioteca().carregar());

        Files.writeString(pasta.resolve("exemplares.txt"), "T-1;L1;DISPONIVEL\nT-1;L1;DISPONIVEL\n");
        assertThrows(PersistenciaException.class, () -> Fabrica.novaBiblioteca().carregar());

        Files.writeString(pasta.resolve("exemplares.txt"), "T-1;L1;EMPRESTADO\n");
        Files.writeString(pasta.resolve("usuarios.txt"), "ana;Ana;true\n");
        Files.writeString(pasta.resolve("emprestimos.txt"), "E1;T-1;ana;ontem;\\N\n");
        assertThrows(PersistenciaException.class, () -> Fabrica.novaBiblioteca().carregar());
    }

    @Test
    @DisplayName("exemplar EMPRESTADO sem empréstimo ativo é estado inconsistente")
    void emprestadoSemEmprestimo() throws IOException {
        Files.writeString(pasta.resolve("itens.txt"), "L1;Refatoração;Fowler;livro;2004\n");
        Files.writeString(pasta.resolve("exemplares.txt"), "T-1;L1;EMPRESTADO\n");

        assertThrows(PersistenciaException.class, () -> Fabrica.novaBiblioteca().carregar());
    }

    @Test
    @DisplayName("duas bibliotecas apontadas para a mesma pasta enxergam o mesmo estado salvo")
    void duasBibliotecasMesmaPasta() throws BibliotecaException {
        Biblioteca a = bibliotecaCompleta();
        a.salvar();
        Biblioteca b = Fabrica.novaBiblioteca();
        b.carregar();
        b.emprestimos().devolver("T-2", DIA.plusDays(10));

        assertEquals(StatusEmprestimo.ATIVO, a.emprestimos().listarEmprestimosDoUsuario("bruno").get(1).status());
    }
}