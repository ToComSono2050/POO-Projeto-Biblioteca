package br.ufpb.dcx.poo.biblioteca;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import br.ufpb.dcx.poo.biblioteca.contrato.Biblioteca;
import br.ufpb.dcx.poo.biblioteca.contrato.ReservaView;
import br.ufpb.dcx.poo.biblioteca.contrato.StatusExemplar;
import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.BibliotecaException;
import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.DadosInvalidosException;
import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.ExemplarIndisponivelException;
import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.OperacaoNaoPermitidaException;
import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.RecursoNaoEncontradoException;
import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.UsuarioInativoException;

/** Reservas: o ciclo completo do exemplar separado e cada exceção. */
class ReservaTest {

    private static final LocalDate DIA = LocalDate.of(2026, 11, 2);

    private Biblioteca biblioteca;

    @BeforeEach
    void criarItemComUmExemplarETresUsuarios() throws BibliotecaException {
        biblioteca = Fabrica.novaBiblioteca();
        biblioteca.acervo().cadastrarItem("L1", "Refatoração", "Fowler", "livro", 2004);
        biblioteca.acervo().adicionarExemplar("L1", "T-1");
        biblioteca.usuarios().cadastrarUsuario("ana", "Ana");
        biblioteca.usuarios().cadastrarUsuario("bruno", "Bruno");
        biblioteca.usuarios().cadastrarUsuario("carla", "Carla");
    }

    private StatusExemplar statusDeT1() throws BibliotecaException {
        return biblioteca.acervo().listarExemplares("L1").get(0).status();
    }

    private List<String> filaDeL1() throws BibliotecaException {
        return biblioteca.emprestimos().listarReservas("L1").stream().map(ReservaView::matricula).toList();
    }

    @Test
    @DisplayName("ciclo completo: devolução separa o exemplar, só o dono empresta, e a fila anda")
    void cicloDeReserva() throws BibliotecaException {
        biblioteca.emprestimos().emprestar("T-1", "ana", DIA);
        assertEquals(StatusExemplar.EMPRESTADO, statusDeT1());

        String reservaDeBruno = biblioteca.emprestimos().reservar("L1", "bruno", DIA);
        biblioteca.emprestimos().reservar("L1", "carla", DIA);
        assertEquals(List.of("bruno", "carla"), filaDeL1());
        assertEquals(StatusExemplar.EMPRESTADO, statusDeT1());

        biblioteca.emprestimos().devolver("T-1", DIA.plusDays(1));
        assertEquals(StatusExemplar.RESERVADO, statusDeT1());
        assertEquals(List.of("bruno", "carla"), filaDeL1());

        assertThrows(ExemplarIndisponivelException.class,
                () -> biblioteca.emprestimos().emprestar("T-1", "carla", DIA.plusDays(1)));
        assertEquals(StatusExemplar.RESERVADO, statusDeT1());

        biblioteca.emprestimos().emprestar("T-1", "bruno", DIA.plusDays(1));
        assertEquals(StatusExemplar.EMPRESTADO, statusDeT1());
        assertEquals(List.of("carla"), filaDeL1());
        assertThrows(RecursoNaoEncontradoException.class, () -> biblioteca.emprestimos().cancelarReserva(reservaDeBruno));

        biblioteca.emprestimos().devolver("T-1", DIA.plusDays(2));
        assertEquals(StatusExemplar.RESERVADO, statusDeT1());

        String reservaDeCarla = biblioteca.emprestimos().listarReservas("L1").get(0).id();
        biblioteca.emprestimos().cancelarReserva(reservaDeCarla);
        assertEquals(StatusExemplar.DISPONIVEL, statusDeT1());
        assertTrue(filaDeL1().isEmpty());
    }

    @Test
    @DisplayName("reserva traz id único, data e posição na fila de 1 em diante")
    void reservaNaVisao() throws BibliotecaException {
        biblioteca.emprestimos().emprestar("T-1", "ana", DIA);
        String primeira = biblioteca.emprestimos().reservar("L1", "bruno", DIA.plusDays(1));
        String segunda = biblioteca.emprestimos().reservar("L1", "carla", DIA.plusDays(2));

        List<ReservaView> reservas = biblioteca.emprestimos().listarReservas("L1");

        assertFalse(primeira.isBlank());
        assertNotEquals(primeira, segunda);
        assertEquals(primeira, reservas.get(0).id());
        assertEquals(1, reservas.get(0).posicaoNaFila());
        assertEquals(2, reservas.get(1).posicaoNaFila());
        assertEquals("L1", reservas.get(0).codigoDoItem());
        assertEquals(DIA.plusDays(1), reservas.get(0).dataDaReserva());
    }

