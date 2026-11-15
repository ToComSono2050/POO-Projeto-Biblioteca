package br.ufpb.dcx.poo.biblioteca.servico;

import br.ufpb.dcx.poo.biblioteca.contrato.AcervoService;
import br.ufpb.dcx.poo.biblioteca.contrato.Biblioteca;
import br.ufpb.dcx.poo.biblioteca.contrato.EmprestimoService;
import br.ufpb.dcx.poo.biblioteca.contrato.RelatorioService;
import br.ufpb.dcx.poo.biblioteca.contrato.UsuarioService;
import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.PersistenciaException;
import br.ufpb.dcx.poo.biblioteca.persistencia.DiretorioDeDados;

/**
 * A Facade do sistema: a porta única por onde o resto do mundo (os testes de correção
 * agora, a interface gráfica na Entrega 3) fala com o domínio. Quem está fora só enxerga
 * as interfaces do contrato e as visões; nunca um {@code Item}, uma coleção ou um arquivo.
 *
 * <p>Criar uma biblioteca não lê o disco: quem carrega é quem chama {@link #carregar()}.</p>
 */
public class BibliotecaDaEquipe implements Biblioteca {

    private final RegistroDaBiblioteca registro = new RegistroDaBiblioteca();
    private final AcervoEmMemoria acervo = new AcervoEmMemoria(registro);
    private final UsuariosEmMemoria usuarios = new UsuariosEmMemoria(registro);
    private final EmprestimosEmMemoria emprestimos = new EmprestimosEmMemoria(registro);
    private final RelatoriosNaoImplementados relatorios = new RelatoriosNaoImplementados();

    @Override
    public AcervoService acervo() {
        return acervo;
    }

    @Override
    public UsuarioService usuarios() {
        return usuarios;
    }

    @Override
    public EmprestimoService emprestimos() {
        return emprestimos;
    }

    @Override
    public RelatorioService relatorios() {
        return relatorios;
    }

    /** Grava o estado completo no diretório de dados (lido a cada chamada). */
    @Override
    public void salvar() throws PersistenciaException {
        new PersistenciaDaBiblioteca(registro).salvar(DiretorioDeDados.atual());
    }

    /**
     * Substitui o estado em memória pelo gravado. Diretório ou arquivos ausentes dão
     * estado vazio; arquivo corrompido lança {@link PersistenciaException} sem alterar nada.
     */
    @Override
    public void carregar() throws PersistenciaException {
        new PersistenciaDaBiblioteca(registro).carregar(DiretorioDeDados.atual());
    }
}