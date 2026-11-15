package br.ufpb.dcx.poo.biblioteca.dominio;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A fila de reservas de um item, em ordem de chegada.
 *
 * <p>Compõe uma lista privada em vez de herdar de {@code ArrayList}: quem usa a fila só
 * consegue entrar, sair e perguntar quem é o próximo. Nenhum {@code add(int, ...)},
 * {@code set} ou {@code clear} fura a ordem de chegada.</p>
 */
public final class FilaDeReservas {

    private final List<Reserva> reservas = new ArrayList<>();

    /** A reserva entra no fim da fila. */
    public void entrar(Reserva reserva) {
        reservas.add(reserva);
    }

    public void remover(Reserva reserva) {
        reservas.remove(reserva);
    }

    /** Uma cópia das reservas ativas, por ordem de chegada. */
    public List<Reserva> ativas() {
        return List.copyOf(reservas);
    }

    /** A reserva mais antiga que ainda não tem exemplar separado. */
    public Optional<Reserva> primeiraSemExemplar() {
        for (Reserva reserva : reservas) {
            if (!reserva.temExemplarSeparado()) {
                return Optional.of(reserva);
            }
        }
        return Optional.empty();
    }

    public boolean temReservaDe(Usuario usuario) {
        for (Reserva reserva : reservas) {
            if (reserva.pertenceA(usuario)) {
                return true;
            }
        }
        return false;
    }

    /** Posição da reserva na fila, de 1 em diante; 0 se não estiver na fila. */
    public int posicaoDe(Reserva reserva) {
        return reservas.indexOf(reserva) + 1;
    }
}