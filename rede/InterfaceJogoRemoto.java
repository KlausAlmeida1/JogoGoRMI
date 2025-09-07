package rede;

import modelo.Tabuleiro;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface InterfaceJogoRemoto extends Remote{

    boolean fazerJogada(int x,int y) throws RemoteException;
    Tabuleiro getTabuleiro() throws RemoteException;
}
