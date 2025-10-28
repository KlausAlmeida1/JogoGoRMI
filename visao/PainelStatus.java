package visao;

import javax.swing.*;
import java.awt.*;
import modelo.EstadoJogo;
import modelo.Tabuleiro;

public class PainelStatus extends JPanel {
    private final boolean isServidor;

    // identidade fixa
    private final MyBadge myBadge;

    // relógios
    private final ClockBadge clockPretas = new ClockBadge(true);
    private final ClockBadge clockBrancas = new ClockBadge(false);

    // componentes centrais
    private final JLabel turnoIcon = new JLabel();
    private final CountBadge pretasBadge = new CountBadge(true);
    private final CountBadge brancasBadge = new CountBadge(false);

    // info à direita
    private final JLabel infoLabel = new JLabel();

    public PainelStatus(boolean isServidor) {
        this.isServidor = isServidor;
        setOpaque(true);
        setLayout(new BorderLayout());

        int minhaCor = isServidor ? Tabuleiro.PRETO : Tabuleiro.BRANCO;
        myBadge = new MyBadge(minhaCor);

        // --- ESQUERDA: badge "VOCÊ" + relógio da sua cor ---
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 6)) { @Override public boolean isOpaque(){ return false; } };
        left.add(myBadge);
        left.add(minhaCor==Tabuleiro.PRETO ? clockPretas : clockBrancas);
        add(left, BorderLayout.WEST);

        // --- CENTRO: prisioneiros e ícone do turno ---
        JPanel center = new JPanel() { @Override public boolean isOpaque(){ return false; } };
        center.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = 0; c.insets = new Insets(4, 10, 4, 10);

        turnoIcon.setIcon(new StoneIcon(Color.BLACK, 22));
        pretasBadge.setPreferredSize(new Dimension(70, 24));
        brancasBadge.setPreferredSize(new Dimension(70, 24));

        c.gridx = 0; center.add(pretasBadge, c);
        c.gridx = 1; center.add(turnoIcon, c);
        c.gridx = 2; center.add(brancasBadge, c);

        // --- DIREITA: relógio da outra cor + info ---
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 6)) { @Override public boolean isOpaque(){ return false; } };
        right.add(minhaCor==Tabuleiro.PRETO ? clockBrancas : clockPretas);
        infoLabel.setFont(infoLabel.getFont().deriveFont(Font.PLAIN, 12f));
        infoLabel.setForeground(new Color(70, 70, 70));
        right.add(infoLabel);

        add(center, BorderLayout.CENTER);
        add(right, BorderLayout.EAST);
        setPreferredSize(new Dimension(100, 48));
    }

    public void atualizarStatus(EstadoJogo estado) {
        boolean turnoPretas = (estado.getJogadorAtual() == Tabuleiro.PRETO);
        turnoIcon.setIcon(new StoneIcon(turnoPretas ? Color.BLACK : Color.WHITE, 22));

        // prisioneiros
        pretasBadge.setCount(estado.getPontuacaoPretas());
        brancasBadge.setCount(estado.getPontuacaoBrancas());

        // relógios (ms -> mm:ss)
        clockPretas.setMillis(estado.getTempoPretasMs(), turnoPretas);
        clockBrancas.setMillis(estado.getTempoBrancasMs(), !turnoPretas);

        // badge “VOCÊ” ativo se for sua vez
        boolean euSouPretas = isServidor;
        boolean meuTurno = (euSouPretas && turnoPretas) || (!euSouPretas && !turnoPretas);
        myBadge.setActive(meuTurno && !estado.isGameOver());

        String info = estado.getLastInfo();
        if (estado.isGameOver()) {
            infoLabel.setText((info != null ? info + " " : "") + "Fim de jogo.");
            infoLabel.setForeground(new Color(100, 70, 40));
        } else {
            infoLabel.setText(info != null ? info : "");
            infoLabel.setForeground(new Color(70, 70, 70));
        }

        repaint();
    }

    // --- fundo gradiente como a sidebar ---
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        int w = getWidth(), h = getHeight();
        Color c1 = new Color(228, 200, 140);
        Color c2 = new Color(206, 178, 118);
        g2.setPaint(new GradientPaint(0, 0, c1, 0, h, c2));
        g2.fillRect(0, 0, w, h);
        g2.dispose();
    }

    // ===== Auxiliares visuais (StoneIcon, MyBadge, CountBadge, ClockBadge) =====
    static class StoneIcon implements Icon {
        private final Color base; private final int d;
        StoneIcon(Color base, int diameter){ this.base = base; this.d = diameter; }
        @Override public int getIconWidth() { return d; }
        @Override public int getIconHeight() { return d; }
        @Override public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color hi = base.equals(Color.BLACK) ? new Color(80,80,80) : new Color(235,235,235);
            RadialGradientPaint rgp = new RadialGradientPaint(new Point(x + d/2 - 5, y + d/2 - 5), d*0.55f,
                    new float[]{0f, 1f}, new Color[]{hi, base});
            Paint old = g2.getPaint();
            g2.setPaint(rgp); g2.fillOval(x, y, d, d);
            g2.setPaint(old); g2.setColor(new Color(0,0,0,150)); g2.drawOval(x, y, d, d);
            g2.dispose();
        }
    }

    static class MyBadge extends JComponent {
        private final boolean pretas; private boolean active=false;
        MyBadge(int cor){ this.pretas=(cor==Tabuleiro.PRETO); setOpaque(false); }
        void setActive(boolean a){ this.active=a; repaint(); }
        @Override public Dimension getPreferredSize(){ return new Dimension(120, 36); }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(255,255,255,90)); g2.fillRoundRect(0,4,getWidth(),getHeight()-8,14,14);
            if(active){ g2.setColor(new Color(120,170,120)); g2.setStroke(new BasicStroke(2f));
                        g2.drawRoundRect(0,4,getWidth()-1,getHeight()-9,14,14); }
            int d=18,cx=10,cy=(getHeight()-d)/2;
            g2.setColor(pretas?Color.BLACK:Color.WHITE); g2.fillOval(cx,cy,d,d);
            g2.setColor(new Color(0,0,0,140)); g2.drawOval(cx,cy,d,d);
            g2.setFont(getFont().deriveFont(Font.BOLD,12f)); g2.setColor(new Color(60,60,60));
            g2.drawString("VOCÊ", cx+d+8, cy+d-4);
            g2.dispose();
        }
    }

    static class CountBadge extends JComponent {
        private int count=0; private final boolean pretas;
        CountBadge(boolean p){ this.pretas=p; setOpaque(false); }
        void setCount(int c){ this.count=Math.max(0,c); repaint(); }
        @Override public Dimension getPreferredSize(){ return new Dimension(70,24); }
        @Override protected void paintComponent(Graphics g){
            Graphics2D g2=(Graphics2D)g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(255,255,255,80)); g2.fillRoundRect(0,2,getWidth(),getHeight()-4,12,12);
            int d=14,cx=8,cy=(getHeight()-d)/2;
            g2.setColor(pretas?Color.BLACK:Color.WHITE); g2.fillOval(cx,cy,d,d);
            g2.setColor(new Color(0,0,0,140)); g2.drawOval(cx,cy,d,d);
            g2.setFont(getFont().deriveFont(Font.BOLD,12f)); g2.setColor(new Color(60,60,60));
            g2.drawString("x"+count, cx+d+6, cy+d-2);
            g2.dispose();
        }
    }

    /** Relógio mm:ss com destaque ao jogador da vez e alerta <10s */
    static class ClockBadge extends JComponent {
        private long millis = 0;
        private boolean ativo = false;
        private final boolean pretas;
        ClockBadge(boolean pretas){ this.pretas=pretas; setOpaque(false); }
        void setMillis(long ms, boolean ativo){ this.millis = Math.max(0, ms); this.ativo = ativo; repaint(); }
        @Override public Dimension getPreferredSize(){ return new Dimension(74, 26); }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2=(Graphics2D)g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            boolean low = millis <= 10_000;
            Color fill = new Color(255,255,255,80);
            Color border = new Color(120,110,90);
            if (ativo) border = new Color(90,120,90);
            if (low && ativo) fill = new Color(255, 220, 220, 180);

            g2.setColor(fill); g2.fillRoundRect(0,4,getWidth(),getHeight()-8,12,12);
            g2.setColor(border); g2.drawRoundRect(0,4,getWidth()-1,getHeight()-9,12,12);

            // pedra pequena
            int d=12,cx=8,cy=(getHeight()-d)/2;
            g2.setColor(pretas?Color.BLACK:Color.WHITE); g2.fillOval(cx,cy,d,d);
            g2.setColor(new Color(0,0,0,140)); g2.drawOval(cx,cy,d,d);

            // tempo
            long total = millis/1000;
            long mm = total/60, ss = total%60;
            String txt = String.format("%d:%02d", mm, ss);
            g2.setFont(getFont().deriveFont(Font.BOLD, 12f));
            g2.setColor(low ? new Color(160,40,40) : new Color(60,60,60));
            g2.drawString(txt, cx + d + 6, cy + d - 1);

            g2.dispose();
        }
    }
}
