package br.ufpb.dcx.poo.biblioteca.dominio;

import java.time.LocalDate;

import br.ufpb.dcx.poo.biblioteca.contrato.EmprestimoService;
import br.ufpb.dcx.poo.biblioteca.contrato.EmprestimoView;
import br.ufpb.dcx.poo.biblioteca.contrato.StatusEmprestimo;
import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.DadosInvalidosException;

/**
 * Um empréstimo: liga um exemplar a um usuário, com datas e situação próprias.
 *
 * <p>Prazo, atraso e devolução são regras daqui. A data é sempre recebida; este
 * código nunca lê o relógio do sistema.</p>
 */
public final class Emprestimo {

    private final String id;
    private final Exemplar exemplar;
    private final Usuario usuario;
    private final LocalDate dataDoEmprestimo;
    private final LocalDate dataPrevistaDeDevolucao;
    private LocalDate dataDaDevolucao;

    public Emprestimo(String id, Exemplar exemplar, Usuario usuario, LocalDate data) {
        this.id = Validacao.exigirTexto(id, "id do emprestimo");
        this.exemplar = Validacao.exigirPresente(exemplar, "exemplar");
        this.usuario = Validacao.exigirPresente(usuario, "usuario");
        this.dataDoEmprestimo = Validacao.exigirPresente(data, "data do emprestimo");
        this.dataPrevistaDeDevolucao = data.plusDays(EmprestimoService.PRAZO_EM_DIAS);
    }

    /** Recria um empréstimo já existente (usado ao carregar do arquivo). */
    public Emprestimo(String id, Exemplar exemplar, Usuario usuario, LocalDate data,
                      LocalDate dataDaDevolucao) {
        this(id, exemplar, usuario, data);
        if (dataDaDevolucao != null) {
            registrarDevolucao(dataDaDevolucao);
        }
    }

    public String getId() {
        return id;
    }

    public Exemplar getExemplar() {
        return exemplar;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public LocalDate getDataDoEmprestimo() {
        return dataDoEmprestimo;
    }

    public LocalDate getDataPrevistaDeDevolucao() {
        return dataPrevistaDeDevolucao;
    }

    public LocalDate getDataDaDevolucao() {
        return dataDaDevolucao;
    }

    public boolean estaAtivo() {
        return dataDaDevolucao == null;
    }

    /** No próprio dia previsto ainda não há atraso. */
    public boolean estaAtrasadoEm(LocalDate data) {
        return estaAtivo() && data.isAfter(dataPrevistaDeDevolucao);
    }

    /**
     * @throws DadosInvalidosException se a data for anterior à do empréstimo; nesse caso
     *         o empréstimo continua ativo
     */
    public void registrarDevolucao(LocalDate data) {
        Validacao.exigirPresente(data, "data da devolucao");
        if (data.isBefore(dataDoEmprestimo)) {
            throw new DadosInvalidosException("Devolução anterior ao empréstimo.");
        }
        if (!estaAtivo()) {
            throw new IllegalStateException("Empréstimo " + id + " já foi devolvido.");
        }
        this.dataDaDevolucao = data;
    }

    /** Status sem considerar data: ATIVO ou DEVOLVIDO. */
    public StatusEmprestimo statusSimples() {
        return estaAtivo() ? StatusEmprestimo.ATIVO : StatusEmprestimo.DEVOLVIDO;
    }

    public EmprestimoView paraView(StatusEmprestimo status) {
        return new EmprestimoView(id, exemplar.getTombo(), usuario.getMatricula(),
                dataDoEmprestimo, dataPrevistaDeDevolucao, dataDaDevolucao, status);
    }
}