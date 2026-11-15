package br.ufpb.dcx.poo.biblioteca.servico;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.RecursoNaoEncontradoException;
import br.ufpb.dcx.poo.biblioteca.dominio.Emprestimo;
import br.ufpb.dcx.poo.biblioteca.dominio.Exemplar;
import br.ufpb.dcx.poo.biblioteca.dominio.Item;
import br.ufpb.dcx.poo.biblioteca.dominio.Reserva;
import br.ufpb.dcx.poo.biblioteca.dominio.Usuario;
import br.ufpb.dcx.poo.biblioteca.dominio.Validacao;

/**
 * Tudo o que uma biblioteca guarda em memória, num lugar só.
 *
 * <p>Os três serviços compartilham um registro e não guardam coleção própria. Isso tem
 * dois efeitos: o "localizar ou lançar RecursoNaoEncontradoException" existe uma vez só,
 * e ela é o lugar onde o estado inteiro mora.</p>
 *
 * <p>Os contadores de id são campos de instância: duas bibliotecas nunca dividem um id.</p>
 */
final class RegistroDaBiblioteca {

    private Map<String, Item> itens = new HashMap<>();
    private Map<String, Exemplar> exemplares = new HashMap<>();
    private Map<String, Usuario> usuarios = new HashMap<>();
    private List<Emprestimo> emprestimos = new ArrayList<>();
    private Map<String, Emprestimo> emprestimosAtivosPorTombo = new HashMap<>();
    private Map<String, Reserva> reservasAtivas = new HashMap<>();
    private int ultimoIdDeEmprestimo;
    private int ultimoIdDeReserva;

    Map<String, Item> itens() {
        return itens;
    }

    Map<String, Exemplar> exemplares() {
        return exemplares;
    }

    Map<String, Usuario> usuarios() {
        return usuarios;
    }

    List<Emprestimo> emprestimos() {
        return emprestimos;
    }

    Map<String, Emprestimo> emprestimosAtivosPorTombo() {
        return emprestimosAtivosPorTombo;
    }

    Map<String, Reserva> reservasAtivas() {
        return reservasAtivas;
    }

    Item exigirItem(String codigo) throws RecursoNaoEncontradoException {
        Validacao.exigirTexto(codigo, "codigo do item");
        Item item = itens.get(codigo);
        if (item == null) {
            throw new RecursoNaoEncontradoException("Item não encontrado: " + codigo);
        }
        return item;
    }

    Exemplar exigirExemplar(String tombo) throws RecursoNaoEncontradoException {
        Validacao.exigirTexto(tombo, "tombo");
        Exemplar exemplar = exemplares.get(tombo);
        if (exemplar == null) {
            throw new RecursoNaoEncontradoException("Exemplar não encontrado: " + tombo);
        }
        return exemplar;
    }

    Usuario exigirUsuario(String matricula) throws RecursoNaoEncontradoException {
        Validacao.exigirTexto(matricula, "matricula");
        Usuario usuario = usuarios.get(matricula);
        if (usuario == null) {
            throw new RecursoNaoEncontradoException("Usuário não encontrado: " + matricula);
        }
        return usuario;
    }

    String novoIdDeEmprestimo() {
        return "E" + (++ultimoIdDeEmprestimo);
    }

    String novoIdDeReserva() {
        return "R" + (++ultimoIdDeReserva);
    }

    /** Registra um empréstimo (novo ou carregado): lista geral, usuário e índice de ativos. */
    void registrarEmprestimo(Emprestimo emprestimo) {
        emprestimos.add(emprestimo);
        emprestimo.getUsuario().registrarEmprestimo(emprestimo);
        if (emprestimo.estaAtivo()) {
            emprestimosAtivosPorTombo.put(emprestimo.getExemplar().getTombo(), emprestimo);
        }
    }

}