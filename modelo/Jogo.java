package modelo;

import java.util.ArrayList;
import java.util.List;

// Esta classe é o "gerente" do jogo. Ela controla as regras,
// quem joga, o tempo, e o placar, usando o Tabuleiro para mover as peças.
public class Jogo {
    // Guarda a instância do tabuleiro (a grade) e de quem é a vez de jogar.
    private final Tabuleiro tabuleiro;
    private int jogadorAtual;

    // Contadores de placar (quantas peças cada um capturou).
    private int prisioneirosPretas;
    private int prisioneirosBrancas;

    // Guarda a "foto" (hash) do tabuleiro da jogada anterior, para a regra do "Ko".
    private String ultimoHash = null;

    // Informações para a interface: onde foi a última jogada,
    // uma mensagem (ex: "Jogada ilegal"), e a lista de peças capturadas (para animação).
    private int lastX = -1, lastY = -1;
    private String lastInfo = null;
    private List<int[]> ultimasCapturas = List.of();

    // Controle de fim de jogo: se o jogo acabou e quantos "passar" seguidos houveram.
    private boolean gameOver = false;
    private int consecutivePasses = 0;

    // Variáveis do relógio: o tempo total, o tempo restante de cada jogador,
    // e o "timestamp" (carimbo de tempo) de quando o turno atual começou (em milissegundos).
    private final long tempoInicialMs;
    private long tempoPretasRestanteMs;
    private long tempoBrancasRestanteMs;
    private long turnoIniciadoEmMs;

    // Construtor "atalho": se chamar só com o tamanho,
    // ele chama o outro construtor definindo 5 minutos (5 * 60_000L ms) como tempo padrão.
    public Jogo(int tamanhoTabuleiro) {
        this(tamanhoTabuleiro, 5 * 60_000L);
    }

    // Construtor "principal": inicializa todas as variáveis de estado do jogo.
    public Jogo(int tamanhoTabuleiro, long tempoInicialMs) {
        this.tabuleiro = new Tabuleiro(tamanhoTabuleiro); // Cria o tabuleiro.
        this.jogadorAtual = Tabuleiro.PRETO; // Define o Preto como o primeiro a jogar.
        this.tempoInicialMs = tempoInicialMs; // Guarda o tempo total (para reiniciar).
        this.tempoPretasRestanteMs  = tempoInicialMs; // Define o tempo inicial do Preto.
        this.tempoBrancasRestanteMs = tempoInicialMs; // Define o tempo inicial do Branco.
        // "Ancora" o tempo: o relógio do Preto (primeiro jogador) começa a contar agora.
        this.turnoIniciadoEmMs = System.currentTimeMillis();
    }
    
    // Método-chave do relógio. É chamado antes de QUALQUER ação (jogar, passar, etc).
    private void descontarTempoAteAgora() {
        if (gameOver) return; // Se o jogo acabou, não faz nada.

        long agora = System.currentTimeMillis(); // Pega o tempo atual.
        // Calcula quanto tempo passou desde o início do turno.
        long decorrido = Math.max(0, agora - turnoIniciadoEmMs);

        // Verifica de quem é o turno e subtrai o tempo gasto do total dele.
        if (jogadorAtual == Tabuleiro.PRETO) {
            tempoPretasRestanteMs = Math.max(0, tempoPretasRestanteMs - decorrido);
            // Se o tempo acabar (chegar a 0), encerra o jogo.
            if (tempoPretasRestanteMs == 0) { gameOver = true; lastInfo = "Tempo esgotado para Pretas."; }
        } else {
            tempoBrancasRestanteMs = Math.max(0, tempoBrancasRestanteMs - decorrido);
            if (tempoBrancasRestanteMs == 0) { gameOver = true; lastInfo = "Tempo esgotado para Brancas."; }
        }
        
        // "Reancora" o tempo. Se o método for chamado de novo daqui a 1ms,
        // o "decorrido" será de apenas 1ms. Isso mantém o relógio preciso.
        turnoIniciadoEmMs = agora;
    }

    // Passa o turno para o próximo jogador.
    private void iniciarTurnoDoOponente() {
        // Troca o jogador (PRETO vira BRANCO, BRANCO vira PRETO).
        jogadorAtual = (jogadorAtual == Tabuleiro.PRETO) ? Tabuleiro.BRANCO : Tabuleiro.PRETO;
        // "Ancora" o tempo de início do NOVO turno. O relógio do oponente começa agora.
        turnoIniciadoEmMs = System.currentTimeMillis();
    }

