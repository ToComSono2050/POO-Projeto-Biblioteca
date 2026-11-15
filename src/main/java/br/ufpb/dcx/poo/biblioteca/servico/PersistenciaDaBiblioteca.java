package br.ufpb.dcx.poo.biblioteca.servico;

import java.nio.file.Path;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import br.ufpb.dcx.poo.biblioteca.contrato.StatusExemplar;
import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.PersistenciaException;
import br.ufpb.dcx.poo.biblioteca.dominio.Emprestimo;
import br.ufpb.dcx.poo.biblioteca.dominio.Exemplar;
import br.ufpb.dcx.poo.biblioteca.dominio.Item;
import br.ufpb.dcx.poo.biblioteca.dominio.Reserva;
import br.ufpb.dcx.poo.biblioteca.dominio.Usuario;
import br.ufpb.dcx.poo.biblioteca.persistencia.ArquivoDeTabela;
import br.ufpb.dcx.poo.biblioteca.persistencia.FalhaDePersistencia;

/**
 * Converte o estado da biblioteca em linhas de arquivo e de volta. É a única ponte entre
 * o domínio (que não faz E/S) e a camada de persistência (que não conhece o domínio).
 *
 * <p>{@code carregar} monta um registro novo ao lado, confere tudo, e só no fim troca o
 * estado atual: arquivo corrompido não altera a biblioteca.</p>
 */
final class PersistenciaDaBiblioteca {

    private final RegistroDaBiblioteca registro;

    PersistenciaDaBiblioteca(RegistroDaBiblioteca registro) {
        this.registro = registro;
    }

    // ---------------------------------------------------------------- salvar

    void salvar(Path diretorio) throws PersistenciaException {
        List<Item> itens = itensOrdenadosPorCodigo();
        arquivoDeItens(diretorio).gravar(linhasDeItens(itens));
        arquivoDeExemplares(diretorio).gravar(linhasDeExemplares(itens));
        arquivoDeUsuarios(diretorio).gravar(linhasDeUsuarios());
        arquivoDeEmprestimos(diretorio).gravar(linhasDeEmprestimos());
        arquivoDeReservas(diretorio).gravar(linhasDeReservas(itens));
    }

    private List<Item> itensOrdenadosPorCodigo() {
        List<Item> itens = new ArrayList<>(registro.itens().values());
        itens.sort(Comparator.comparing(Item::getCodigo));
        return itens;
    }

    private static List<String[]> linhasDeItens(List<Item> itens) {
        List<String[]> linhas = new ArrayList<>();
        for (Item item : itens) {
            linhas.add(new String[] {item.getCodigo(), item.getTitulo(), item.getAutoria(),
                    item.getCategoria(), String.valueOf(item.getAno())});
        }
        return linhas;
    }

    private static List<String[]> linhasDeExemplares(List<Item> itens) {
        List<String[]> linhas = new ArrayList<>();
        for (Item item : itens) {
            List<Exemplar> exemplares = new ArrayList<>(item.getExemplares());
            exemplares.sort(Comparator.comparing(Exemplar::getTombo));
            for (Exemplar exemplar : exemplares) {
                linhas.add(new String[] {exemplar.getTombo(), exemplar.getCodigoDoItem(),
                        exemplar.getStatus().name()});
            }
        }
        return linhas;
    }

    private List<String[]> linhasDeUsuarios() {
        List<Usuario> usuarios = new ArrayList<>(registro.usuarios().values());
        usuarios.sort(Comparator.comparing(Usuario::getMatricula));
        List<String[]> linhas = new ArrayList<>();
        for (Usuario usuario : usuarios) {
            linhas.add(new String[] {usuario.getMatricula(), usuario.getNome(),
                    String.valueOf(usuario.isAtivo())});
        }
        return linhas;
    }

    /** Na ordem em que os empréstimos foram registrados (inclusive os devolvidos). */
    private List<String[]> linhasDeEmprestimos() {
        List<String[]> linhas = new ArrayList<>();
        for (Emprestimo emprestimo : registro.emprestimos()) {
            linhas.add(new String[] {emprestimo.getId(), emprestimo.getExemplar().getTombo(),
                    emprestimo.getUsuario().getMatricula(),
                    emprestimo.getDataDoEmprestimo().toString(),
                    textoOuNulo(emprestimo.getDataDaDevolucao())});
        }
        return linhas;
    }

