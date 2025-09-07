package rede;

import java.rmi.Naming;
import javax.swing.SwingUtilities;
import visao.JanelaJogo;

public class Cliente {
    public static void main(String args[]){
        try {
            InterfaceJogoRemoto jogoRemoto = (InterfaceJogoRemoto) Naming.lookup("rmi://localhost/JogoGo");

            SwingUtilities.invokeLater(() -> {
                JanelaJogo janela = new JanelaJogo(jogoRemoto);
                janela.setVisible(true);
            });
        } catch (Exception e) {
            System.err.println("Erro no cliente: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
