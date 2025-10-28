package modelo;

import java.util.ArrayList;
import java.util.List;

public class Jogo {
    private final Tabuleiro tabuleiro;
    private int jogadorAtual;

    // placar por capturas (prisioneiros)
    private int prisioneirosPretas;   // pretas capturaram
    private int prisioneirosBrancas;  // brancas capturaram

    // ko simples
    private String ultimoHash = null;

    // última jogada e info
    private int lastX = -1, lastY = -1;
    private String lastInfo = null;
    private List<int[]> ultimasCapturas = List.of();

    // fim de jogo
    private boolean gameOver = false;
    private int consecutivePasses = 0;

    // >>> Controle de tempo <<<
    private final long tempoInicialMs;       // por jogador
    private long tempoPretasRestanteMs;
    private long tempoBrancasRestanteMs;
    private long turnoIniciadoEmMs;          // timestamp do início do turno atual

    public Jogo(int tamanhoTabuleiro) {
        this(tamanhoTabuleiro, 5 * 60_000L); // padrão: 5 minutos por lado
    }

    public Jogo(int tamanhoTabuleiro, long tempoInicialMs) {
        tabuleiro = new Tabuleiro(tamanhoTabuleiro);
        jogadorAtual = Tabuleiro.PRETO;
        this.tempoInicialMs = tempoInicialMs;
        this.tempoPretasRestanteMs  = tempoInicialMs;
        this.tempoBrancasRestanteMs = tempoInicialMs;
        this.turnoIniciadoEmMs = System.currentTimeMillis();
    }

    // ----- Tempo -----
    private void descontarTempoAteAgora() {
        if (gameOver) return;
        long agora = System.currentTimeMillis();
        long decorrido = Math.max(0, agora - turnoIniciadoEmMs);
        if (jogadorAtual == Tabuleiro.PRETO) {
            tempoPretasRestanteMs = Math.max(0, tempoPretasRestanteMs - decorrido);
            if (tempoPretasRestanteMs == 0) { gameOver = true; lastInfo = "Tempo esgotado para Pretas."; }
        } else {
            tempoBrancasRestanteMs = Math.max(0, tempoBrancasRestanteMs - decorrido);
            if (tempoBrancasRestanteMs == 0) { gameOver = true; lastInfo = "Tempo esgotado para Brancas."; }
        }
        turnoIniciadoEmMs = agora; // reancora
    }

    private void iniciarTurnoDoOponente() {
        jogadorAtual = (jogadorAtual == Tabuleiro.PRETO) ? Tabuleiro.BRANCO : Tabuleiro.PRETO;
        turnoIniciadoEmMs = System.currentTimeMillis();
    }

    // ----- Ações -----
    public synchronized boolean fazerJogada(int x, int y, int corJogador){
        if (gameOver) { lastInfo = "Jogo encerrado."; return false; }
        descontarTempoAteAgora();
        if (gameOver) return false; // tempo pode ter acabado ao descontar
        if (corJogador != jogadorAtual) { lastInfo = "Não é seu turno."; return false; }

        Tabuleiro.MoveResult r = tabuleiro.tentarJogada(x,y,corJogador, ultimoHash);
        if (!r.legal) { lastInfo = r.reason; return false; }

        // ko simples
        ultimoHash = r.antesHash;

        // placar por capturas
        if (corJogador == Tabuleiro.PRETO) prisioneirosPretas += r.capturadas;
        else prisioneirosBrancas += r.capturadas;

        lastX = r.lastX; lastY = r.lastY;
        ultimasCapturas = r.removed != null ? r.removed : new ArrayList<>();
        consecutivePasses = 0;
        lastInfo = null;

        iniciarTurnoDoOponente();
        return true;
    }

    public synchronized void passar(int corJogador){
        if (gameOver) return;
        descontarTempoAteAgora();
        if (gameOver) return;
        if (corJogador != jogadorAtual) return;

        consecutivePasses++;
        lastInfo = "Jogador passou.";
        ultimasCapturas = List.of();
        if (consecutivePasses >= 2) {
            gameOver = true;
            lastInfo = "Dois passes consecutivos. Jogo encerrado.";
        } else {
            iniciarTurnoDoOponente();
        }
    }

    public synchronized void desistir(int corJogador){
        if (gameOver) return;
        descontarTempoAteAgora();
        gameOver = true;
        lastInfo = (corJogador==Tabuleiro.PRETO? "Pretas" : "Brancas") + " desistiram.";
    }

    // Reiniciar
    public synchronized void reiniciar(){
        int n = tabuleiro.getTamanho();
        tabuleiro.copiarDe(new int[n][n]);
        jogadorAtual = Tabuleiro.PRETO;

        prisioneirosPretas = 0;
        prisioneirosBrancas = 0;
        ultimoHash = null;
        lastX = lastY = -1;
        lastInfo = "Novo jogo iniciado.";
        ultimasCapturas = List.of();
        gameOver = false;
        consecutivePasses = 0;

        // tempos
        tempoPretasRestanteMs  = tempoInicialMs;
        tempoBrancasRestanteMs = tempoInicialMs;
        turnoIniciadoEmMs = System.currentTimeMillis();
    }

    // ----- Getters para UI/RMI -----
    public synchronized EstadoJogo snapshotEstado(){
        // sempre desconta antes de enviar o estado, para relógio “ao vivo”
        descontarTempoAteAgora();
        return new EstadoJogo(
            tabuleiro,
            jogadorAtual,
            prisioneirosPretas,
            prisioneirosBrancas,
            lastX,
            lastY,
            lastInfo,
            ultimasCapturas,
            gameOver,
            tempoPretasRestanteMs,
            tempoBrancasRestanteMs
        );
    }

    // legado (chamado pela impl RMI antiga); manter por compatibilidade
    public Tabuleiro getTabuleiro(){ return this.tabuleiro; }
    public int getJogadorAtual(){ return this.jogadorAtual; }
    public int getPontuacaoPretas(){ return this.prisioneirosPretas; }
    public int getPontuacaoBrancas(){ return this.prisioneirosBrancas; }
    public int getLastX(){ return lastX; }
    public int getLastY(){ return lastY; }
    public String getLastInfo(){ return lastInfo; }
    public List<int[]> getUltimasCapturas(){ return ultimasCapturas; }
    public boolean isGameOver(){ return gameOver; }
    public long getTempoPretasRestanteMs(){ return tempoPretasRestanteMs; }
    public long getTempoBrancasRestanteMs(){ return tempoBrancasRestanteMs; }
}
