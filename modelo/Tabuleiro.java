package modelo;
import java.util.*;
import java.io.Serializable;

public class Tabuleiro implements Serializable{
    private static final long serialVersionUID = 1L;
    private int tam;
    private int posicoes[][];
    static public final int BRANCO = 1;
    static public final int PRETO = 2;
    static public final int VAZIO = 0;

    public Tabuleiro(int tamanho){
        this.tam = tamanho;
        this.posicoes = new int[tam][tam];
    }

    private List<Ponto> encontrarGrupo(int x, int y){
        if(x < 0 || x >= tam || y < 0 || y >= tam){
            System.out.println("Ponto não existe");
            ArrayList<Ponto> p = new ArrayList<Ponto>();
            return p;
        }

        if(posicoes[x][y] == 0){
            System.out.println("Casa não pertence a nenhuma cor");
            ArrayList<Ponto> p = new ArrayList<Ponto>();
            return p;
        }

        ArrayList<Ponto> grupo = new ArrayList<Ponto>();
        LinkedList<Ponto> fila = new LinkedList<>();
        boolean[][] visitados = new boolean[tam][tam];
        int corGrupo = posicoes[x][y];

        visitados[x][y] = true;
        Ponto pontoInicial = new Ponto(x,y);
        fila.add(pontoInicial);

        while(!fila.isEmpty()){
            Ponto atual = fila.poll();
            grupo.add(atual);

            //esquerda
            if(atual.y - 1 >= 0 && posicoes[atual.x][atual.y - 1] == corGrupo && visitados[atual.x][atual.y-1] == false){

                fila.add(new Ponto(atual.x,atual.y -1));
                visitados[atual.x][atual.y -1] = true;
            }

            //cima
            if(atual.x - 1 >= 0 && posicoes[atual.x - 1][atual.y] == corGrupo && visitados[atual.x - 1][atual.y] == false){

                fila.add(new Ponto(atual.x - 1,atual.y));
                visitados[atual.x - 1][atual.y] = true;
            }

            //direita
            if(atual.y + 1 < tam && posicoes[atual.x][atual.y + 1] == corGrupo && visitados[atual.x][atual.y + 1] == false){

                fila.add(new Ponto(atual.x,atual.y + 1));
                visitados[atual.x][atual.y + 1] = true;
            }

            //baixo
            if(atual.x + 1 < tam && posicoes[atual.x + 1][atual.y] == corGrupo && visitados[atual.x + 1][atual.y] == false){

                fila.add(new Ponto(atual.x + 1,atual.y));
                visitados[atual.x + 1][atual.y] = true;
            }
        }
        return grupo;
    }

    private int contarLiberdade(List<Ponto> g){
        LinkedList<Ponto> grupo = new LinkedList<>(g);
        HashSet<Ponto> liberdades = new HashSet<Ponto>();
        int[][] direcoes = {{-1,0},{0,-1},{0,+1},{+1,0}};

        while (!grupo.isEmpty()) {
            Ponto atual = grupo.poll();

            //versão mais esperta de verificar os 4 vizinhos com menos código
            for(int[] dir: direcoes){
                int x = atual.x + dir[0];
                int y = atual.y + dir[1];

                if(posicaoValida(x, y) && posicoes[x][y] == 0){
                    liberdades.add(new Ponto(x, y));
                }
            }
        }
        
        return liberdades.size();
    }

    public int processarCapturas(int posX, int posY, int corJogador){
        int capturas = 0;
        int corInimigo = corJogador == 1 ? 2 : 1;
        int[][] direcoes = {{-1,0},{0,-1},{0,+1},{+1,0}};
        
        for(int[] dir: direcoes){
            int x = posX + dir[0];
            int y = posY + dir[1];

            if(posicaoValida(x, y) && posicoes[x][y] == corInimigo){
                List<Ponto> grupo = encontrarGrupo(x, y);
                int liberdades = contarLiberdade(grupo);

                if(liberdades == 0){
                    capturas += grupo.size();

                    for(Ponto peca : grupo){
                        posicoes[peca.x][peca.y] = 0;
                    }
                }
            }
        }
        return capturas;
    }

    public int getTamanho(){
        return this.tam;
    }

    public int getPeca(int x, int y){
        if(x >= 0 && x < tam && y >= 0 && y < tam){
            return posicoes[x][y];
        }
        return -1;
    }

    public boolean posicaoValida(int x, int y){
        if(x >= 0 && x < tam && y >= 0 && y < tam){
            return true;
        }
        return false;
    }
    public void colocarPeca(int x, int y, int cor){
        if(posicaoValida(x, y) && (cor == 1 || cor == 2)){
            posicoes[x][y] = cor;
        }
    }
}
