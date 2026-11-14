package br.ufpb.dcx.poo.biblioteca.servico;

import java.util.ArrayList;
import java.util.List;

import br.ufpb.dcx.poo.biblioteca.contrato.UsuarioService;
import br.ufpb.dcx.poo.biblioteca.contrato.UsuarioView;
import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.OperacaoNaoPermitidaException;
import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.RecursoDuplicadoException;
import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.RecursoNaoEncontradoException;
import br.ufpb.dcx.poo.biblioteca.dominio.Usuario;

/** Serviço de usuários: {@code Map<String, Usuario>} por matrícula, no registro. */
class UsuariosEmMemoria implements UsuarioService {

    private final RegistroDaBiblioteca registro;

    UsuariosEmMemoria(RegistroDaBiblioteca registro) {
        this.registro = registro;
    }

    /**
     * Cadastra um usuário ativo e sem empréstimos.
     *
     * @throws br.ufpb.dcx.poo.biblioteca.contrato.excecoes.DadosInvalidosException
     *         se matrícula ou nome forem nulos ou em branco
     * @throws RecursoDuplicadoException se a matrícula já existir
     */
    @Override
    public void cadastrarUsuario(String matricula, String nome)
            throws RecursoDuplicadoException {
        Usuario usuario = new Usuario(matricula, nome); // valida primeiro
        if (registro.usuarios().containsKey(matricula)) {
            throw new RecursoDuplicadoException("Já existe usuário com a matrícula " + matricula);
        }
        registro.usuarios().put(matricula, usuario);
    }

    @Override
    public UsuarioView buscarUsuario(String matricula) throws RecursoNaoEncontradoException {
        return registro.exigirUsuario(matricula).paraView();
    }

    /** Todos os usuários, ordenados por nome sem distinguir maiúsculas. Nunca devolve null. */
    @Override
    public List<UsuarioView> listarUsuarios() {
        List<UsuarioView> resultado = new ArrayList<>();
        for (Usuario usuario : registro.usuarios().values()) {
            resultado.add(usuario.paraView());
        }
        resultado.sort((a, b) -> a.nome().compareToIgnoreCase(b.nome()));
        return resultado;
    }

    @Override
    public void desativarUsuario(String matricula)
            throws RecursoNaoEncontradoException, OperacaoNaoPermitidaException {
        throw new UnsupportedOperationException("Entrega 2: implementar desativarUsuario");
    }

    @Override
    public void reativarUsuario(String matricula) throws RecursoNaoEncontradoException {
        throw new UnsupportedOperationException("Entrega 2: implementar reativarUsuario");
    }

}