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
    
    private boolean gameOverDialogShown = false;

    // Este é o Construtor. É o "dia da construção" da janela.
    // Ele roda SÓ UMA VEZ, no início, para montar tudo.
    public JanelaJogo(InterfaceJogoRemoto jogoRemoto, boolean isServidor) {
        // Define o título da janela (Ex: "Go - Você: PRETO").
        super(isServidor ? "Go — Você: PRETO (Servidor)" : "Go — Você: BRANCO (Cliente)");
        
        // Guarda o "telefone" RMI para ser usado por todos os métodos.
        this.jogoRemoto = jogoRemoto;

        // Pede o estado INICIAL do jogo ao servidor.
        // Isso é crucial para o tabuleiro não começar vazio se o cliente se conectar
        // no meio de um jogo.
        EstadoJogo estadoInicial;
        try {
            estadoInicial = jogoRemoto.getEstadoJogo();
        } catch (RemoteException e) {
            // Se não conseguir nem pegar o estado inicial, o jogo não pode abrir.
            throw new RuntimeException(e);
        }

        // Cria os 3 componentes visuais principais da janela.
        this.painelTabuleiro = new PainelTabuleiro(estadoInicial.getTabuleiro());
        this.painelTabuleiro.setCellSize(48); // Define um tamanho de célula fixo (48px).
        this.painelStatus = new PainelStatus(isServidor);

        // Organiza a janela: Tabuleiro no CENTRO, Status embaixo (SOUTH),
        // e a barra de botões na direita (EAST).
        setLayout(new BorderLayout());
        add(painelTabuleiro, BorderLayout.CENTER);
        add(painelStatus, BorderLayout.SOUTH);
        // Chama a função "buildSidebar" para construir o painel de botões.
        add(buildSidebar(isServidor), BorderLayout.EAST);

        // ===== FLUXO DE AÇÃO (O Clique do Jogador) =====
        // "Instala a campainha" (o MouseListener) no tabuleiro.
        // Este código só roda QUANDO o usuário clicar no tabuleiro.
        painelTabuleiro.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                // 1. Pede ao painel para "traduzir" o pixel do clique (ex: 110, 160)
                // para a coordenada da grade (ex: 0, 0).
                int[] xy = painelTabuleiro.mouseToGrid(e.getX(), e.getY());
                int x = xy[0], y = xy[1];
                // Se o clique foi fora da grade (retornou -1), ignora.
                if (x < 0 || y < 0) return;

                try {
                    // 2. Descobre quem EU sou (PRETO ou BRANCO).
                    int minhaCor = isServidor ? Tabuleiro.PRETO : Tabuleiro.BRANCO;
                    
                    // 3. Pega o estado do jogo ANTES de jogar, para validar.
                    EstadoJogo est = jogoRemoto.getEstadoJogo();
                    
                    // 4. Validações: O jogo já acabou?
                    if (est.isGameOver()) { maybeShowGameOverDialog(); return; }
                    // É a minha vez de jogar?
                    if (est.getJogadorAtual() != minhaCor) {
                        JOptionPane.showMessageDialog(JanelaJogo.this, "Aguarde sua vez.");
                        return;
                    }
                    
                    // 5. **A CHAMADA DE REDE (AÇÃO)**
                    // Tenta fazer a jogada. O servidor (Jogo.java) vai validar
                    // as regras (Ko, Suicídio, etc.).
                    boolean ok = jogoRemoto.fazerJogada(x, y, minhaCor);
                    
                    // 6. Se o servidor disse que a jogada foi ilegal (retornou 'false')...
                    if (!ok) {
                        // ...pega a mensagem de erro que o servidor guardou...
                        EstadoJogo eAtual = jogoRemoto.getEstadoJogo();
                        String msg = eAtual.getLastInfo() != null ? eAtual.getLastInfo() : "Jogada ilegal.";
                        // ...e mostra na tela.
                        JOptionPane.showMessageDialog(JanelaJogo.this, msg);
                    }
                    
                    // 7. Força uma atualização IMEDIATA da tela.
                    // (Não espera pelo timer de 500ms, para a jogada parecer instantânea).
                    atualizarTela();
                } catch (RemoteException ex) {
                    JOptionPane.showMessageDialog(JanelaJogo.this, "Erro de rede.");
                }
            }
        });

        // Cria o "motor" que vai rodar a cada 500ms.
        // (e -> atualizarTela()) é um atalho (lambda) para "execute 'atualizarTela()'".
        timer = new Timer(500, e -> atualizarTela());
        timer.start(); // Liga o motor.

        // Configurações finais da janela.
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // Fecha o programa no "X".
        setResizable(true); // Permite ao usuário redimensionar.
        pack(); // Ajusta o tamanho da janela automaticamente ao conteúdo.
        setLocationRelativeTo(null); // Centraliza a janela na tela.
    }

    // Função de fábrica: Constrói, estiliza e retorna o painel lateral (Sidebar)
    // com os botões de ação.
    private JPanel buildSidebar(boolean isServidor){
        // Usa a classe customizada "SidebarPanel" (definida lá embaixo)
        // que tem o fundo gradiente de "madeira".
        SidebarPanel side = new SidebarPanel();
        // Organiza os botões verticalmente, de cima para baixo (Y_AXIS).
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        // Adiciona uma margem interna de 16 pixels.
        side.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        // Define uma largura fixa para a barra lateral (180 pixels).
        Dimension w = new Dimension(180, 0);
        side.setPreferredSize(w);
        side.setMinimumSize(w);
        side.setMaximumSize(new Dimension(220, Integer.MAX_VALUE));

        // Cria o título "Ações" e o estiliza.
        JLabel titulo = new JLabel("Ações", SwingConstants.CENTER);
        titulo.setAlignmentX(Component.CENTER_ALIGNMENT); // Centraliza.
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 16f));
        titulo.setForeground(new Color(40, 40, 40));

        // Cria os 3 botões de ação (usando a outra função de fábrica).
        JButton btnPassar = createSidebarButton("Passar (P)");
        JButton btnDesistir = createSidebarButton("Desistir (R)");
        JButton btnNovo = createSidebarButton("Novo Jogo");

        // "Instala a campainha" (Listener) no botão "Passar".
        btnPassar.addActionListener(a -> {
            try {
                int minhaCor = isServidor ? Tabuleiro.PRETO : Tabuleiro.BRANCO;
                // Manda a ordem "passar" para o servidor.
                jogoRemoto.passar(minhaCor);
                atualizarTela(); // Atualiza a tela imediatamente.
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Erro de rede ao passar.");
            }
        });
        
        // "Instala a campainha" no botão "Desistir".
        btnDesistir.addActionListener(a -> {
            // Mostra um pop-up de confirmação ANTES de desistir.
            int conf = JOptionPane.showConfirmDialog(this, "Confirmar desistência?", "Desistir", JOptionPane.YES_NO_OPTION);
            if (conf != JOptionPane.YES_OPTION) return; // Se clicou "Não", cancela.
            
            try {
                int minhaCor = isServidor ? Tabuleiro.PRETO : Tabuleiro.BRANCO;
                // Manda a ordem "desistir" para o servidor.
                jogoRemoto.desistir(minhaCor);
                atualizarTela(); // Atualiza a tela imediatamente.
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Erro de rede ao desistir.");
            }
        });
        
        // "Instala a campainha" no botão "Novo Jogo".
        btnNovo.addActionListener(a -> {
            try {
                // Manda a ordem "reiniciar" para o servidor.
                jogoRemoto.reiniciar();
                // Reseta o controle do pop-up (para o jogo não travar).
                gameOverDialogShown = false;
                atualizarTela(); // Atualiza a tela imediatamente.
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Erro de rede ao reiniciar.");
            }
        });

        // Adiciona os componentes na barra (Titulo, Botões)
        // com espaçadores (Glue e Strut) para centralizá-los verticalmente.
        side.add(Box.createVerticalGlue()); // Espaço flexível em cima.
        side.add(titulo);
        side.add(Box.createVerticalStrut(14)); // Espaço fixo (14px).
        side.add(btnPassar);
        side.add(Box.createVerticalStrut(10)); // Espaço fixo (10px).
        side.add(btnDesistir);
        side.add(Box.createVerticalStrut(10));
        side.add(btnNovo);
        side.add(Box.createVerticalGlue()); // Espaço flexível embaixo.

        return side; // Retorna o painel lateral pronto.
    }

    // Função de fábrica: Recebe um texto e constrói um botão
    // com todo o estilo "madeira" customizado.
    private JButton createSidebarButton(String text){
        JButton b = new JButton(text);
        b.setAlignmentX(Component.CENTER_ALIGNMENT); // Centraliza o botão.
        b.setFocusPainted(false); // Tira o contorno azul de "foco".
        
        // Cria a borda e a cor de fundo com estilo "madeira".
        b.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(120,110,90)), // Borda escura
            BorderFactory.createEmptyBorder(8,14,8,14) // Margem interna
        ));
        b.setBackground(new Color(236, 222, 186));
        b.setOpaque(true);
        b.setCursor(new Cursor(Cursor.HAND_CURSOR)); // Muda o cursor para "mãozinha".
        
        // Adiciona um ouvinte para mudar a cor dinamicamente:
        // (quando o mouse passa por cima, ou quando é clicado).
        b.addChangeListener(e -> {
            if (b.getModel().isPressed()) b.setBackground(new Color(230, 214, 176)); // Cor de "clicado"
            else if (b.getModel().isRollover()) b.setBackground(new Color(243, 230, 197)); // Cor de "hover"
            else b.setBackground(new Color(236, 222, 186)); // Cor normal
        });
        return b;
    }

    // Classe interna (ajudante) que só existe para desenhar o fundo
    // gradiente da barra lateral (Sidebar).
    static class SidebarPanel extends JPanel {
        SidebarPanel() { setOpaque(true); }
        
        // Sobrescreve o método de desenho padrão do JPanel.
        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g); // Desenha o básico (necessário).
            Graphics2D g2 = (Graphics2D) g.create();
            int w = getWidth(), h = getHeight();
            // Define as duas cores para o gradiente (de cima para baixo).
            Color c1 = new Color(228, 200, 140); // Cor de cima (mais clara)
            Color c2 = new Color(206, 178, 118); // Cor de baixo (mais escura)
            // Pinta o fundo com o gradiente.
            g2.setPaint(new GradientPaint(0, 0, c1, 0, h, c2));
            g2.fillRect(0, 0, w, h);
            // Desenha uma linha escura sutil na borda esquerda (para separar).
            g2.setColor(new Color(180,160,110));
            g2.drawLine(0, 0, 0, h);
            g2.dispose();
        }
    }

    // Este é o "coração" do FLUXO DE VISUALIZAÇÃO (Polling).
    // É chamado a cada 500ms pelo Timer E também após cada ação (clique/botão).
    private void atualizarTela() {
        try {
            // ** A CHAMADA DE REDE (VISUALIZAÇÃO) **
            // Pede ao servidor a "foto" (snapshot) mais recente do jogo.
            EstadoJogo estadoAtual = jogoRemoto.getEstadoJogo();

            // Agora, atualiza todos os componentes visuais com os dados da "foto".
            
            // 1. Manda o PainelTabuleiro usar o novo tabuleiro vindo do servidor.
            painelTabuleiro.setTabuleiro(estadoAtual.getTabuleiro());
            // 2. Avisa o PainelTabuleiro de quem é a vez (para a sombra/preview).
            painelTabuleiro.setTurnoAtual(estadoAtual.getJogadorAtual());
            // 3. Avisa o PainelTabuleiro onde foi a última jogada (para a marcação).
            painelTabuleiro.setUltimaJogada(estadoAtual.getLastX(), estadoAtual.getLastY());
            
            // 4. Se a "foto" disse que houveram capturas...
            List<int[]> caps = estadoAtual.getUltimasCapturas();
            if (caps != null && !caps.isEmpty()) {
                // ...avisa o PainelTabuleiro para fazer a animação de "flash".
                painelTabuleiro.flashCaptures(caps);
            }

            // 5. Manda o PainelStatus atualizar (relógios, placar, turno).
            painelStatus.atualizarStatus(estadoAtual);
            
            // 6. Manda o tabuleiro se redesenhar AGORA (com os novos dados).
            painelTabuleiro.repaint();
            // 7. Reajusta o tamanho da janela (caso algo tenha mudado de tamanho).
            pack();

            // 8. Verifica se o jogo acabou (segundo o servidor).
            if (estadoAtual.isGameOver()) {
                // Se sim, chama o método que mostra o pop-up de fim de jogo.
                maybeShowGameOverDialog();
            } else {
                // Se não, reseta o controle do pop-up (para o próximo fim de jogo).
                gameOverDialogShown = false;
            }
        } catch (RemoteException e) {
            // Se a rede cair no meio do jogo, para o timer e avisa o usuário.
            timer.stop();
            JOptionPane.showMessageDialog(this, "Conexão com o servidor perdida.", "Erro de Rede", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Método de conveniência para mostrar o pop-up de fim de jogo.
    private void maybeShowGameOverDialog() {
        // Se o pop-up já está sendo mostrado, não mostre de novo (evita spam).
        if (gameOverDialogShown) return;
        // Trava para não mostrar de novo.
        gameOverDialogShown = true;
        
        // Cria o pop-up com as opções "Novo jogo" e "Fechar".
        String[] options = {"Novo jogo", "Fechar"};
        int choice = JOptionPane.showOptionDialog(
                this,
                "O jogo terminou. O que deseja fazer?",
                "Fim de jogo",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.INFORMATION_MESSAGE,
                null, options, options[0]
        );
        
        // Se o usuário escolheu "Novo jogo"...
        if (choice == JOptionPane.YES_OPTION) {
            try {
                // ...manda o servidor reiniciar o jogo.
                jogoRemoto.reiniciar();
                gameOverDialogShown = false; // Libera a trava do pop-up.
                atualizarTela(); // Atualiza a tela para ver o novo tabuleiro.
            } catch (RemoteException ex) {
                JOptionPane.showMessageDialog(this, "Erro ao reiniciar.");
            }
        } else {
            dispose();
        }
    }
}