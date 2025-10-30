package visao;

import javax.swing.*;
import java.awt.*;
import modelo.EstadoJogo;
import modelo.Tabuleiro;

/**
 * Esta classe é o painel de status (placar, relógio) que fica na parte
 * inferior da janela.
 *
 * É um "display" burro: ele não faz cálculos, apenas recebe o
 * 'EstadoJogo' (a "foto" do jogo) e atualiza seus componentes visuais
 * (badges, relógios, ícones) para refletir essa "foto".
 */
public class PainelStatus extends JPanel {
    private final boolean isServidor;

    // ---- Componentes Visuais (os "displays" de informação) ----
    
    // Mostra "VOCÊ" e sua cor.
    private final MyBadge myBadge;

    // Relógios individuais para cada cor.
    private final ClockBadge clockPretas = new ClockBadge(true);
    private final ClockBadge clockBrancas = new ClockBadge(false);

    // Ícone (pedra preta/branca) que mostra de quem é o turno.
    private final JLabel turnoIcon = new JLabel();
    // Contadores de peças capturadas (prisioneiros).
    private final CountBadge pretasBadge = new CountBadge(true);
    private final CountBadge brancasBadge = new CountBadge(false);

    // Mostra mensagens de texto (ex: "Fim de jogo", "Não é seu turno").
    private final JLabel infoLabel = new JLabel();

