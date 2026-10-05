package br.ufpb.dcx.poo.biblioteca.inicial;

import java.util.*;
import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.OperacaoNaoPermitidaException;
import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.RecursoNaoEncontradoException;

public class ExemplaresEmMemoria {
    private final Map<String, Exemplar> exemplares = new HashMap<>();
    public void cadastrarExemplar(Exemplar exemplar) throws OperacaoNaoPermitidaException {
        if (exemplares.containsKey(exemplar.getTombo())) {
            throw new OperacaoNaoPermitidaException("Tombo já existe: " + exemplar.getTombo());
        }
        exemplares.put(exemplar.getTombo(), exemplar);
    }
    public Exemplar buscarPorTombo(String tombo) throws RecursoNaoEncontradoException {
        Exemplar ex = exemplares.get(tombo);
        if (ex == null) {
            throw new RecursoNaoEncontradoException("Exemplar não encontrado: " + tombo);
        }
        return ex;
    }
    public List<Exemplar> listarTodos() {
        return List.copyOf(exemplares.values()); // protege contra vazamento
    }
        public boolean existeTombo(String tombo) {
        return exemplares.containsKey(tombo);
    }
    public void adicionarExemplarAoItem(Item item, String tombo) throws OperacaoNaoPermitidaException {
        Exemplar novo = new Exemplar(tombo, item);
        cadastrarExemplar(novo);
    }
}