    /** Cada fila já está em ordem de chegada, e a ordem das linhas a preserva. */
    private static List<String[]> linhasDeReservas(List<Item> itens) {
        List<String[]> linhas = new ArrayList<>();
        for (Item item : itens) {
            for (Reserva reserva : item.getFila().ativas()) {
                Exemplar separado = reserva.getExemplarSeparado();
                linhas.add(new String[] {reserva.getId(), reserva.getCodigoDoItem(),
                        reserva.getUsuario().getMatricula(), reserva.getDataDaReserva().toString(),
                        separado == null ? null : separado.getTombo()});
            }
        }
        return linhas;
    }

    private static String textoOuNulo(Object valor) {
        return valor == null ? null : valor.toString();
    }

    // --------------------------------------------------------------- carregar

    void carregar(Path diretorio) throws PersistenciaException {
        RegistroDaBiblioteca novo = new RegistroDaBiblioteca();
        // Ordem de carga: cada arquivo referencia o anterior por código, tombo ou matrícula.
        converter("itens.txt", arquivoDeItens(diretorio).ler(), campos -> lerItem(novo, campos));
        converter("exemplares.txt", arquivoDeExemplares(diretorio).ler(), campos -> lerExemplar(novo, campos));
        converter("usuarios.txt", arquivoDeUsuarios(diretorio).ler(), campos -> lerUsuario(novo, campos));
        converter("emprestimos.txt", arquivoDeEmprestimos(diretorio).ler(), campos -> lerEmprestimo(novo, campos));
        converter("reservas.txt", arquivoDeReservas(diretorio).ler(), campos -> lerReserva(novo, campos));
        conferirConsistencia(novo);
        registro.substituirPor(novo); // só aqui o estado atual muda
    }

    private interface ConversorDeLinha {
        void converter(String[] campos) throws PersistenciaException;
    }

    /** Aplica o conversor a cada linha, dizendo arquivo e linha quando algo estiver errado. */
    private static void converter(String arquivo, List<String[]> linhas, ConversorDeLinha conversor)
            throws PersistenciaException {
        int numero = 0;
        for (String[] campos : linhas) {
            numero++;
            try {
                conversor.converter(campos);
            } catch (PersistenciaException e) {
                throw FalhaDePersistencia.de(arquivo + ", linha " + numero + ": " + e.getMessage(), e);
            } catch (IllegalArgumentException | DateTimeException e) {
                throw FalhaDePersistencia.de(
                        arquivo + ", linha " + numero + " inválida: " + e.getMessage(), e);
            }
        }
    }

    private static void lerItem(RegistroDaBiblioteca novo, String[] campos) throws PersistenciaException {
        String codigo = campos[0];
        Item item = new Item(codigo, campos[1], campos[2], campos[3], Integer.parseInt(campos[4]));
        if (novo.itens().putIfAbsent(codigo, item) != null) {
            throw new PersistenciaException("código de item repetido: " + codigo);
        }
    }

    private static void lerExemplar(RegistroDaBiblioteca novo, String[] campos) throws PersistenciaException {
        String tombo = campos[0];
        String codigoDoItem = campos[1];
        Item item = existente(novo.itens(), codigoDoItem, "exemplar " + tombo + " referencia item");
        Exemplar exemplar = new Exemplar(tombo, codigoDoItem, StatusExemplar.valueOf(campos[2]));
        if (novo.exemplares().putIfAbsent(tombo, exemplar) != null) {
            throw new PersistenciaException("tombo repetido: " + tombo);
        }
        item.adicionarExemplar(exemplar);
    }

    private static void lerUsuario(RegistroDaBiblioteca novo, String[] campos) throws PersistenciaException {
        String matricula = campos[0];
        String situacao = campos[2];
        if (!"true".equals(situacao) && !"false".equals(situacao)) {
            throw new PersistenciaException("situação inválida: " + situacao);
        }
        Usuario usuario = new Usuario(matricula, campos[1], Boolean.parseBoolean(situacao));
        if (novo.usuarios().putIfAbsent(matricula, usuario) != null) {
            throw new PersistenciaException("matrícula repetida: " + matricula);
        }
    }

