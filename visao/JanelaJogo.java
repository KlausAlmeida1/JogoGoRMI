package visao;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.rmi.RemoteException;
import java.util.List;
import javax.swing.*;

import modelo.EstadoJogo;
import modelo.Tabuleiro;
import rede.InterfaceJogoRemoto;

public class JanelaJogo extends JFrame {
    private final InterfaceJogoRemoto jogoRemoto;
    private final PainelTabuleiro painelTabuleiro;
    private final PainelStatus painelStatus;
    private final Timer timer;
    private boolean gameOverDialogShown = false; // evita múltiplos diálogos

    public JanelaJogo(InterfaceJogoRemoto jogoRemoto, boolean isServidor) {
        super(isServidor ? "Go — Você: PRETO (Servidor)" : "Go — Você: BRANCO (Cliente)");
        this.jogoRemoto = jogoRemoto;

        EstadoJogo estadoInicial;
        try {
            estadoInicial = jogoRemoto.getEstadoJogo();
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }

        this.painelTabuleiro = new PainelTabuleiro(estadoInicial.getTabuleiro());
        this.painelTabuleiro.setCellSize(48); // ajuste o tamanho da célula aqui
        this.painelStatus = new PainelStatus(isServidor);

        setLayout(new BorderLayout());
        add(painelTabuleiro, BorderLayout.CENTER);
        add(painelStatus, BorderLayout.SOUTH);
        add(buildSidebar(isServidor), BorderLayout.EAST);

        painelTabuleiro.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                int[] xy = painelTabuleiro.mouseToGrid(e.getX(), e.getY());
                int x = xy[0], y = xy[1];
                if (x < 0 || y < 0) return;

                try {
                    int minhaCor = isServidor ? Tabuleiro.PRETO : Tabuleiro.BRANCO;
                    EstadoJogo est = jogoRemoto.getEstadoJogo();
                    if (est.isGameOver()) { maybeShowGameOverDialog(); return; }
                    if (est.getJogadorAtual() != minhaCor) {
                        JOptionPane.showMessageDialog(JanelaJogo.this, "Aguarde sua vez.");
                        return;
                    }
                    boolean ok = jogoRemoto.fazerJogada(x, y, minhaCor);
                    if (!ok) {
                        EstadoJogo eAtual = jogoRemoto.getEstadoJogo();
                        String msg = eAtual.getLastInfo() != null ? eAtual.getLastInfo() : "Jogada ilegal.";
                        JOptionPane.showMessageDialog(JanelaJogo.this, msg);
                    }
                    atualizarTela();
                } catch (RemoteException ex) {
                    JOptionPane.showMessageDialog(JanelaJogo.this, "Erro de rede.");
                }
            }
        });

        timer = new Timer(500, e -> atualizarTela());
        timer.start();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(true);
        pack();
        setLocationRelativeTo(null);
    }

    private JPanel buildSidebar(boolean isServidor){
        SidebarPanel side = new SidebarPanel(); // painel com gradiente madeira escura
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        // largura consistente
        Dimension w = new Dimension(180, 0);
        side.setPreferredSize(w);
        side.setMinimumSize(w);
        side.setMaximumSize(new Dimension(220, Integer.MAX_VALUE));

        JLabel titulo = new JLabel("Ações", SwingConstants.CENTER);
        titulo.setAlignmentX(Component.CENTER_ALIGNMENT);
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 16f));
        titulo.setForeground(new Color(40, 40, 40));

        JButton btnPassar = createSidebarButton("Passar (P)");
        JButton btnDesistir = createSidebarButton("Desistir (R)");
        JButton btnNovo = createSidebarButton("Novo Jogo");  // <<< novo

        btnPassar.addActionListener(a -> {
            try {
                int minhaCor = isServidor ? Tabuleiro.PRETO : Tabuleiro.BRANCO;
                jogoRemoto.passar(minhaCor);
                atualizarTela();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Erro de rede ao passar.");
            }
        });
        btnDesistir.addActionListener(a -> {
            int conf = JOptionPane.showConfirmDialog(this, "Confirmar desistência?", "Desistir", JOptionPane.YES_NO_OPTION);
            if (conf != JOptionPane.YES_OPTION) return;
            try {
                int minhaCor = isServidor ? Tabuleiro.PRETO : Tabuleiro.BRANCO;
                jogoRemoto.desistir(minhaCor);
                atualizarTela();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Erro de rede ao desistir.");
            }
        });
        btnNovo.addActionListener(a -> {                     // <<< novo
            try {
                jogoRemoto.reiniciar();
                gameOverDialogShown = false;
                atualizarTela();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Erro de rede ao reiniciar.");
            }
        });

        side.add(Box.createVerticalGlue());
        side.add(titulo);
        side.add(Box.createVerticalStrut(14));
        side.add(btnPassar);
        side.add(Box.createVerticalStrut(10));
        side.add(btnDesistir);
        side.add(Box.createVerticalStrut(10));
        side.add(btnNovo); // <<< novo
        side.add(Box.createVerticalGlue());

        return side;
    }

    private JButton createSidebarButton(String text){
        JButton b = new JButton(text);
        b.setAlignmentX(Component.CENTER_ALIGNMENT);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(120,110,90)),
            BorderFactory.createEmptyBorder(8,14,8,14)
        ));
        b.setBackground(new Color(236, 222, 186)); // tom “madeira”
        b.setOpaque(true);
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        b.addChangeListener(e -> {
            if (b.getModel().isPressed()) b.setBackground(new Color(230, 214, 176));
            else if (b.getModel().isRollover()) b.setBackground(new Color(243, 230, 197));
            else b.setBackground(new Color(236, 222, 186));
        });
        return b;
    }

    /** Painel com gradiente “madeira” mais escuro, combinando com o tabuleiro */
    static class SidebarPanel extends JPanel {
        SidebarPanel() { setOpaque(true); }
        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g); // base
            Graphics2D g2 = (Graphics2D) g.create();
            int w = getWidth(), h = getHeight();
            Color c1 = new Color(228, 200, 140); // topo
            Color c2 = new Color(206, 178, 118); // base (mais escuro)
            g2.setPaint(new GradientPaint(0, 0, c1, 0, h, c2));
            g2.fillRect(0, 0, w, h);
            // borda sutil à esquerda
            g2.setColor(new Color(180,160,110));
            g2.drawLine(0, 0, 0, h);
            g2.dispose();
        }
    }

    private void atualizarTela() {
        try {
            EstadoJogo estadoAtual = jogoRemoto.getEstadoJogo();

            painelTabuleiro.setTabuleiro(estadoAtual.getTabuleiro());
            painelTabuleiro.setTurnoAtual(estadoAtual.getJogadorAtual());
            painelTabuleiro.setUltimaJogada(estadoAtual.getLastX(), estadoAtual.getLastY());
            List<int[]> caps = estadoAtual.getUltimasCapturas();
            if (caps != null && !caps.isEmpty()) painelTabuleiro.flashCaptures(caps);

            painelStatus.atualizarStatus(estadoAtual);
            painelTabuleiro.repaint();
            pack();

            if (estadoAtual.isGameOver()) {
                maybeShowGameOverDialog();
            } else {
                gameOverDialogShown = false; // reset para próximo fim
            }
        } catch (RemoteException e) {
            JOptionPane.showMessageDialog(this, "Conexão com o servidor perdida.", "Erro de Rede", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Mostra o diálogo de fim de jogo uma única vez por término */
    private void maybeShowGameOverDialog() {
        if (gameOverDialogShown) return;
        gameOverDialogShown = true;
        String[] options = {"Novo jogo", "Fechar"};
        int choice = JOptionPane.showOptionDialog(
                this,
                "O jogo terminou. O que deseja fazer?",
                "Fim de jogo",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.INFORMATION_MESSAGE,
                null, options, options[0]
        );
        if (choice == JOptionPane.YES_OPTION) {
            try {
                jogoRemoto.reiniciar();
                gameOverDialogShown = false;
                atualizarTela();
            } catch (RemoteException ex) {
                JOptionPane.showMessageDialog(this, "Erro ao reiniciar.");
            }
        } else {
            // Fecha somente esta janela
            dispose();
        }
    }
}
