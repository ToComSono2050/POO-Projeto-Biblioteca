package br.ufpb.dcx.poo.biblioteca.persistencia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.PersistenciaException;

/** O arquivo de tabela não conhece o domínio: grava e lê linhas de texto. */
class ArquivoDeTabelaTest {

    @TempDir
    Path pasta;

    @Test
    @DisplayName("linhas gravadas voltam iguais na leitura, inclusive com ponto e vírgula e nulo")
    void idaEVolta() throws PersistenciaException {
        ArquivoDeTabela arquivo = new ArquivoDeTabela(pasta.resolve("t.txt"), 3);
        List<String[]> linhas = Arrays.asList(
                new String[] {"L1", "Título; com ponto e vírgula e \\ barra", null},
                new String[] {"L2", "", "duas\nlinhas"});

        arquivo.gravar(linhas);
        List<String[]> lidas = new ArquivoDeTabela(pasta.resolve("t.txt"), 3).ler();

        assertEquals(2, lidas.size());
        assertEquals("Título; com ponto e vírgula e \\ barra", lidas.get(0)[1]);
        assertNull(lidas.get(0)[2]);
        assertEquals("", lidas.get(1)[1]);
        assertEquals("duas\nlinhas", lidas.get(1)[2]);
    }

    @Test
    @DisplayName("arquivo ausente equivale a tabela vazia")
    void arquivoAusente() throws PersistenciaException {
        assertTrue(new ArquivoDeTabela(pasta.resolve("nao-existe.txt"), 3).ler().isEmpty());
    }

    @Test
    @DisplayName("gravar cria o diretório que não existe")
    void gravarCriaODiretorio() throws PersistenciaException {
        Path arquivo = pasta.resolve("a/b/t.txt");

        new ArquivoDeTabela(arquivo, 1).gravar(Collections.singletonList(new String[] {"x"}));

        assertTrue(Files.exists(arquivo));
    }

    @Test
    @DisplayName("linha com número de campos errado lança PersistenciaException dizendo a linha")
    void linhaMalFormada() throws IOException {
        Files.writeString(pasta.resolve("t.txt"), "a;b;c\nsó;dois\n");

        PersistenciaException erro = assertThrows(PersistenciaException.class,
                () -> new ArquivoDeTabela(pasta.resolve("t.txt"), 3).ler());

        assertTrue(erro.getMessage().contains("linha 2"));
    }

    @Test
    @DisplayName("escape inválido lança PersistenciaException")
    void escapeInvalido() throws IOException {
        Files.writeString(pasta.resolve("t.txt"), "a;\\x;c\n");

        assertThrows(PersistenciaException.class, () -> new ArquivoDeTabela(pasta.resolve("t.txt"), 3).ler());
    }

    @Test
    @DisplayName("IOException vira PersistenciaException com a causa preservada")
    void ioExceptionPreservaACausa() throws IOException {
        Path umArquivoComum = Files.writeString(pasta.resolve("arquivo"), "x");

        PersistenciaException erro = assertThrows(PersistenciaException.class,
                () -> new ArquivoDeTabela(umArquivoComum.resolve("filho.txt"), 1).gravar(Collections.singletonList(new String[] {"x"})));

        assertNotNull(erro.getCause());
        assertTrue(erro.getCause() instanceof IOException);
    }

    @Test
    @DisplayName("gravar com número de campos diferente do declarado é recusado")
    void camposDemais() {
        assertThrows(IllegalArgumentException.class,
                () -> new ArquivoDeTabela(pasta.resolve("t.txt"), 2).gravar(Collections.singletonList(new String[] {"a", "b", "c"})));
    }
}