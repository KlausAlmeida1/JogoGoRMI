package modelo;

import java.io.Serializable;
import java.util.List;

public class EstadoJogo implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Tabuleiro tabuleiro;
    private final int jogadorAtual;
    private final int pontuacaoPretas;
    private final int pontuacaoBrancas;
    private final int lastX, lastY;
    private final String lastInfo;
    private final List<int[]> ultimasCapturas;
    private final boolean gameOver;

    // >>> Tempos (ms) <<<
    private final long tempoPretasMs;
    private final long tempoBrancasMs;

    public EstadoJogo(Tabuleiro tabuleiro, int jogadorAtual, int pontPretas, int pontBrancas,
                      int lastX, int lastY, String lastInfo, List<int[]> ultimasCapturas, boolean gameOver,
                      long tempoPretasMs, long tempoBrancasMs) {
        this.tabuleiro = tabuleiro;
        this.jogadorAtual = jogadorAtual;
        this.pontuacaoPretas = pontPretas;
        this.pontuacaoBrancas = pontBrancas;
        this.lastX = lastX; this.lastY = lastY;
        this.lastInfo = lastInfo;
        this.ultimasCapturas = ultimasCapturas;
        this.gameOver = gameOver;
        this.tempoPretasMs = tempoPretasMs;
        this.tempoBrancasMs = tempoBrancasMs;
    }

    public Tabuleiro getTabuleiro(){ return tabuleiro; }
    public int getJogadorAtual(){ return jogadorAtual; }
    public int getPontuacaoPretas(){ return pontuacaoPretas; }
    public int getPontuacaoBrancas(){ return pontuacaoBrancas; }
    public int getLastX(){ return lastX; }
    public int getLastY(){ return lastY; }
    public String getLastInfo(){ return lastInfo; }
    public List<int[]> getUltimasCapturas(){ return ultimasCapturas; }
    public boolean isGameOver(){ return gameOver; }
    public long getTempoPretasMs(){ return tempoPretasMs; }
    public long getTempoBrancasMs(){ return tempoBrancasMs; }
}
