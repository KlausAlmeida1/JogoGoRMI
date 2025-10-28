package rede;

import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;

import javax.swing.SwingUtilities;

import modelo.Jogo;
import visao.JanelaJogo;

public class Servidor {
    public static void main(String args[]){
        try{
            JogoRemotoImpl servico = new JogoRemotoImpl();

            LocateRegistry.createRegistry(1099);

            Naming.rebind("rmi://localhost/JogoGo", servico);

            System.out.println("Servidor pronto");

            InterfaceJogoRemoto jogoRemoto = (InterfaceJogoRemoto) Naming.lookup("rmi://localhost/JogoGo");

             SwingUtilities.invokeLater(() -> {
                JanelaJogo janela = new JanelaJogo(jogoRemoto, true);
                janela.setTitle("Jogo do servidor");
                janela.setVisible(true);
            });
        }
        catch (Exception e){
            System.err.println("Erro no servidor: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
