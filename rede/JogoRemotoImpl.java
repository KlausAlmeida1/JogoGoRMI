package rede;

import java.rmi.server.UnicastRemoteObject;
import java.rmi.RemoteException;
import modelo.Jogo;
import modelo.EstadoJogo;

public class JogoRemotoImpl extends UnicastRemoteObject implements InterfaceJogoRemoto {
    private final Jogo jogo;

    public JogoRemotoImpl() throws RemoteException {
        super();
        // o 5 significa os minutos
        this.jogo = new Jogo(9, 5 * 60_000L);
    }

    @Override public boolean fazerJogada(int x, int y, int corJogador) throws RemoteException {
        return jogo.fazerJogada(x, y, corJogador);
    }
    @Override public void passar(int corJogador) throws RemoteException { jogo.passar(corJogador); }
    @Override public void desistir(int corJogador) throws RemoteException { jogo.desistir(corJogador); }
    @Override public void reiniciar() throws RemoteException { jogo.reiniciar(); }

    @Override public EstadoJogo getEstadoJogo() throws RemoteException {
        return jogo.snapshotEstado();
    }
}
