package visao;

import modelo.Tabuleiro;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.List;
import java.util.ArrayList;

/**
 * Esta classe é um painel (JPanel) que desenha o tabuleiro do jogo.
 * Ela é especializada em duas tarefas:
 * 1. Desenhar a grade, peças, sombras, e animações (no 'paintComponent').
 * 2. Traduzir os pixels do mouse em coordenadas da grade (no 'mouseToGrid').
 */
public class PainelTabuleiro extends JPanel {
    
    // Guarda a 'foto' (estado) do tabuleiro que deve ser desenhada.
    private Tabuleiro tabuleiro;
    // Define a margem em pixels entre a grade e a borda do painel.
    private final int PADDING = 36;
    // Define a distância em pixels entre as linhas da grade.
    private int tamanhoCelula = 44;
    
    // Coordenadas [x,y] de onde o mouse está (para a sombra 'preview').
    private int hoverX = -1, hoverY = -1;
    // Coordenadas [x,y] da última jogada (para a 'marcação' vermelha).
    private int lastX = -1, lastY = -1;
    // Cor do jogador atual (para a 'sombra' ter a cor certa).
    private int turnoAtual = Tabuleiro.PRETO;

    // Variáveis para controlar a animação de 'flash' das capturas.
    private long captureFlashUntil = 0L; // Timestamp de quando a animação deve parar.
    private final List<int[]> capturedStones = new ArrayList<>(); // Lista de peças a animar.

    /**
     * Construtor do painel. Roda uma vez para configurar o painel.
     */
    public PainelTabuleiro(Tabuleiro tabuleiro) {
        this.tabuleiro = tabuleiro; // Guarda o tabuleiro inicial.
        setOpaque(true); // Otimização de pintura.
        setBackground(new Color(247, 220, 153)); // Cor de fundo "madeira clara".

        // Instala o 'ouvinte' de movimento do mouse (para a 'sombra').
        addMouseMotionListener(new MouseMotionAdapter() {
            // Este código roda toda vez que o mouse se move sobre o painel.
            @Override public void mouseMoved(MouseEvent e) {
                // 1. Traduz o pixel (ex: 200,300) para a grade (ex: 4,5).
                int[] xy = mouseToGrid(e.getX(), e.getY());
                hoverX = xy[0]; // Guarda a coordenada X.
                hoverY = xy[1]; // Guarda a coordenada Y.
                
                // 2. Define o texto da "dica" (ex: "A1", "B2").
                setToolTipText(coordText(hoverX, hoverY));
                
                // 3. Manda o painel se redesenhar (para a 'sombra' seguir o mouse).
                repaint();
            }
        });
    }

    // ---- Métodos públicos chamados pela JanelaJogo ----

    // Define um tamanho fixo para a célula (em pixels).
    public void setCellSize(int px){ this.tamanhoCelula = Math.max(20, px); revalidate(); repaint(); }
    
    // Recebe a nova 'foto' do tabuleiro para desenhar.
    public void setTabuleiro(Tabuleiro t){ this.tabuleiro = t; revalidate(); repaint(); }
    
    // Recebe as coordenadas da última jogada (para a marcação vermelha).
    public void setUltimaJogada(int x, int y){ this.lastX=x; this.lastY=y; repaint(); }
    
    // Recebe a cor do turno atual (para a sombra).
    public void setTurnoAtual(int cor){ this.turnoAtual = cor; repaint(); }

    
    /**
     * Converte coordenadas da grade (ex: 0, 8) para texto (ex: "A1").
     * Usado para a dica (tooltip) do mouse.
     */
    private String coordText(int x, int y){
        if (x<0 || y<0 || x>=tabuleiro.getTamanho() || y>=tabuleiro.getTamanho()) return null;
        char letra = (char)('A' + x + (x >= 8 ? 1 : 0)); // Converte 0->A, 1->B (pula 'I').
        return letra + String.valueOf(tabuleiro.getTamanho()-y); // Converte 8->1, 7->2.
    }