    /**
     * Construtor: Roda UMA VEZ para montar o painel de status.
     * Ele organiza todos os componentes visuais (badges, relógios)
     * usando um layout (BorderLayout).
     */
    public PainelStatus(boolean isServidor) {
        this.isServidor = isServidor;
        setOpaque(true); // Otimização de pintura.
        setLayout(new BorderLayout()); // Layout principal (Oeste, Centro, Leste).

        // Descobre qual cor (PRETO ou BRANCO) este jogador é.
        int minhaCor = isServidor ? Tabuleiro.PRETO : Tabuleiro.BRANCO;
        myBadge = new MyBadge(minhaCor); // Cria o badge "VOCÊ".

        // ---- Monta o Lado Esquerdo (WEST) ----
        // (Mostra o badge "VOCÊ" e o relógio da SUA cor).
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 6)) { @Override public boolean isOpaque(){ return false; } };
        left.add(myBadge);
        left.add(minhaCor==Tabuleiro.PRETO ? clockPretas : clockBrancas);
        add(left, BorderLayout.WEST);

        // ---- Monta o Centro (CENTER) ----
        // (Mostra os placares de captura e o ícone de turno).
        JPanel center = new JPanel() { @Override public boolean isOpaque(){ return false; } };
        center.setLayout(new GridBagLayout()); // Layout para centralizar perfeitamente.
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = 0; c.insets = new Insets(4, 10, 4, 10); // Espaçamento.

        turnoIcon.setIcon(new StoneIcon(Color.BLACK, 22)); // Ícone inicial.
        pretasBadge.setPreferredSize(new Dimension(70, 24));
        brancasBadge.setPreferredSize(new Dimension(70, 24));

        c.gridx = 0; center.add(pretasBadge, c); // Placar Pretas
        c.gridx = 1; center.add(turnoIcon, c);   // Ícone de Turno
        c.gridx = 2; center.add(brancasBadge, c); // Placar Brancas
        add(center, BorderLayout.CENTER);

        // ---- Monta o Lado Direito (EAST) ----
        // (Mostra o relógio do OPONENTE e as mensagens de info).
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 6)) { @Override public boolean isOpaque(){ return false; } };
        right.add(minhaCor==Tabuleiro.PRETO ? clockBrancas : clockPretas);
        infoLabel.setFont(infoLabel.getFont().deriveFont(Font.PLAIN, 12f));
        infoLabel.setForeground(new Color(70, 70, 70));
        right.add(infoLabel);
        add(right, BorderLayout.EAST);
        
        // Define a altura preferida do painel de status.
        setPreferredSize(new Dimension(100, 48));
    }

    /**
     * O "Coração" do Painel: Atualiza todos os componentes visuais.
     * É chamado pela JanelaJogo (no 'atualizarTela') toda vez que
     * o Timer dispara ou uma jogada é feita.
     */
    public void atualizarStatus(EstadoJogo estado) {
        // 1. Atualiza o ÍCONE DE TURNO (pedra preta ou branca).
        boolean turnoPretas = (estado.getJogadorAtual() == Tabuleiro.PRETO);
        turnoIcon.setIcon(new StoneIcon(turnoPretas ? Color.BLACK : Color.WHITE, 22));

        // 2. Atualiza os PLACARES de prisioneiros.
        pretasBadge.setCount(estado.getPontuacaoPretas());
        brancasBadge.setCount(estado.getPontuacaoBrancas());

        // 3. Atualiza os RELÓGIOS.
        // Envia os milissegundos e quem está "ativo" (para o destaque).
        clockPretas.setMillis(estado.getTempoPretasMs(), turnoPretas);
        clockBrancas.setMillis(estado.getTempoBrancasMs(), !turnoPretas);

        // 4. Atualiza o BADGE "VOCÊ" (dá o destaque verde se for sua vez).
        boolean euSouPretas = isServidor;
        boolean meuTurno = (euSouPretas && turnoPretas) || (!euSouPretas && !turnoPretas);
        myBadge.setActive(meuTurno && !estado.isGameOver());

        // 5. Atualiza a MENSAGEM de informação.
        String info = estado.getLastInfo();
        if (estado.isGameOver()) {
            // Se o jogo acabou, mostra a mensagem de fim de jogo.
            infoLabel.setText((info != null ? info + " " : "") + "Fim de jogo.");
            infoLabel.setForeground(new Color(100, 70, 40)); // Cor diferente
        } else {
            // Se o jogo está rolando, mostra a info da última jogada (ou nada).
            infoLabel.setText(info != null ? info : "");
            infoLabel.setForeground(new Color(70, 70, 70)); // Cor normal
        }

        // Manda o painel se redesenhar com os novos dados.
        repaint();
    }

    /**
     * Sobrescreve o método de desenho padrão para pintar o
     * fundo com o gradiente "madeira", igual à Sidebar.
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g); // Limpa o painel.
        Graphics2D g2 = (Graphics2D) g.create();
        int w = getWidth(), h = getHeight();
        // Define as cores (de cima para baixo).
        Color c1 = new Color(228, 200, 140);
        Color c2 = new Color(206, 178, 118);
        // Pinta o gradiente.
        g2.setPaint(new GradientPaint(0, 0, c1, 0, h, c2));
        g2.fillRect(0, 0, w, h);
        g2.dispose();
    }

    // ======================================================================
    // ---- Classes Internas (Componentes Visuais Customizados) ----
    // Estas classes são "ajudantes" que o PainelStatus usa.
    // ======================================================================

    /**
     * Classe ajudante que desenha um ÍCONE de pedra (preta ou branca)
     * com efeito de gradiente/luz.
     * Usada pelo ícone de turno.
     */
    static class StoneIcon implements Icon {
        private final Color base; private final int d;
        StoneIcon(Color base, int diameter){ this.base = base; this.d = diameter; }
        @Override public int getIconWidth() { return d; }
        @Override public int getIconHeight() { return d; }
        @Override public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            // Define a cor do "brilho" (cinza claro para preto, branco para branco).
            Color hi = base.equals(Color.BLACK) ? new Color(80,80,80) : new Color(235,235,235);
            // Desenha a pedra com o efeito de brilho (gradiente).
            RadialGradientPaint rgp = new RadialGradientPaint(new Point(x + d/2 - 5, y + d/2 - 5), d*0.55f,
                    new float[]{0f, 1f}, new Color[]{hi, base});
            Paint old = g2.getPaint();
            g2.setPaint(rgp); g2.fillOval(x, y, d, d);
            g2.setPaint(old); g2.setColor(new Color(0,0,0,150)); g2.drawOval(x, y, d, d);
            g2.dispose();
        }
    }

    /**
     * Classe ajudante que desenha o "badge" (crachá) "VOCÊ".
     * Ele mostra a cor do jogador e fica com uma borda verde quando
     * é a vez desse jogador.
     */
    static class MyBadge extends JComponent {
        private final boolean pretas; private boolean active=false;
        MyBadge(int cor){ this.pretas=(cor==Tabuleiro.PRETO); setOpaque(false); }
        // Método chamado por 'atualizarStatus' para ligar/desligar o destaque.
        void setActive(boolean a){ this.active=a; repaint(); }
        @Override public Dimension getPreferredSize(){ return new Dimension(120, 36); }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            // Desenha o fundo transparente do crachá.
            g2.setColor(new Color(255,255,255,90)); g2.fillRoundRect(0,4,getWidth(),getHeight()-8,14,14);
            // Se estiver "ativo" (é minha vez), desenha a borda VERDE.
            if(active){ g2.setColor(new Color(120,170,120)); g2.setStroke(new BasicStroke(2f));
                        g2.drawRoundRect(0,4,getWidth()-1,getHeight()-9,14,14); }
            // Desenha a pedra (minha cor).
            int d=18,cx=10,cy=(getHeight()-d)/2;
            g2.setColor(pretas?Color.BLACK:Color.WHITE); g2.fillOval(cx,cy,d,d);
            g2.setColor(new Color(0,0,0,140)); g2.drawOval(cx,cy,d,d);
            // Desenha o texto "VOCÊ".
            g2.setFont(getFont().deriveFont(Font.BOLD,12f)); g2.setColor(new Color(60,60,60));
            g2.drawString("VOCÊ", cx+d+8, cy+d-4);
            g2.dispose();
        }
    }

    /**
     * Classe ajudante que desenha o "badge" (crachá) do placar.
     * Mostra uma pedrinha (preta ou branca) e o texto "x 0" (o contador).
     */
    static class CountBadge extends JComponent {
        private int count=0; private final boolean pretas;
        CountBadge(boolean p){ this.pretas=p; setOpaque(false); }
        // Método chamado por 'atualizarStatus' para definir o novo placar.
        void setCount(int c){ this.count=Math.max(0,c); repaint(); }
        @Override public Dimension getPreferredSize(){ return new Dimension(70,24); }
        @Override protected void paintComponent(Graphics g){
            Graphics2D g2=(Graphics2D)g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            // Desenha o fundo transparente.
            g2.setColor(new Color(255,255,255,80)); g2.fillRoundRect(0,2,getWidth(),getHeight()-4,12,12);
            // Desenha a pedra.
            int d=14,cx=8,cy=(getHeight()-d)/2;
            g2.setColor(pretas?Color.BLACK:Color.WHITE); g2.fillOval(cx,cy,d,d);
            g2.setColor(new Color(0,0,0,140)); g2.drawOval(cx,cy,d,d);
            // Desenha o texto do placar (ex: "x5").
            g2.setFont(getFont().deriveFont(Font.BOLD,12f)); g2.setColor(new Color(60,60,60));
            g2.drawString("x"+count, cx+d+6, cy+d-2);
            g2.dispose();
        }
    }

    /**
     * Classe ajudante que desenha o "badge" (crachá) do Relógio.
     * Converte milissegundos para "mm:ss" e muda de cor (verde, vermelho)
     * se estiver ativo ou com pouco tempo.
     */
    static class ClockBadge extends JComponent {
        private long millis = 0;
        private boolean ativo = false;
        private final boolean pretas;
        ClockBadge(boolean pretas){ this.pretas=pretas; setOpaque(false); }
        // Método chamado por 'atualizarStatus' para definir o novo tempo.
        void setMillis(long ms, boolean ativo){ this.millis = Math.max(0, ms); this.ativo = ativo; repaint(); }
        @Override public Dimension getPreferredSize(){ return new Dimension(74, 26); }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2=(Graphics2D)g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // ---- Lógica das Cores ----
            boolean low = millis <= 10_000; // Tempo está abaixo de 10s?
            Color fill = new Color(255,255,255,80); // Fundo normal.
            Color border = new Color(120,110,90); // Borda normal.
            if (ativo) border = new Color(90,120,90); // Borda VERDE (ativo).
            if (low && ativo) fill = new Color(255, 220, 220, 180); // Fundo VERMELHO (ativo e pouco tempo).

            // Desenha o fundo e a borda.
            g2.setColor(fill); g2.fillRoundRect(0,4,getWidth(),getHeight()-8,12,12);
            g2.setColor(border); g2.drawRoundRect(0,4,getWidth()-1,getHeight()-9,12,12);

            // Desenha a pedra pequena (preta ou branca).
            int d=12,cx=8,cy=(getHeight()-d)/2;
            g2.setColor(pretas?Color.BLACK:Color.WHITE); g2.fillOval(cx,cy,d,d);
            g2.setColor(new Color(0,0,0,140)); g2.drawOval(cx,cy,d,d);

            // ---- Lógica do Tempo (mm:ss) ----
            long total = millis/1000; // Converte ms para segundos.
            long mm = total/60; // Pega os minutos.
            long ss = total%60; // Pega os segundos restantes.
            String txt = String.format("%d:%02d", mm, ss); // Formata (ex: "5:03").
            
            // Define a cor do texto (vermelho se pouco tempo, senão normal).
            g2.setFont(getFont().deriveFont(Font.BOLD, 12f));
            g2.setColor(low ? new Color(160,40,40) : new Color(60,60,60));
            // Desenha o texto do relógio.
            g2.drawString(txt, cx + d + 6, cy + d - 1);

            g2.dispose();
        }
    }
}