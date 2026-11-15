package br.ufpb.dcx.poo.biblioteca.servico;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import br.ufpb.dcx.poo.biblioteca.contrato.EmprestimoService;
import br.ufpb.dcx.poo.biblioteca.contrato.EmprestimoView;
import br.ufpb.dcx.poo.biblioteca.contrato.ReservaView;
import br.ufpb.dcx.poo.biblioteca.contrato.StatusEmprestimo;
import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.ExemplarIndisponivelException;
import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.LimiteDeEmprestimosExcedidoException;
import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.OperacaoNaoPermitidaException;
import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.RecursoNaoEncontradoException;
import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.UsuarioInativoException;
import br.ufpb.dcx.poo.biblioteca.dominio.Emprestimo;
import br.ufpb.dcx.poo.biblioteca.dominio.Exemplar;
import br.ufpb.dcx.poo.biblioteca.dominio.Item;
import br.ufpb.dcx.poo.biblioteca.dominio.Reserva;
import br.ufpb.dcx.poo.biblioteca.dominio.Usuario;
import br.ufpb.dcx.poo.biblioteca.dominio.Validacao;

/**
 * Serviço de empréstimos e reservas. Orquestra: valida na ordem da especificação, e só
 * depois de passar por todas as verificações altera o estado. As regras de cada objeto
 * (atraso, quem pode levar o exemplar, para onde vai um exemplar livre) ficam no domínio.
 */
class EmprestimosEmMemoria implements EmprestimoService {

    private final RegistroDaBiblioteca registro;

    EmprestimosEmMemoria(RegistroDaBiblioteca registro) {
        this.registro = registro;
    }

    /**
     * Empresta o exemplar ao usuário, com prazo de {@code PRAZO_EM_DIAS} dias.
     *
     * <p>Ordem das verificações: parâmetros, existência, usuário inativo, exemplar
     * indisponível (ou reservado para outro), limite de empréstimos. Se o exemplar estava
     * reservado para este usuário, a reserva é atendida e sai da fila.</p>
     *
     * @return o id do empréstimo
     */
    @Override
    public String emprestar(String tombo, String matricula, LocalDate data)
            throws RecursoNaoEncontradoException, ExemplarIndisponivelException,
            UsuarioInativoException, LimiteDeEmprestimosExcedidoException {
        Validacao.exigirTexto(tombo, "tombo");
        Validacao.exigirTexto(matricula, "matricula");
        Validacao.exigirPresente(data, "data");
        Exemplar exemplar = registro.exigirExemplar(tombo);
        Usuario usuario = registro.exigirUsuario(matricula);
        if (!usuario.isAtivo()) {
            throw new UsuarioInativoException("Usuário inativo: " + matricula);
        }
        if (!exemplar.podeSerEmprestadoPara(usuario)) {
            throw new ExemplarIndisponivelException("Exemplar indisponível: " + tombo);
        }
        if (usuario.emprestimosAtivos() >= LIMITE_DE_EMPRESTIMOS) {
            throw new LimiteDeEmprestimosExcedidoException(
                    "Usuário " + matricula + " atingiu o limite de " + LIMITE_DE_EMPRESTIMOS);
        }
        // Só agora o estado muda.
        atenderReservaDoExemplar(exemplar);
        exemplar.emprestar();
        Emprestimo emprestimo = new Emprestimo(registro.novoIdDeEmprestimo(), exemplar, usuario, data);
        registro.registrarEmprestimo(emprestimo);
        return emprestimo.getId();
    }

    /**
     * Registra a devolução. O exemplar fica reservado para a reserva mais antiga ainda sem
     * exemplar; se não houver, fica disponível.
     *
     * @throws OperacaoNaoPermitidaException se o exemplar não tem empréstimo ativo
     */
    @Override
    public void devolver(String tombo, LocalDate data)
            throws RecursoNaoEncontradoException, OperacaoNaoPermitidaException {
        Validacao.exigirTexto(tombo, "tombo");
        Validacao.exigirPresente(data, "data");
        Exemplar exemplar = registro.exigirExemplar(tombo);
        Emprestimo emprestimo = registro.emprestimosAtivosPorTombo().get(tombo);
        if (emprestimo == null) {
            throw new OperacaoNaoPermitidaException("Exemplar sem empréstimo ativo: " + tombo);
        }
        emprestimo.registrarDevolucao(data); // recusa data anterior antes de mudar qualquer coisa
        registro.emprestimosAtivosPorTombo().remove(tombo);
        registro.exigirItem(exemplar.getCodigoDoItem()).destinarExemplarLivre(exemplar);
    }

