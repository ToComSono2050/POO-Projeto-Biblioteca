package br.ufpb.dcx.poo.biblioteca;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import br.ufpb.dcx.poo.biblioteca.inicial.AcervoEmMemoria;
import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.RecursoDuplicadoException;

class DefeitoOcultoTest {

    @Test
    void naoPermiteCadastroComCodigoDuplicado() throws RecursoDuplicadoException {
        AcervoEmMemoria acervo = new AcervoEmMemoria();

        // Primeiro item com código "123"
        acervo.cadastrarItem("123", "Livro A", "Autor A", "Categoria A", 2020);

        // Segundo item com mesmo conteúdo, mas nova String (objeto diferente)
        assertThrows(RecursoDuplicadoException.class, () -> {
            acervo.cadastrarItem(new String("123"), "Livro B", "Autor B", "Categoria B", 2021);
        });
    }
}