    // Ação principal: Tentar fazer uma jogada.
    // "synchronized" impede que dois jogadores mexam no jogo ao mesmo tempo (segurança).
    public synchronized boolean fazerJogada(int x, int y, int corJogador){
        // 1. Validação: Se o jogo acabou, avisa e não faz nada.
        if (gameOver) { lastInfo = "Jogo encerrado."; return false; }
        
        // 2. Relógio: Atualiza o relógio do jogador atual ANTES de fazer a jogada.
        descontarTempoAteAgora();
        
        // 3. Validação: Verifica se o tempo acabou APÓS o desconto.
        if (gameOver) return false;
        
        // 4. Validação: Verifica se é a vez deste jogador.
        if (corJogador != jogadorAtual) { lastInfo = "Não é seu turno."; return false; }

        // 5. Tentativa: Pede ao Tabuleiro para TENTAR a jogada.
        // O Tabuleiro vai validar as regras (Ko, Suicídio, Ocupado).
        Tabuleiro.MoveResult r = tabuleiro.tentarJogada(x,y,corJogador, ultimoHash);
        
        // 6. Validação: Se o Tabuleiro disse que é ilegal, avisa e não faz nada.
        if (!r.legal) { lastInfo = r.reason; return false; }

        // 7. Sucesso! A jogada foi legal. Atualiza o estado do jogo:
        
        // Guarda o hash ANTERIOR para a regra do Ko na PRÓXIMA jogada.
        ultimoHash = r.antesHash;

        // Atualiza o placar de prisioneiros.
        if (corJogador == Tabuleiro.PRETO) prisioneirosPretas += r.capturadas;
        else prisioneirosBrancas += r.capturadas;

        // Salva as infos (onde foi, o que capturou) para a interface.
        lastX = r.lastX; lastY = r.lastY;
        ultimasCapturas = r.removed != null ? r.removed : new ArrayList<>();
        consecutivePasses = 0; // Zera o contador de "passar", já que foi uma jogada.
        lastInfo = null; // Limpa a mensagem de status.

        // 8. Passa a vez para o oponente.
        iniciarTurnoDoOponente();
        return true; // Avisa que a jogada foi um sucesso.
    }

    // Ação: Jogador decide "Passar" a vez.
    public synchronized void passar(int corJogador){
        // Validações: Jogo acabou?
        if (gameOver) return;
        // Atualiza o relógio.
        descontarTempoAteAgora();
        // Tempo acabou?
        if (gameOver) return;
        // É sua vez?
        if (corJogador != jogadorAtual) return;

        // Ação de "Passar":
        consecutivePasses++; // Aumenta o contador de "passar".
        lastInfo = "Jogador passou."; // Define a mensagem.
        ultimasCapturas = List.of(); // Limpa as capturas (para a animação parar).

        // Se 2 jogadores passaram em sequência, o jogo acaba.
        if (consecutivePasses >= 2) {
            gameOver = true;
            lastInfo = "Dois passes consecutivos. Jogo encerrado.";
        } else {
            // Se não, só passa a vez.
            iniciarTurnoDoOponente();
        }
    }

    // Ação: Jogador decide "Desistir" do jogo.
    public synchronized void desistir(int corJogador){
        if (gameOver) return; // Não pode desistir se já acabou.
        
        descontarTempoAteAgora(); // Atualiza o relógio (para a contagem final).
        gameOver = true; // Encerra o jogo imediatamente.
        // Define a mensagem de quem desistiu.
        lastInfo = (corJogador==Tabuleiro.PRETO? "Pretas" : "Brancas") + " desistiram.";
    }

    // Ação: Reinicia o jogo para o estado inicial.
    public synchronized void reiniciar(){
        int n = tabuleiro.getTamanho();
        // Limpa o tabuleiro (copia um array vazio para ele).
        tabuleiro.copiarDe(new int[n][n]);
        // Reseta o jogador inicial.
        jogadorAtual = Tabuleiro.PRETO;

        // Zera todas as variáveis de estado: placar, hash, infos, etc.
        prisioneirosPretas = 0;
        prisioneirosBrancas = 0;
        ultimoHash = null;
        lastX = lastY = -1;
        lastInfo = "Novo jogo iniciado.";
        ultimasCapturas = List.of();
        gameOver = false;
        consecutivePasses = 0;

        // Reseta os relógios para o valor inicial.
        tempoPretasRestanteMs  = tempoInicialMs;
        tempoBrancasRestanteMs = tempoInicialMs;
        // E "dispara" o relógio do Preto novamente.
        turnoIniciadoEmMs = System.currentTimeMillis();
    }

    // O método mais importante para a rede (RMI).
    // Tira uma "foto" (snapshot) de todo o estado atual do jogo.
    public synchronized EstadoJogo snapshotEstado(){
        // ATENÇÃO: Sempre atualiza o relógio ANTES de tirar a foto.
        // Isso faz com que a interface (que chama isso de 500 em 500ms)
        // receba o tempo "ao vivo" sendo descontado.
        descontarTempoAteAgora();
        
        // Cria e envia o "pacote" de dados (EstadoJogo) com todas as infos.
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

    // Getters simples: Funções que só retornam um valor.
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