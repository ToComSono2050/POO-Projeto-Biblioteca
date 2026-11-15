package br.ufpb.dcx.poo.biblioteca.extensao;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import br.ufpb.dcx.poo.biblioteca.contrato.Biblioteca;
import br.ufpb.dcx.poo.biblioteca.contrato.EmprestimoView;
import br.ufpb.dcx.poo.biblioteca.contrato.UsuarioView;
import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.DadosInvalidosException;
import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.RecursoNaoEncontradoException;
import br.ufpb.dcx.poo.biblioteca.dominio.Validacao;

/**
 * Extensão autoral: <strong>multa por atraso</strong> no acervo de livros.
 *
 * <p>A regra: cada livro devolvido depois da data prevista, ou ainda não devolvido e já
 * vencido na data da consulta, custa {@link #VALOR_POR_DIA_PADRAO} por dia de atraso. No
 * próprio dia previsto ainda não há atraso, igual à regra do contrato.</p>
 *
 * <p>Fica <em>por fora</em> do contrato: só lê pela interface {@link Biblioteca} (a Facade)
 * e nunca muda o comportamento de {@code emprestar} ou {@code devolver}. A multa é
 * <em>calculada</em> a cada consulta, a partir das datas dos empréstimos, e nunca guardada:
 * um valor guardado à parte poderia ficar diferente dos empréstimos, e um valor calculado
 * nunca engana. Por isso a persistência não precisa saber que ela existe.</p>
 */
public final class ServicoDeMultas {

    /** R$ 0,50 por dia de atraso. */
    public static final BigDecimal VALOR_POR_DIA_PADRAO = new BigDecimal("0.50");

    private final Biblioteca biblioteca;
    private final BigDecimal valorPorDia;

    public ServicoDeMultas(Biblioteca biblioteca) {
        this(biblioteca, VALOR_POR_DIA_PADRAO);
    }

    /**
     * @param valorPorDia valor da multa por dia de atraso, não negativo
     * @throws DadosInvalidosException se a biblioteca for nula ou o valor for nulo ou negativo
     */
    public ServicoDeMultas(Biblioteca biblioteca, BigDecimal valorPorDia) {
        this.biblioteca = Validacao.exigirPresente(biblioteca, "biblioteca");
        Validacao.exigirPresente(valorPorDia, "valor por dia");
        if (valorPorDia.signum() < 0) {
            throw new DadosInvalidosException("O valor por dia não pode ser negativo.");
        }
        this.valorPorDia = valorPorDia.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * As multas do usuário na data da consulta: só os empréstimos com algum dia de atraso, na
     * mesma ordem de {@code listarEmprestimosDoUsuario} (do mais recente para o mais antigo).
     *
     * @throws DadosInvalidosException se a matrícula ou a data forem inválidas
     * @throws RecursoNaoEncontradoException se o usuário não existir
     */
    public List<Multa> multasDoUsuario(String matricula, LocalDate data)
            throws RecursoNaoEncontradoException {
        Validacao.exigirTexto(matricula, "matricula");
        Validacao.exigirPresente(data, "data");
        List<Multa> multas = new ArrayList<>();
        for (EmprestimoView emprestimo : biblioteca.emprestimos().listarEmprestimosDoUsuario(matricula)) {
            Multa multa = multaDo(emprestimo, data);
            if (multa.diasDeAtraso() > 0) {
                multas.add(multa);
            }
        }
        return multas;
    }

    /** A soma das multas do usuário na data da consulta, com duas casas decimais. */
    public BigDecimal totalDoUsuario(String matricula, LocalDate data)
            throws RecursoNaoEncontradoException {
        BigDecimal total = BigDecimal.ZERO.setScale(2);
        for (Multa multa : multasDoUsuario(matricula, data)) {
            total = total.add(multa.valor());
        }
        return total;
    }

    /**
     * Quem deve: matrícula e total das multas de cada usuário que tem alguma, na ordem de
     * {@code listarUsuarios} (por nome).
     *
     * @throws DadosInvalidosException se a data for nula
     */
    public Map<String, BigDecimal> usuariosComMulta(LocalDate data) {
        Validacao.exigirPresente(data, "data");
        Map<String, BigDecimal> devedores = new LinkedHashMap<>();
        for (UsuarioView usuario : biblioteca.usuarios().listarUsuarios()) {
            try {
                BigDecimal total = totalDoUsuario(usuario.matricula(), data);
                if (total.signum() > 0) {
                    devedores.put(usuario.matricula(), total);
                }
            } catch (RecursoNaoEncontradoException e) {
                throw new IllegalStateException(
                        "Usuário listado mas inexistente: " + usuario.matricula(), e);
            }
        }
        return devedores;
    }

    /**
     * Dias de atraso: entre a data prevista e a devolução; se ainda não foi devolvido, entre a
     * data prevista e a data da consulta. Nunca negativo.
     */
    private Multa multaDo(EmprestimoView emprestimo, LocalDate dataDaConsulta) {
        LocalDate fim = emprestimo.dataDaDevolucao() != null
                ? emprestimo.dataDaDevolucao()
                : dataDaConsulta;
        long dias = Math.max(0, ChronoUnit.DAYS.between(emprestimo.dataPrevistaDeDevolucao(), fim));
        return new Multa(emprestimo.id(), emprestimo.tombo(), dias,
                valorPorDia.multiply(BigDecimal.valueOf(dias)));
    }
}