    /**
     * Função principal de interação: Converte Posição do Mouse (Pixels) para Coordenada (Grade).
     * Usada tanto pelo 'mouseClicked' (na JanelaJogo) quanto pelo 'mouseMoved' (aqui).
     */
    public int[] mouseToGrid(int px, int py){
        int n = tabuleiro.getTamanho(); // Tamanho (ex: 9).

        // 1. Calcula o tamanho (em px) da célula que cabe na janela atual.
        // Isso faz a grade se ajustar se a janela for redimensionada.
        int maxCell = Math.max(20, Math.min((getWidth()  - PADDING) / Math.max(1,(n-1)),
                                            (getHeight() - PADDING) / Math.max(1,(n-1))));
        tamanhoCelula = Math.min(tamanhoCelula, maxCell); // Usa o tamanho definido (48) ou o menor, se não couber.

        // 2. Calcula o tamanho total da grade em pixels (um tabuleiro 9x9 tem 8 células: n-1).
        int boardPixels = (n - 1) * tamanhoCelula;
        
        // 3. Calcula o pixel exato do canto [0][0] (x0, y0), centralizando a grade.
        int x0 = (getWidth()  - (boardPixels + PADDING)) / 2 + PADDING/2;
        int y0 = (getHeight() - (boardPixels + PADDING)) / 2 + PADDING/2;

        // 4. Converte o pixel do mouse (px) em uma coordenada 'quebrada' (float).
        // Ex: (px=200 - x0=108) / 48 = 1.91...
        float gx = (px - x0) / (float)tamanhoCelula;
        float gy = (py - y0) / (float)tamanhoCelula;

        // 5. Arredonda o valor 'quebrado' para a interseção mais próxima.
        // Ex: 1.91 vira 2. 2.1 vira 2. É assim que o "clique perto" funciona.
        int x = Math.round(gx), y = Math.round(gy);
        
        // 6. Se o clique foi fora da grade, retorna inválido [-1, -1].
        if (x < 0 || y < 0 || x >= n || y >= n) return new int[]{-1,-1};
        
        // 7. Retorna a coordenada [x,y] da grade (ex: [2, 2]).
        return new int[]{x,y};
    }

    /**
     * Define o tamanho preferido deste painel.
     * Usado pela JanelaJogo (no 'pack()') para ajustar o tamanho da janela.
     */
    @Override public Dimension getPreferredSize() {
        int n = (tabuleiro != null) ? tabuleiro.getTamanho() : 9;
        // Tamanho da grade (8 * 48px) + margem (36px).
        int side = PADDING + (n - 1) * tamanhoCelula;
        return new Dimension(side + 80, side + 80); // Adiciona 80px de "respiro" geral.
    }

    /**
     * Ativa a animação de "flash" das peças capturadas.
     * É chamado pela JanelaJogo quando 'atualizarTela' detecta capturas.
     */
    public void flashCaptures(List<int[]> stones){
        capturedStones.clear(); // Limpa a animação anterior.
        if (stones != null) capturedStones.addAll(stones); // Adiciona as novas peças.
        // Define o "cronômetro" da animação: 350 milissegundos a partir de agora.
        captureFlashUntil = System.currentTimeMillis() + 350L;
        repaint(); // Manda redesenhar para iniciar a animação.
    }

