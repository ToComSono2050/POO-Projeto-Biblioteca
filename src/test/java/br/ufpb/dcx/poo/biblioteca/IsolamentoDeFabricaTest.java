package br.ufpb.dcx.poo.biblioteca;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import br.ufpb.dcx.poo.biblioteca.contrato.Biblioteca;
import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.BibliotecaException;

/** Nenhum estado, nem o de empréstimos e reservas, é compartilhado entre bibliotecas. */
class IsolamentoDaFabricaTest {

    @Test
    @DisplayName("itens, empréstimos, reservas e ids de uma biblioteca não aparecem em outra")
    void nadaVazaEntreBibliotecas() throws BibliotecaException {
        Biblioteca a = Fabrica.novaBiblioteca();
        Biblioteca b = Fabrica.novaBiblioteca();
        LocalDate dia = LocalDate.of(2026, 11, 2);

        a.acervo().cadastrarItem("L1", "Java Efetivo", "Bloch", "livro", 2019);
        a.acervo().adicionarExemplar("L1", "T-001");
        a.usuarios().cadastrarUsuario("1", "Ana");
        String idDeA = a.emprestimos().emprestar("T-001", "1", dia);

        assertTrue(b.acervo().listarItens().isEmpty());
        assertTrue(b.usuarios().listarUsuarios().isEmpty());
        b.acervo().cadastrarItem("L1", "Java Efetivo", "Bloch", "livro", 2019);
        b.acervo().adicionarExemplar("L1", "T-001");
        b.usuarios().cadastrarUsuario("1", "Ana");
        assertEquals(idDeA, b.emprestimos().emprestar("T-001", "1", dia)); // contador próprio: mesmo primeiro id
        assertEquals(1, b.acervo().buscarItem("L1").totalDeExemplares());
    }
}