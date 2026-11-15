package br.ufpb.dcx.poo.biblioteca.persistencia;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.PersistenciaException;

/**
 * Um arquivo de texto com uma linha por registro e campos separados por ponto e vírgula.
 * Não conhece o domínio: grava e lê linhas de campos de texto.
 *
 * <p>Dentro de um campo, {@code \} vira {@code \\}, {@code ;} vira {@code \;}, quebras de
 * linha viram {@code \n} e {@code \r}, e o valor nulo é escrito como {@code \N}. Assim um
 * título com ponto e vírgula volta igual.</p>
 *
 * <p>Arquivo ausente equivale a tabela vazia. Linha com número de campos diferente do
 * esperado, ou com escape inválido, é arquivo corrompido: {@link PersistenciaException}.
 * Toda {@link IOException} vira {@link PersistenciaException} com a causa preservada.</p>
 */
public final class ArquivoDeTabela {

    private static final char SEPARADOR = ';';
    private static final String NULO = "\\N";

    private final Path arquivo;
    private final int numeroDeCampos;

    public ArquivoDeTabela(Path arquivo, int numeroDeCampos) {
        this.arquivo = arquivo;
        this.numeroDeCampos = numeroDeCampos;
    }

    /** Grava as linhas, criando o diretório se preciso. O arquivo antigo só é trocado no fim. */
    public void gravar(List<String[]> linhas) throws PersistenciaException {
        Path temporario = arquivo.resolveSibling(arquivo.getFileName() + ".tmp");
        try {
            Files.createDirectories(arquivo.toAbsolutePath().getParent());
            try (BufferedWriter saida = Files.newBufferedWriter(temporario, StandardCharsets.UTF_8)) {
                for (String[] campos : linhas) {
                    saida.write(montar(campos));
                    saida.newLine();
                }
            }
            Files.move(temporario, arquivo, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw FalhaDePersistencia.de("Não foi possível gravar " + arquivo, e);
        }
    }

    /** Lê todas as linhas; campo nulo volta como {@code null}. Arquivo ausente: lista vazia. */
    public List<String[]> ler() throws PersistenciaException {
        List<String[]> linhas = new ArrayList<>();
        if (Files.notExists(arquivo)) {
            return linhas;
        }
        try (BufferedReader entrada = Files.newBufferedReader(arquivo, StandardCharsets.UTF_8)) {
            String linha;
            int numero = 0;
            while ((linha = entrada.readLine()) != null) {
                numero++;
                linhas.add(interpretar(linha, numero));
            }
        } catch (IOException e) {
            throw FalhaDePersistencia.de("Não foi possível ler " + arquivo, e);
        }
        return linhas;
    }

    private String montar(String[] campos) {
        if (campos.length != numeroDeCampos) {
            throw new IllegalArgumentException(
                    "Esperava " + numeroDeCampos + " campos, recebeu " + campos.length);
        }
        StringBuilder linha = new StringBuilder();
        for (int i = 0; i < campos.length; i++) {
            if (i > 0) {
                linha.append(SEPARADOR);
            }
            linha.append(codificar(campos[i]));
        }
        return linha.toString();
    }

    private static String codificar(String campo) {
        if (campo == null) {
            return NULO;
        }
        StringBuilder texto = new StringBuilder();
        for (char c : campo.toCharArray()) {
            switch (c) {
                case '\\' -> texto.append("\\\\");
                case SEPARADOR -> texto.append("\\;");
                case '\n' -> texto.append("\\n");
                case '\r' -> texto.append("\\r");
                default -> texto.append(c);
            }
        }
        return texto.toString();
    }

    private String[] interpretar(String linha, int numero) throws PersistenciaException {
        List<String> brutos = dividir(linha);
        if (brutos.size() != numeroDeCampos) {
            throw new PersistenciaException(arquivo.getFileName() + ", linha " + numero
                    + " mal formada: esperava " + numeroDeCampos + " campos e achou "
                    + brutos.size());
        }
        String[] campos = new String[numeroDeCampos];
        for (int i = 0; i < campos.length; i++) {
            campos[i] = decodificar(brutos.get(i), numero);
        }
        return campos;
    }

    /** Divide a linha nos {@code ;} que não estão escapados, mantendo os escapes intactos. */
    private static List<String> dividir(String linha) {
        List<String> brutos = new ArrayList<>();
        StringBuilder atual = new StringBuilder();
        for (int i = 0; i < linha.length(); i++) {
            char c = linha.charAt(i);
            if (c == '\\' && i + 1 < linha.length()) {
                atual.append(c).append(linha.charAt(++i));
            } else if (c == SEPARADOR) {
                brutos.add(atual.toString());
                atual.setLength(0);
            } else {
                atual.append(c);
            }
        }
        brutos.add(atual.toString());
        return brutos;
    }

    private String decodificar(String bruto, int numero) throws PersistenciaException {
        if (bruto.equals(NULO)) {
            return null;
        }
        StringBuilder texto = new StringBuilder();
        for (int i = 0; i < bruto.length(); i++) {
            char c = bruto.charAt(i);
            if (c != '\\') {
                texto.append(c);
            } else if (i + 1 >= bruto.length()) {
                throw new PersistenciaException(arquivo.getFileName() + ", linha " + numero
                        + ": escape incompleto");
            } else {
                texto.append(caractereEscapado(bruto.charAt(++i), numero));
            }
        }
        return texto.toString();
    }

    /** O caractere que um escape {@code \x} representa. */
    private char caractereEscapado(char seguinte, int numero) throws PersistenciaException {
        return switch (seguinte) {
            case '\\' -> '\\';
            case ';' -> ';';
            case 'n' -> '\n';
            case 'r' -> '\r';
            default -> throw new PersistenciaException(arquivo.getFileName() + ", linha "
                    + numero + ": escape inválido \\" + seguinte);
        };
    }
}