    private static void lerEmprestimo(RegistroDaBiblioteca novo, String[] campos) throws PersistenciaException {
        String id = campos[0];
        String tombo = campos[1];
        Exemplar exemplar = existente(novo.exemplares(), tombo, "empréstimo referencia tombo");
        Usuario usuario = existente(novo.usuarios(), campos[2], "empréstimo referencia matrícula");
        novo.absorverIdDeEmprestimo(id);
        String devolucao = campos[4];
        Emprestimo emprestimo = new Emprestimo(id, exemplar, usuario, LocalDate.parse(campos[3]),
                devolucao == null ? null : LocalDate.parse(devolucao));
        if (emprestimo.estaAtivo() && novo.emprestimosAtivosPorTombo().containsKey(tombo)) {
            throw new PersistenciaException("dois empréstimos ativos para o tombo " + tombo);
        }
        novo.registrarEmprestimo(emprestimo);
    }

    private static void lerReserva(RegistroDaBiblioteca novo, String[] campos) throws PersistenciaException {
        String id = campos[0];
        String codigoDoItem = campos[1];
        Item item = existente(novo.itens(), codigoDoItem, "reserva referencia item");
        Usuario usuario = existente(novo.usuarios(), campos[2], "reserva referencia matrícula");
        novo.absorverIdDeReserva(id);
        Reserva reserva = new Reserva(id, codigoDoItem, usuario, LocalDate.parse(campos[3]));
        if (novo.reservasAtivas().containsKey(id) || item.getFila().temReservaDe(usuario)) {
            throw new PersistenciaException("reserva repetida: " + id);
        }
        String tomboSeparado = campos[4];
        if (tomboSeparado != null) {
            separarExemplar(novo, reserva, tomboSeparado);
        }
        item.getFila().entrar(reserva);
        novo.reservasAtivas().put(id, reserva);
    }

    /** O tombo separado precisa ser do mesmo item, estar RESERVADO e não ter outra dona. */
    private static void separarExemplar(RegistroDaBiblioteca novo, Reserva reserva, String tombo)
            throws PersistenciaException {
        Exemplar separado = existente(novo.exemplares(), tombo, "reserva referencia tombo");
        boolean podeSerDela = separado.getCodigoDoItem().equals(reserva.getCodigoDoItem())
                && separado.getStatus() == StatusExemplar.RESERVADO
                && separado.getReservaDona() == null;
        if (!podeSerDela) {
            throw new PersistenciaException("tombo " + tombo + " não pode estar separado para " + reserva.getId());
        }
        reserva.separar(separado);
    }

    /** Estados que só existem num arquivo adulterado: exemplar sem empréstimo ou sem dono. */
    private static void conferirConsistencia(RegistroDaBiblioteca novo) throws PersistenciaException {
        for (Exemplar exemplar : novo.exemplares().values()) {
            boolean emprestado = novo.emprestimosAtivosPorTombo().containsKey(exemplar.getTombo());
            if ((exemplar.getStatus() == StatusExemplar.EMPRESTADO) != emprestado) {
                throw new PersistenciaException("estado inconsistente para o tombo "
                        + exemplar.getTombo() + ": " + exemplar.getStatus()
                        + " e empréstimo ativo = " + emprestado);
            }
            if (exemplar.getStatus() == StatusExemplar.RESERVADO && exemplar.getReservaDona() == null) {
                throw new PersistenciaException("tombo " + exemplar.getTombo()
                        + " está RESERVADO, mas nenhuma reserva o guarda");
            }
        }
    }

    /** Procura a referência no mapa; se não existir, o arquivo está corrompido. */
    private static <T> T existente(Map<String, T> mapa, String chave, String descricao)
            throws PersistenciaException {
        T valor = mapa.get(chave);
        if (valor == null) {
            throw new PersistenciaException(descricao + " inexistente: " + chave);
        }
        return valor;
    }

    // ----------------------------------------------------------------- arquivos

    private static ArquivoDeTabela arquivoDeItens(Path diretorio) {
        return new ArquivoDeTabela(diretorio.resolve("itens.txt"), 5);
    }

    private static ArquivoDeTabela arquivoDeExemplares(Path diretorio) {
        return new ArquivoDeTabela(diretorio.resolve("exemplares.txt"), 3);
    }

    private static ArquivoDeTabela arquivoDeUsuarios(Path diretorio) {
        return new ArquivoDeTabela(diretorio.resolve("usuarios.txt"), 3);
    }

    private static ArquivoDeTabela arquivoDeEmprestimos(Path diretorio) {
        return new ArquivoDeTabela(diretorio.resolve("emprestimos.txt"), 5);
    }

    private static ArquivoDeTabela arquivoDeReservas(Path diretorio) {
        return new ArquivoDeTabela(diretorio.resolve("reservas.txt"), 5);
    }
}