    /**
     * Método principal de desenho. Roda toda vez que 'repaint()' é chamado.
     * Desenha tudo em camadas, de trás para frente.
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g); // Limpa o painel (obrigatório).
        int n = tabuleiro.getTamanho();
        Graphics2D g2 = (Graphics2D) g; // Usa Graphics2D para desenho de alta qualidade.
        
        // Ativa o "anti-aliasing" para linhas e círculos ficarem suaves.
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        // --- CAMADA 1: CALCULAR MEDIDAS ---
        // (Repete o cálculo de 'mouseToGrid' para garantir que o DESENHO
        // e o CLIQUE usem as mesmas medidas exatas).
        int maxCell = Math.max(20, Math.min((getWidth()  - PADDING) / Math.max(1,(n-1)),
                                            (getHeight() - PADDING) / Math.max(1,(n-1))));
        tamanhoCelula = Math.min(tamanhoCelula, maxCell);
        int boardPixels = (n - 1) * tamanhoCelula;
        int x0 = (getWidth()  - (boardPixels + PADDING)) / 2 + PADDING/2;
        int y0 = (getHeight() - (boardPixels + PADDING)) / 2 + PADDING/2;

        // --- CAMADA 2: FUNDO DE MADEIRA ---
        g2.setColor(new Color(247, 220, 153));
        g2.fillRect(x0 - PADDING/2, y0 - PADDING/2, boardPixels + PADDING, boardPixels + PADDING);

        // --- CAMADA 3: GRADE ---
        g2.setColor(new Color(70,70,70)); // Cor da linha (cinza escuro).
        g2.setStroke(new BasicStroke(1.2f)); // Espessura da linha.
        for (int i=0; i<n; i++){
            int x = x0 + i * tamanhoCelula; // Posição X da linha vertical.
            int y = y0 + i * tamanhoCelula; // Posição Y da linha horizontal.
            g2.drawLine(x, y0, x, y0 + boardPixels); // Desenha linha vertical.
            g2.drawLine(x0, y, x0 + boardPixels, y); // Desenha linha horizontal.
        }

        // --- CAMADA 4: HOSHI (PONTOS-ESTRELA) ---
        // Define as coordenadas [x,y] dos pontos de estrela baseado no tamanho.
        int[][] hoshi;
        if (n == 9) hoshi = new int[][]{{2,2},{2,6},{6,2},{6,6},{4,4}};
        else if (n == 13) hoshi = new int[][]{{3,3},{3,9},{9,3},{9,9},{6,6}};
        else if (n == 19) hoshi = new int[][]{{3,3},{3,9},{3,15},{9,3},{9,9},{9,15},{15,3},{15,9},{15,15}};
        else hoshi = new int[0][0]; // Nenhum, se for outro tamanho.
        
        // Desenha um pequeno círculo em cada coordenada hoshi.
        g2.setColor(new Color(60,60,60));
        for (int[] h : hoshi) {
            int hx = x0 + h[0]*tamanhoCelula; // Converte grade (h[0]) para pixel.
            int hy = y0 + h[1]*tamanhoCelula; // Converte grade (h[1]) para pixel.
            g2.fillOval(hx-3, hy-3, 6, 6); // Desenha o círculo de 6x6px.
        }

        // --- CAMADA 5: COORDENADAS (A, B, C... 1, 2, 3...) ---
        g2.setFont(getFont().deriveFont(Font.PLAIN, 12f));
        g2.setColor(new Color(80,80,80));
        for (int i=0;i<n;i++){
            int x = x0 + i*tamanhoCelula;
            int y = y0 + i*tamanhoCelula;
            char letra = (char)('A' + i + (i >= 8 ? 1 : 0)); // A, B... (pula 'I').
            g2.drawString(String.valueOf(letra), x-4, y0 - 8); // Desenha letras (em cima).
            g2.drawString(String.valueOf(n-i), x0 - 20, y+4); // Desenha números (na esquerda).
        }

        // --- CAMADA 6: PEÇAS (PRETAs E BRANCAs) ---
        // Loop por CADA interseção do tabuleiro.
        for (int i=0;i<n;i++){
            for (int j=0;j<n;j++){
                int cor = tabuleiro.get(i,j); // Pega a cor (0, 1 ou 2).
                
                // Se a casa NÃO ESTIVER VAZIA...
                if (cor != Tabuleiro.VAZIO){
                    // ...converte a grade (i, j) para o pixel central (cx, cy).
                    int cx = x0 + i * tamanhoCelula;
                    int cy = y0 + j * tamanhoCelula;
                    
                    // --- Efeito de "Relevo" (Gradiente) ---
                    // Define as cores (Preto: cinza->preto, Branco: branco->cinza).
                    Color base = (cor==Tabuleiro.PRETO) ? Color.BLACK : Color.WHITE;
                    Color hi   = (cor==Tabuleiro.PRETO) ? new Color(80,80,80) : new Color(235,235,235);
                    Paint old = g2.getPaint(); // Salva a "tinta" antiga.
                    
                    // Cria um efeito de luz (gradiente radial) vindo do canto.
                    RadialGradientPaint rgp = new RadialGradientPaint(
                       new Point(cx-6, cy-6), // Ponto de luz (levemente acima/esquerda).
                       (float)(tamanhoCelula*0.45), // Raio do brilho.
                       new float[]{0f, 1f}, // Posições (0=centro, 1=borda).
                       new Color[]{hi, base}); // Cores (do "brilho" para a "base").
                    
                    g2.setPaint(rgp); // Usa o gradiente como "tinta".
                    
                    // Calcula o diâmetro da peça (72% do espaço da célula).
                    int d = (int)(tamanhoCelula*0.72);
                    // Desenha a peça com o gradiente.
                    g2.fillOval(cx-d/2, cy-d/2, d, d);
                    
                    // Desenha uma borda escura sutil na peça.
                    g2.setPaint(old); // Restaura a "tinta" normal (cor sólida).
                    g2.setColor(new Color(0,0,0,130)); // Preto semi-transparente.
                    g2.drawOval(cx-d/2, cy-d/2, d, d);
                }
            }
        }

        // --- CAMADA 7: MARCAÇÃO DA ÚLTIMA JOGADA ---
        // Se a JanelaJogo nos disse onde foi a última jogada...
        if (lastX >= 0 && lastY >= 0){
            // ...converte a grade (lastX, lastY) para pixel (cx, cy).
            int cx = x0 + lastX*tamanhoCelula;
            int cy = y0 + lastY*tamanhoCelula;
            // ...desenha um pequeno círculo VERMELHO no centro da peça.
            g2.setColor(new Color(220,20,60));
            g2.fillOval(cx-4, cy-4, 8, 8);
        }

        // --- CAMADA 8: "SOMBRA" DO MOUSE (PREVIEW) ---
        // Se o mouse está sobre uma coordenada válida (hoverX/Y) E a casa está VAZIA...
        if (hoverX >= 0 && hoverY >= 0 && tabuleiro.get(hoverX, hoverY) == Tabuleiro.VAZIO){
            // ...converte a grade (hoverX, hoverY) para pixel (cx, cy).
            int cx = x0 + hoverX*tamanhoCelula;
            int cy = y0 + hoverY*tamanhoCelula;
            // ...define a cor da sombra (cor do turno atual, mas transparente).
            g2.setColor(turnoAtual==Tabuleiro.PRETO ? new Color(0,0,0,90) : new Color(255,255,255,130));
            int d = (int)(tamanhoCelula*0.70); // Diâmetro (70%).
            // ...desenha a sombra.
            g2.fillOval(cx-d/2, cy-d/2, d, d);
            g2.setColor(new Color(0,0,0,100)); // Borda sutil na sombra.
            g2.drawOval(cx-d/2, cy-d/2, d, d);
        }

        // --- CAMADA 9: ANIMAÇÃO DE CAPTURA (FLASH) ---
        long now = System.currentTimeMillis();
        // Se o "cronômetro" da animação (350ms) ainda estiver rodando...
        if (now < captureFlashUntil){
            // ...calcula a transparência (alpha) para o efeito de "fade out".
            float alpha = (captureFlashUntil-now)/350f;
            // ...define a cor (vermelho transparente, baseado no 'alpha').
            g2.setColor(new Color(255,80,80,(int)(140*alpha)));
            
            // ...para cada peça capturada que está na lista...
            for (int[] p : capturedStones){
                // ...converte a grade (p[0], p[1]) para pixel (cx, cy).
                int cx = x0 + p[0]*tamanhoCelula;
                int cy = y0 + p[1]*tamanhoCelula;
                int r = (int)(tamanhoCelula*0.80); // Tamanho do flash.
                // ...desenha o círculo vermelho do flash.
                g2.fillOval(cx-r/2, cy-r/2, r, r);
            }
            // Manda redesenhar de novo IMEDIATAMENTE.
            // Isso cria o "loop" da animação, fazendo o flash apagar (fade out).
            repaint();
        }
    }

    // ---- Getters (Ajudantes) ----
    
    // Permite que a 'JanelaJogo' pergunte qual é a margem (PADDING).
    // Usado pela JanelaJogo para o cálculo do clique.
    public int getPADDING(){ return PADDING; }
    
    // Permite que a 'JanelaJogo' pergunte qual é o tamanho da célula.
    // Usado pela JanelaJogo para o cálculo do clique.
    public int getTAMANHO_CELULA(){ return tamanhoCelula; }
}