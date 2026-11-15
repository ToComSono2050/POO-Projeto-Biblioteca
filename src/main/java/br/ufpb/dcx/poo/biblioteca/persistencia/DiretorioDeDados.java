package br.ufpb.dcx.poo.biblioteca.persistencia;

import java.nio.file.Path;

/**
 * Onde ficam os arquivos: {@code dados/} no diretório de execução, ou o valor da
 * propriedade de sistema {@code biblioteca.dados}, quando definida.
 *
 * <p>Chame {@link #atual()} dentro de {@code salvar()} e {@code carregar()}, nunca no
 * construtor nem num campo {@code static}: os testes definem a propriedade depois de
 * criar a biblioteca.</p>
 */
public final class DiretorioDeDados {

    private DiretorioDeDados() {
    }

    public static Path atual() {
        return Path.of(System.getProperty("biblioteca.dados", "dados"));
    }
}