package br.ufpb.dcx.poo.biblioteca.servico;

import java.util.HashMap;
import java.util.Map;

import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.RecursoNaoEncontradoException;
import br.ufpb.dcx.poo.biblioteca.dominio.Exemplar;
import br.ufpb.dcx.poo.biblioteca.dominio.Item;
import br.ufpb.dcx.poo.biblioteca.dominio.Usuario;
import br.ufpb.dcx.poo.biblioteca.dominio.Validacao;

/**
 * Tudo o que uma biblioteca guarda em memória, num lugar só.
 *
 * <p>Os serviços compartilham um registro e não guardam coleção própria. Assim o
 * "localizar ou lançar RecursoNaoEncontradoException" existe uma vez só.</p>
 */
final class RegistroDaBiblioteca {

    private final Map<String, Item> itens = new HashMap<>();
    private final Map<String, Exemplar> exemplares = new HashMap<>();
    private final Map<String, Usuario> usuarios = new HashMap<>();

    Map<String, Item> itens() {
        return itens;
    }

    Map<String, Exemplar> exemplares() {
        return exemplares;
    }

    Map<String, Usuario> usuarios() {
        return usuarios;
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
}