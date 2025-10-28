package visao;

import modelo.Tabuleiro;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.List;
import java.util.ArrayList;

public class PainelTabuleiro extends JPanel {
    private Tabuleiro tabuleiro;
    private final int PADDING = 36;    // margem externa (em px) do bloco tabuleiro
    private int tamanhoCelula = 44;    // será recalculado se caber maior/menor
    private int hoverX = -1, hoverY = -1;
    private int lastX = -1, lastY = -1;
    private int turnoAtual = Tabuleiro.PRETO;

    // animação simples de capturas
    private long captureFlashUntil = 0L;
    private final List<int[]> capturedStones = new ArrayList<>();

    public PainelTabuleiro(Tabuleiro tabuleiro) {
        this.tabuleiro = tabuleiro;
        setOpaque(true);
        setBackground(new Color(247, 220, 153)); // madeira clara

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override public void mouseMoved(MouseEvent e) {
                int[] xy = mouseToGrid(e.getX(), e.getY());
                hoverX = xy[0]; hoverY = xy[1];
                setToolTipText(coordText(hoverX, hoverY));
                repaint();
            }
        });
    }

    // ---- API chamada pela Janela ----
    public void setCellSize(int px){ this.tamanhoCelula = Math.max(20, px); revalidate(); repaint(); }
    public void setTabuleiro(Tabuleiro t){ this.tabuleiro = t; revalidate(); repaint(); }
    public void setUltimaJogada(int x, int y){ this.lastX=x; this.lastY=y; repaint(); }
    public void setTurnoAtual(int cor){ this.turnoAtual = cor; repaint(); }

    /** entrega coordenada A.. / 1.. como dica */
    private String coordText(int x, int y){
        if (x<0 || y<0 || x>=tabuleiro.getTamanho() || y>=tabuleiro.getTamanho()) return null;
        char letra = (char)('A' + x + (x >= 8 ? 1 : 0)); // pular 'I'
        return letra + String.valueOf(tabuleiro.getTamanho()-y);
    }

    /** Conversão mouse → grade usando a MESMA origem do desenho */
    public int[] mouseToGrid(int px, int py){
        int n = tabuleiro.getTamanho();
        int side = Math.min(getWidth(), getHeight());
        // escolhe o maior tamanho de célula que caiba, mantendo PADDING
        int maxCell = Math.max(20, Math.min((getWidth()  - PADDING) / Math.max(1,(n-1)),
                                            (getHeight() - PADDING) / Math.max(1,(n-1))));
        tamanhoCelula = Math.min(tamanhoCelula, maxCell); // respeita setCellSize() se menor
        int boardPixels = (n - 1) * tamanhoCelula;
        int x0 = (getWidth()  - (boardPixels + PADDING)) / 2 + PADDING/2;
        int y0 = (getHeight() - (boardPixels + PADDING)) / 2 + PADDING/2;

        float gx = (px - x0) / (float)tamanhoCelula;
        float gy = (py - y0) / (float)tamanhoCelula;
        int x = Math.round(gx), y = Math.round(gy);
        if (x < 0 || y < 0 || x >= n || y >= n) return new int[]{-1,-1};
        return new int[]{x,y};
    }

    @Override public Dimension getPreferredSize() {
        int n = (tabuleiro != null) ? tabuleiro.getTamanho() : 9;
        int side = PADDING + (n - 1) * tamanhoCelula;
        return new Dimension(side + 80, side + 80); // um respiro extra
    }

    /** recebe capturas p/ efeito flash */
    public void flashCaptures(List<int[]> stones){
        capturedStones.clear();
        if (stones != null) capturedStones.addAll(stones);
        captureFlashUntil = System.currentTimeMillis() + 350L;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        int n = tabuleiro.getTamanho();
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        // recalcula célula para caber
        int maxCell = Math.max(20, Math.min((getWidth()  - PADDING) / Math.max(1,(n-1)),
                                            (getHeight() - PADDING) / Math.max(1,(n-1))));
        tamanhoCelula = Math.min(tamanhoCelula, maxCell);
        int boardPixels = (n - 1) * tamanhoCelula;
        int x0 = (getWidth()  - (boardPixels + PADDING)) / 2 + PADDING/2;
        int y0 = (getHeight() - (boardPixels + PADDING)) / 2 + PADDING/2;

        // madeira do tabuleiro
        g2.setColor(new Color(247, 220, 153));
        g2.fillRect(x0 - PADDING/2, y0 - PADDING/2, boardPixels + PADDING, boardPixels + PADDING);

        // grade
        g2.setColor(new Color(70,70,70));
        g2.setStroke(new BasicStroke(1.2f));
        for (int i=0; i<n; i++){
            int x = x0 + i * tamanhoCelula;
            int y = y0 + i * tamanhoCelula;
            g2.drawLine(x, y0, x, y0 + boardPixels);
            g2.drawLine(x0, y, x0 + boardPixels, y);
        }

        // hoshi (pontos-estrela)
        int[][] hoshi;
        if (n == 9) hoshi = new int[][]{{2,2},{2,6},{6,2},{6,6},{4,4}};
        else if (n == 13) hoshi = new int[][]{{3,3},{3,9},{9,3},{9,9},{6,6}};
        else if (n == 19) hoshi = new int[][]{{3,3},{3,9},{3,15},{9,3},{9,9},{9,15},{15,3},{15,9},{15,15}};
        else hoshi = new int[0][0];
        g2.setColor(new Color(60,60,60));
        for (int[] h : hoshi) {
            int hx = x0 + h[0]*tamanhoCelula;
            int hy = y0 + h[1]*tamanhoCelula;
            g2.fillOval(hx-3, hy-3, 6, 6);
        }

        // coordenadas
        g2.setFont(getFont().deriveFont(Font.PLAIN, 12f));
        g2.setColor(new Color(80,80,80));
        for (int i=0;i<n;i++){
            int x = x0 + i*tamanhoCelula;
            int y = y0 + i*tamanhoCelula;
            char letra = (char)('A' + i + (i >= 8 ? 1 : 0));
            g2.drawString(String.valueOf(letra), x-4, y0 - 8);
            g2.drawString(String.valueOf(n-i), x0 - 20, y+4);
        }

        // pedras (com relevo)
        for (int i=0;i<n;i++){
            for (int j=0;j<n;j++){
                int cor = tabuleiro.get(i,j);
                if (cor != Tabuleiro.VAZIO){
                    int cx = x0 + i * tamanhoCelula;
                    int cy = y0 + j * tamanhoCelula;
                    Color base = (cor==Tabuleiro.PRETO) ? Color.BLACK : Color.WHITE;
                    Color hi   = (cor==Tabuleiro.PRETO) ? new Color(80,80,80) : new Color(235,235,235);
                    Paint old = g2.getPaint();
                    RadialGradientPaint rgp = new RadialGradientPaint(
                       new Point(cx-6, cy-6), (float)(tamanhoCelula*0.45),
                       new float[]{0f, 1f}, new Color[]{hi, base});
                    g2.setPaint(rgp);
                    int d = (int)(tamanhoCelula*0.72);
                    g2.fillOval(cx-d/2, cy-d/2, d, d);
                    g2.setPaint(old);
                    g2.setColor(new Color(0,0,0,130));
                    g2.drawOval(cx-d/2, cy-d/2, d, d);
                }
            }
        }

        // última jogada
        if (lastX >= 0 && lastY >= 0){
            int cx = x0 + lastX*tamanhoCelula;
            int cy = y0 + lastY*tamanhoCelula;
            g2.setColor(new Color(220,20,60));
            g2.fillOval(cx-4, cy-4, 8, 8);
        }

        // hover pré-visualização na cor do turno (se casa vazia)
        if (hoverX >= 0 && hoverY >= 0 && tabuleiro.get(hoverX, hoverY) == Tabuleiro.VAZIO){
            int cx = x0 + hoverX*tamanhoCelula;
            int cy = y0 + hoverY*tamanhoCelula;
            g2.setColor(turnoAtual==Tabuleiro.PRETO ? new Color(0,0,0,90) : new Color(255,255,255,130));
            int d = (int)(tamanhoCelula*0.70);
            g2.fillOval(cx-d/2, cy-d/2, d, d);
            g2.setColor(new Color(0,0,0,100));
            g2.drawOval(cx-d/2, cy-d/2, d, d);
        }

        // flash de capturas
        long now = System.currentTimeMillis();
        if (now < captureFlashUntil){
            float alpha = (captureFlashUntil-now)/350f;
            g2.setColor(new Color(255,80,80,(int)(140*alpha)));
            for (int[] p : capturedStones){
                int cx = x0 + p[0]*tamanhoCelula;
                int cy = y0 + p[1]*tamanhoCelula;
                int r = (int)(tamanhoCelula*0.80);
                g2.fillOval(cx-r/2, cy-r/2, r, r);
            }
            repaint(); // anima
        }
    }

    public int getPADDING(){ return PADDING; }
    public int getTAMANHO_CELULA(){ return tamanhoCelula; }
}
