package modelo;

public class Jogo {
    private Tabuleiro tabuleiro;
    private int jogadorAtual;
    private int pontuacaoBranco;
    private int pontuacaoPreto;
   
    public Jogo(int tam){
        tabuleiro = new Tabuleiro(tam);
        jogadorAtual = 2;
        pontuacaoPreto = 0;
        pontuacaoBranco = 0;
    }

    public boolean fazerJogada(int x, int y){
        if(!tabuleiro.posicaoValida(x, y) || tabuleiro.getPeca(x,y) != 0){
            return false;
        }

        tabuleiro.colocarPeca(x, y, jogadorAtual);
        int capturadas = tabuleiro.processarCapturas(x, y, jogadorAtual);
        System.out.println("Pecas capturadas: " + capturadas);
        if(jogadorAtual == 1){
            pontuacaoBranco += capturadas;
        }
        else{
            pontuacaoPreto += capturadas;
        }
        jogadorAtual = jogadorAtual == 1 ? 2 : 1;

        return true;
    }

    public Tabuleiro getTabuleiro(){
        return this.tabuleiro;
    }
}
