// Arquivo: src/visao/JanelaJogo.java (VERSÃO FINAL DE REDE)
package visao;

import java.rmi.RemoteException;
import javax.swing.Timer;
import modelo.Tabuleiro;
import rede.InterfaceJogoRemoto;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class JanelaJogo extends JFrame {

    private final InterfaceJogoRemoto jogoRemoto;
    private final PainelTabuleiro painelTabuleiro;
    private final Timer timer;

    private final int padding = 30;
    private final int tamanhoCelula;

    public JanelaJogo(InterfaceJogoRemoto jogoRemoto) {
        this.jogoRemoto = jogoRemoto;
        
        Tabuleiro tabuleiroInicial = null;
        try {
            tabuleiroInicial = jogoRemoto.getTabuleiro();
        } catch (RemoteException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Erro de conexão com o servidor.", "Erro de Rede", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        this.painelTabuleiro = new PainelTabuleiro(tabuleiroInicial);
        this.tamanhoCelula = (500 - 2 * padding) / (tabuleiroInicial.getTamanho() - 1);

        painelTabuleiro.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                try {
                    int x = Math.round((float)(e.getX() - padding) / tamanhoCelula);
                    int y = Math.round((float)(e.getY() - padding) / tamanhoCelula);
                    
                    jogoRemoto.fazerJogada(x, y);
                } catch (RemoteException ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(JanelaJogo.this, "Erro de comunicação com o servidor.", "Erro de Rede", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        this.timer = new Timer(500, e -> atualizarTela());
        this.timer.start();

        this.setTitle("Jogo de Go (Rede)");
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.add(painelTabuleiro);
        this.pack();
        this.setLocationRelativeTo(null);
        this.setResizable(false);
    }

    private void atualizarTela() {
        try {
            Tabuleiro tabuleiroAtualizado = jogoRemoto.getTabuleiro();
            this.painelTabuleiro.setTabuleiro(tabuleiroAtualizado);
            this.painelTabuleiro.repaint();
        } catch (RemoteException e) {
            this.timer.stop();
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Conexão com o servidor perdida.", "Erro de Rede", JOptionPane.ERROR_MESSAGE);
        }
    }
}