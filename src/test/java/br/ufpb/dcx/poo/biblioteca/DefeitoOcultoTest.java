package br.ufpb.dcx.poo.biblioteca;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import br.ufpb.dcx.poo.biblioteca.contrato.AcervoService;
import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.BibliotecaException;
import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.RecursoDuplicadoException;

/**
 * Regressão do defeito oculto do código inicial: o código do item era comparado com
 * {@code ==} (referência) em vez de {@code equals} (conteúdo). Passa pela Fábrica, como
 * qualquer usuário do sistema.
 */
class DefeitoOcultoTest {

    @Test
    @DisplayName("não permite cadastro com código duplicado, mesmo vindo de outra referência")
    void naoPermiteCadastroComCodigoDuplicado() throws BibliotecaException {
        AcervoService acervo = Fabrica.novaBiblioteca().acervo();
        acervo.cadastrarItem("123", "Livro A", "Autor A", "Categoria A", 2020);

        assertThrows(RecursoDuplicadoException.class,
                () -> acervo.cadastrarItem(new String("123"), "Livro B", "Autor B", "Categoria B", 2021));
        assertEquals(1, acervo.listarItens().size());
    }

    @Test
    @DisplayName("buscar item encontra o código mesmo vindo de outra referência")
    void buscarComOutraReferencia() throws BibliotecaException {
        AcervoService acervo = Fabrica.novaBiblioteca().acervo();
        acervo.cadastrarItem("123", "Livro A", "Autor A", "Categoria A", 2020);

        assertEquals("Livro A", acervo.buscarItem(new String("123")).titulo());
    }
}