    @Test
    @DisplayName("item com exemplar disponível recusa a reserva")
    void itemComExemplarDisponivel() {
        assertThrows(OperacaoNaoPermitidaException.class, () -> biblioteca.emprestimos().reservar("L1", "ana", DIA));
    }

    @Test
    @DisplayName("item sem nenhum exemplar aceita a reserva")
    void itemSemExemplares() throws BibliotecaException {
        biblioteca.acervo().cadastrarItem("L2", "Código Limpo", "Martin", "livro", 2009);

        biblioteca.emprestimos().reservar("L2", "ana", DIA);

        assertEquals(1, biblioteca.emprestimos().listarReservas("L2").size());
    }

    @Test
    @DisplayName("o mesmo usuário não reserva duas vezes o mesmo item")
    void reservaRepetida() throws BibliotecaException {
        biblioteca.emprestimos().emprestar("T-1", "ana", DIA);
        biblioteca.emprestimos().reservar("L1", "bruno", DIA);

        assertThrows(OperacaoNaoPermitidaException.class, () -> biblioteca.emprestimos().reservar("L1", "bruno", DIA));
        assertEquals(1, biblioteca.emprestimos().listarReservas("L1").size());
    }

    @Test
    @DisplayName("usuário inativo não reserva")
    void reservaDeUsuarioInativo() throws BibliotecaException {
        biblioteca.emprestimos().emprestar("T-1", "ana", DIA);
        biblioteca.usuarios().desativarUsuario("bruno");

        assertThrows(UsuarioInativoException.class, () -> biblioteca.emprestimos().reservar("L1", "bruno", DIA));
        assertTrue(filaDeL1().isEmpty());
    }

    @Test
    @DisplayName("reservar com parâmetro inválido, item ou usuário inexistente")
    void reservarComErro() throws BibliotecaException {
        biblioteca.emprestimos().emprestar("T-1", "ana", DIA);

        assertThrows(DadosInvalidosException.class, () -> biblioteca.emprestimos().reservar(null, "bruno", DIA));
        assertThrows(DadosInvalidosException.class, () -> biblioteca.emprestimos().reservar("L1", " ", DIA));
        assertThrows(DadosInvalidosException.class, () -> biblioteca.emprestimos().reservar("L1", "bruno", null));
        assertThrows(RecursoNaoEncontradoException.class, () -> biblioteca.emprestimos().reservar("L9", "bruno", DIA));
        assertThrows(RecursoNaoEncontradoException.class, () -> biblioteca.emprestimos().reservar("L1", "zzz", DIA));
        assertTrue(filaDeL1().isEmpty());
    }

    @Test
    @DisplayName("listarReservas: código inválido e item inexistente")
    void listarReservasComErro() {
        assertThrows(DadosInvalidosException.class, () -> biblioteca.emprestimos().listarReservas(" "));
        assertThrows(RecursoNaoEncontradoException.class, () -> biblioteca.emprestimos().listarReservas("L9"));
    }

    @Test
    @DisplayName("cancelarReserva: id inválido, inexistente e já cancelada")
    void cancelarReservaComErro() throws BibliotecaException {
        biblioteca.emprestimos().emprestar("T-1", "ana", DIA);
        String id = biblioteca.emprestimos().reservar("L1", "bruno", DIA);
        biblioteca.emprestimos().cancelarReserva(id);

        assertThrows(DadosInvalidosException.class, () -> biblioteca.emprestimos().cancelarReserva(null));
        assertThrows(RecursoNaoEncontradoException.class, () -> biblioteca.emprestimos().cancelarReserva("nao-existe"));
        assertThrows(RecursoNaoEncontradoException.class, () -> biblioteca.emprestimos().cancelarReserva(id));
    }

