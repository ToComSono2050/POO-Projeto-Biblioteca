package br.ufpb.dcx.poo.biblioteca.servico;

import br.ufpb.dcx.poo.biblioteca.contrato.AcervoService;
import br.ufpb.dcx.poo.biblioteca.contrato.Biblioteca;
import br.ufpb.dcx.poo.biblioteca.contrato.EmprestimoService;
import br.ufpb.dcx.poo.biblioteca.contrato.RelatorioService;
import br.ufpb.dcx.poo.biblioteca.contrato.UsuarioService;
import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.PersistenciaException;

/**
 * A Facade do sistema: a porta única por onde o resto do mundo fala com o domínio.
 * Quem está fora só enxerga as interfaces do contrato e as visões.
 */
public class BibliotecaDaEquipe implements Biblioteca {

    private final RegistroDaBiblioteca registro = new RegistroDaBiblioteca();
    private final AcervoEmMemoria acervo = new AcervoEmMemoria(registro);
    private final UsuariosEmMemoria usuarios = new UsuariosEmMemoria(registro);
    private final EmprestimosNaoImplementados emprestimos = new EmprestimosNaoImplementados();
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

    @Override
    public void salvar() throws PersistenciaException {
        throw new UnsupportedOperationException("Entrega 2: implementar salvar");
    }

    @Override
    public void carregar() throws PersistenciaException {
        throw new UnsupportedOperationException("Entrega 2: implementar carregar");
    }
}