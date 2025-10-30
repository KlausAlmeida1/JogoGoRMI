package rede;

import java.rmi.Remote;
import java.rmi.RemoteException;
import modelo.EstadoJogo;

public interface InterfaceJogoRemoto extends Remote {
    boolean fazerJogada(int x, int y, int corJogador) throws RemoteException;
    void passar(int corJogador) throws RemoteException;
    void desistir(int corJogador) throws RemoteException;
    void reiniciar() throws RemoteException;

    EstadoJogo getEstadoJogo() throws RemoteException;
}