    @Test
    @DisplayName("cancelar a reserva que tinha o exemplar o passa à próxima da fila")
    void exemplarPassaParaAProximaReserva() throws BibliotecaException {
        biblioteca.emprestimos().emprestar("T-1", "ana", DIA);
        String reservaDeBruno = biblioteca.emprestimos().reservar("L1", "bruno", DIA);
        biblioteca.emprestimos().reservar("L1", "carla", DIA);
        biblioteca.emprestimos().devolver("T-1", DIA);

        biblioteca.emprestimos().cancelarReserva(reservaDeBruno);

        assertEquals(StatusExemplar.RESERVADO, statusDeT1());
        assertThrows(ExemplarIndisponivelException.class, () -> biblioteca.emprestimos().emprestar("T-1", "bruno", DIA));
        biblioteca.emprestimos().emprestar("T-1", "carla", DIA);
        assertTrue(filaDeL1().isEmpty());
    }

    @Test
    @DisplayName("cancelar reserva sem exemplar separado não mexe no exemplar")
    void cancelarReservaSemExemplarSeparado() throws BibliotecaException {
        biblioteca.emprestimos().emprestar("T-1", "ana", DIA);
        String id = biblioteca.emprestimos().reservar("L1", "bruno", DIA);

        biblioteca.emprestimos().cancelarReserva(id);

        assertEquals(StatusExemplar.EMPRESTADO, statusDeT1());
    }

    @Test
    @DisplayName("sem reservas na fila, a devolução deixa o exemplar DISPONIVEL")
    void devolucaoSemFila() throws BibliotecaException {
        biblioteca.emprestimos().emprestar("T-1", "ana", DIA);
        biblioteca.emprestimos().devolver("T-1", DIA);

        assertEquals(StatusExemplar.DISPONIVEL, statusDeT1());
    }

    @Test
    @DisplayName("exemplar novo, adicionado a item com fila, entra DISPONIVEL e qualquer um pode levar")
    void exemplarNovoEntraDisponivel() throws BibliotecaException {
        biblioteca.emprestimos().emprestar("T-1", "ana", DIA);
        biblioteca.emprestimos().reservar("L1", "bruno", DIA);

        biblioteca.acervo().adicionarExemplar("L1", "T-2");

        assertEquals(StatusExemplar.DISPONIVEL, biblioteca.acervo().listarExemplares("L1").get(1).status());
        biblioteca.emprestimos().emprestar("T-2", "carla", DIA);
        assertEquals(List.of("bruno"), filaDeL1());
    }

    @Test
    @DisplayName("baixar um exemplar reservado: a reserva volta a esperar sem exemplar")
    void baixarExemplarReservado() throws BibliotecaException {
        biblioteca.emprestimos().emprestar("T-1", "ana", DIA);
        biblioteca.emprestimos().reservar("L1", "bruno", DIA);
        biblioteca.emprestimos().devolver("T-1", DIA);
        assertEquals(StatusExemplar.RESERVADO, statusDeT1());

        biblioteca.acervo().baixarExemplar("T-1");

        assertEquals(StatusExemplar.INDISPONIVEL, statusDeT1());
        assertEquals(List.of("bruno"), filaDeL1());
        biblioteca.acervo().adicionarExemplar("L1", "T-2");
        biblioteca.emprestimos().emprestar("T-2", "bruno", DIA);
    }

    @Test
    @DisplayName("com a fila andando, cada devolução separa o exemplar para a reserva mais antiga sem exemplar")
    void doisExemplaresDuasReservas() throws BibliotecaException {
        biblioteca.acervo().adicionarExemplar("L1", "T-2");
        biblioteca.emprestimos().emprestar("T-1", "ana", DIA);
        biblioteca.emprestimos().emprestar("T-2", "ana", DIA);
        biblioteca.emprestimos().reservar("L1", "bruno", DIA);
        biblioteca.emprestimos().reservar("L1", "carla", DIA);

        biblioteca.emprestimos().devolver("T-2", DIA);
        biblioteca.emprestimos().devolver("T-1", DIA);

        assertThrows(ExemplarIndisponivelException.class, () -> biblioteca.emprestimos().emprestar("T-2", "carla", DIA));
        biblioteca.emprestimos().emprestar("T-2", "bruno", DIA);
        biblioteca.emprestimos().emprestar("T-1", "carla", DIA);
        assertTrue(filaDeL1().isEmpty());
    }
}