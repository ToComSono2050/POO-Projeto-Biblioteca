package br.ufpb.dcx.poo.biblioteca.servico;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

import br.ufpb.dcx.poo.biblioteca.contrato.AcervoService;
import br.ufpb.dcx.poo.biblioteca.contrato.ExemplarView;
import br.ufpb.dcx.poo.biblioteca.contrato.ItemView;
import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.OperacaoNaoPermitidaException;
import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.RecursoDuplicadoException;
import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.RecursoNaoEncontradoException;
import br.ufpb.dcx.poo.biblioteca.dominio.Exemplar;
import br.ufpb.dcx.poo.biblioteca.dominio.Item;
import br.ufpb.dcx.poo.biblioteca.dominio.Validacao;

/**
 * Serviço de acervo. Itens ficam em {@code Map<String, Item>} por código e exemplares em
 * {@code Map<String, Exemplar>} por tombo (únicos no acervo inteiro), ambos no registro.
 */
class AcervoEmMemoria implements AcervoService {

    private final RegistroDaBiblioteca registro;

    AcervoEmMemoria(RegistroDaBiblioteca registro) {
        this.registro = registro;
    }

    /**
     * Cadastra um item. Se qualquer verificação falhar, o acervo fica como estava.
     *
     * @throws br.ufpb.dcx.poo.biblioteca.contrato.excecoes.DadosInvalidosException
     *         se código ou título forem nulos ou em branco
     * @throws RecursoDuplicadoException se já existir item com o código
     */
    @Override
    public void cadastrarItem(String codigo, String titulo, String autoria,
                              String categoria, int ano) throws RecursoDuplicadoException {
        Item item = new Item(codigo, titulo, autoria, categoria, ano); // valida primeiro
        if (registro.itens().containsKey(codigo)) {
            throw new RecursoDuplicadoException("Já existe item com o código " + codigo);
        }
        registro.itens().put(codigo, item);
    }

    @Override
    public ItemView buscarItem(String codigo) throws RecursoNaoEncontradoException {
        return registro.exigirItem(codigo).paraView();
    }

    /** Todos os itens, ordenados por título sem distinguir maiúsculas. Nunca devolve null. */
    @Override
    public List<ItemView> listarItens() {
        return filtrar(item -> true);
    }

    /** Itens cujo título contém o trecho, sem distinguir maiúsculas. */
    @Override
    public List<ItemView> buscarPorTitulo(String trecho) {
        Validacao.exigirTexto(trecho, "trecho");
        return filtrar(item -> item.tituloContem(trecho));
    }

    @Override
    public List<ItemView> buscarPorCategoria(String categoria) {
        throw new UnsupportedOperationException("Entrega 2: implementar buscarPorCategoria");
    }

    /**
     * Adiciona um exemplar {@code DISPONIVEL}. O tombo é único no acervo inteiro.
     *
     * @throws br.ufpb.dcx.poo.biblioteca.contrato.excecoes.DadosInvalidosException
     *         se o tombo for nulo ou em branco
     * @throws RecursoNaoEncontradoException se o item não existir
     * @throws RecursoDuplicadoException se o tombo já estiver em uso em qualquer item
     */
    @Override
    public void adicionarExemplar(String codigoDoItem, String tombo)
            throws RecursoNaoEncontradoException, RecursoDuplicadoException {
        Validacao.exigirTexto(tombo, "tombo");
        Item item = registro.exigirItem(codigoDoItem);
        if (registro.exemplares().containsKey(tombo)) {
            throw new RecursoDuplicadoException("Já existe exemplar com o tombo " + tombo);
        }
        Exemplar exemplar = new Exemplar(tombo, item.getCodigo());
        item.adicionarExemplar(exemplar);
        registro.exemplares().put(tombo, exemplar);
    }

    /** Exemplares do item, ordenados por tombo. */
    @Override
    public List<ExemplarView> listarExemplares(String codigoDoItem)
            throws RecursoNaoEncontradoException {
        List<Exemplar> ordenados = new ArrayList<>(registro.exigirItem(codigoDoItem).getExemplares());
        ordenados.sort(Comparator.comparing(Exemplar::getTombo));
        List<ExemplarView> resultado = new ArrayList<>();
        for (Exemplar exemplar : ordenados) {
            resultado.add(exemplar.paraView());
        }
        return resultado;
    }

    @Override
    public void baixarExemplar(String tombo)
            throws RecursoNaoEncontradoException, OperacaoNaoPermitidaException {
        throw new UnsupportedOperationException("Entrega 2: implementar baixarExemplar");
    }

    private List<ItemView> filtrar(Predicate<Item> criterio) {
        List<Item> escolhidos = new ArrayList<>();
        for (Item item : registro.itens().values()) {
            if (criterio.test(item)) {
                escolhidos.add(item);
            }
        }
        escolhidos.sort((a, b) -> a.getTitulo().compareToIgnoreCase(b.getTitulo()));
        List<ItemView> resultado = new ArrayList<>();
        for (Item item : escolhidos) {
            resultado.add(item.paraView());
        }
        return resultado;
    }

}