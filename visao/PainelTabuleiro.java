// Arquivo: src/go/visao/PainelTabuleiro.java
package visao;

import modelo.Tabuleiro;
import javax.swing.*;
import java.awt.*;

public class PainelTabuleiro extends JPanel {

    private Tabuleiro tabuleiro;
    
    private final int PADDING = 30;
    private final int TAMANHO_CELULA;

    public PainelTabuleiro(Tabuleiro tabuleiro) {
        this.tabuleiro = tabuleiro;
        int tamanhoTotal = 500;
        
        this.TAMANHO_CELULA = (tamanhoTotal - 2 * PADDING) / (tabuleiro.getTamanho() - 1);
        
        setPreferredSize(new Dimension(tamanhoTotal, tamanhoTotal));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        g2d.setColor(new Color(218, 165, 32));
        g2d.fillRect(0, 0, getWidth(), getHeight());

        g2d.setColor(Color.BLACK);
        int tamanhoGrid = (tabuleiro.getTamanho() - 1) * TAMANHO_CELULA;
        for (int i = 0; i < tabuleiro.getTamanho(); i++) {
            g2d.drawLine(PADDING + i * TAMANHO_CELULA, PADDING, PADDING + i * TAMANHO_CELULA, PADDING + tamanhoGrid);
            g2d.drawLine(PADDING, PADDING + i * TAMANHO_CELULA, PADDING + tamanhoGrid, PADDING + i * TAMANHO_CELULA);
        }

        for (int x = 0; x < tabuleiro.getTamanho(); x++) {
            for (int y = 0; y < tabuleiro.getTamanho(); y++) {
                
                int peca = tabuleiro.getPeca(x, y);

                if (peca != Tabuleiro.VAZIO) {
                    if (peca == Tabuleiro.PRETO) {
                        g2d.setColor(Color.BLACK);
                    } else {
                        g2d.setColor(Color.WHITE);
                    }
                    
                    int diametro = (int)(TAMANHO_CELULA * 0.9);
                    g2d.fillOval(PADDING + x * TAMANHO_CELULA - diametro / 2, PADDING + y * TAMANHO_CELULA - diametro / 2, diametro, diametro);
                }
            }
        }
    }

    public void setTabuleiro(Tabuleiro novoTabuleiro) {
        this.tabuleiro = novoTabuleiro;
    }
}