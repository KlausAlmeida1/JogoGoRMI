package rede;

import java.rmi.server.UnicastRemoteObject;
import java.rmi.RemoteException;
import modelo.Jogo;
import modelo.Tabuleiro;

public class JogoRemotoImpl extends UnicastRemoteObject implements InterfaceJogoRemoto {
    private Jogo jogo;

    public JogoRemotoImpl() throws RemoteException{
        super();
        this.jogo = new Jogo(9);
    }

    @Override
    public boolean fazerJogada(int x, int y) throws RemoteException{
        return jogo.fazerJogada(x, y);
    }

    @Override
    public Tabuleiro getTabuleiro() throws RemoteException{
        return jogo.getTabuleiro();
    }
}