    /** Todos os empréstimos do usuário, do mais recente para o mais antigo. */
    @Override
    public List<EmprestimoView> listarEmprestimosDoUsuario(String matricula)
            throws RecursoNaoEncontradoException {
        List<Emprestimo> lista = new ArrayList<>(registro.exigirUsuario(matricula).getEmprestimos());
        Collections.reverse(lista); // no empate de data, o registrado por último vem primeiro
        lista.sort(Comparator.comparing(Emprestimo::getDataDoEmprestimo).reversed());
        List<EmprestimoView> resultado = new ArrayList<>();
        for (Emprestimo emprestimo : lista) {
            resultado.add(emprestimo.paraView(emprestimo.statusSimples()));
        }
        return resultado;
    }

    /** Empréstimos não devolvidos com prazo vencido antes da data, do mais antigo ao mais novo. */
    @Override
    public List<EmprestimoView> listarAtrasados(LocalDate data) {
        Validacao.exigirPresente(data, "data");
        List<Emprestimo> atrasados = new ArrayList<>();
        for (Emprestimo emprestimo : registro.emprestimos()) {
            if (emprestimo.estaAtrasadoEm(data)) {
                atrasados.add(emprestimo);
            }
        }
        atrasados.sort(Comparator.comparing(Emprestimo::getDataPrevistaDeDevolucao));
        List<EmprestimoView> resultado = new ArrayList<>();
        for (Emprestimo emprestimo : atrasados) {
            resultado.add(emprestimo.paraView(StatusEmprestimo.ATRASADO));
        }
        return resultado;
    }

    /**
     * Entra na fila do item. Só é possível quando o item não tem exemplar disponível.
     *
     * @return o id da reserva
     */
    @Override
    public String reservar(String codigoDoItem, String matricula, LocalDate data)
            throws RecursoNaoEncontradoException, OperacaoNaoPermitidaException {
        Validacao.exigirTexto(codigoDoItem, "codigo do item");
        Validacao.exigirTexto(matricula, "matricula");
        Validacao.exigirPresente(data, "data");
        Item item = registro.exigirItem(codigoDoItem);
        Usuario usuario = registro.exigirUsuario(matricula);
        if (!usuario.isAtivo()) {
            throw new UsuarioInativoException("Usuário inativo: " + matricula);
        }
        if (item.exemplaresDisponiveis() > 0) {
            throw new OperacaoNaoPermitidaException(
                    "O item " + codigoDoItem + " tem exemplar disponível: empreste em vez de reservar");
        }
        if (item.getFila().temReservaDe(usuario)) {
            throw new OperacaoNaoPermitidaException(
                    "Usuário " + matricula + " já tem reserva ativa para o item " + codigoDoItem);
        }
        Reserva reserva = new Reserva(registro.novoIdDeReserva(), codigoDoItem, usuario, data);
        item.getFila().entrar(reserva);
        registro.reservasAtivas().put(reserva.getId(), reserva);
        return reserva.getId();
    }

    /** Reservas ativas do item, por ordem de chegada, com a posição na fila de 1 em diante. */
    @Override
    public List<ReservaView> listarReservas(String codigoDoItem)
            throws RecursoNaoEncontradoException {
        List<ReservaView> resultado = new ArrayList<>();
        int posicao = 1;
        for (Reserva reserva : registro.exigirItem(codigoDoItem).getFila().ativas()) {
            resultado.add(reserva.paraView(posicao++));
        }
        return resultado;
    }

    /**
     * Tira a reserva da fila. Se havia exemplar separado para ela, o exemplar passa à
     * próxima reserva sem exemplar ou, se não houver, volta a ficar disponível.
     *
     * @throws RecursoNaoEncontradoException se o id for inexistente, já atendido ou cancelado
     */
    @Override
    public void cancelarReserva(String idDaReserva) throws RecursoNaoEncontradoException {
        Validacao.exigirTexto(idDaReserva, "id da reserva");
        Reserva reserva = registro.reservasAtivas().get(idDaReserva);
        if (reserva == null) {
            throw new RecursoNaoEncontradoException("Reserva não encontrada: " + idDaReserva);
        }
        Item item = registro.exigirItem(reserva.getCodigoDoItem());
        Exemplar solto = reserva.liberarExemplar();
        item.getFila().remover(reserva);
        registro.reservasAtivas().remove(idDaReserva);
        if (solto != null) {
            item.destinarExemplarLivre(solto);
        }
    }

    /** Se o exemplar estava guardado para uma reserva, ela foi atendida: sai da fila. */
    private void atenderReservaDoExemplar(Exemplar exemplar) throws RecursoNaoEncontradoException {
        Reserva reserva = exemplar.getReservaDona();
        if (reserva == null) {
            return;
        }
        reserva.liberarExemplar();
        registro.exigirItem(reserva.getCodigoDoItem()).getFila().remover(reserva);
        registro.reservasAtivas().remove(reserva.getId());
    }
}