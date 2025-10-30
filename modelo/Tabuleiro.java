package modelo;
import java.io.Serializable;
import java.util.*;

/**
 * A classe Tabuleiro representa a grade do jogo de Go.
 * Ela é 'Serializable' para poder ser enviada pela rede (RMI) como parte
 * do 'EstadoJogo'.
 *
 * Esta classe NÃO sabe sobre turnos, tempo ou placar total.
 * Ela APENAS gerencia a grade, a colocação de peças, as capturas, e as
 * regras de posição (como suicídio e Ko). É a "calculadora" de física do jogo.
 */
public class Tabuleiro implements Serializable {
    private static final long serialVersionUID = 1L;
    private final int tam;
    private final int[][] posicoes;

    // Constantes públicas para identificar o conteúdo da grade
    public static final int VAZIO = 0;
    public static final int BRANCO = 1;
    public static final int PRETO  = 2;

    public Tabuleiro(int tamanho){
        this.tam = tamanho;
        this.posicoes = new int[tam][tam];
    }

    public int getTamanho(){ return tam; }


    public int get(int x, int y){ return posicoes[x][y]; }


    public boolean posicaoValida(int x, int y){ return x>=0 && x<tam && y>=0 && y<tam; }

    /**
     * Retorna uma lista de coordenadas [x, y] dos vizinhos diretos (não-diagonais)
     * de um ponto que estão DENTRO do tabuleiro.
     */
    private List<int[]> vizinhos(int x, int y){
        // Array de direções: [Baixo], [Cima], [Direita], [Esquerda]
        int[][] d = {{1,0},{-1,0},{0,1},{0,-1}};
        List<int[]> ns = new ArrayList<>(4);
        for (int[] v: d) {
            int nx = x + v[0], ny = y + v[1];
            if (posicaoValida(nx,ny)) ns.add(new int[]{nx,ny});
        }
        return ns;
    }

    /**
     * Algoritmo central do Go. A partir de uma peça em (x, y), encontra todo o
     * "grupo" de peças conectadas da mesma cor e também todas as "liberdades"
     * (interseções vazias) adjacentes a esse grupo.
     *
     * Usa uma Busca em Largura (BFS / "flood-fill") para explorar o grupo.
     * conjunto de liberdades do grupo.
     */
    private GroupInfo grupoELiberdades(int x, int y){
        int cor = get(x,y);
        // Se a casa inicial está vazia, retorna um grupo vazio.
        if (cor == VAZIO) return new GroupInfo(Collections.emptySet(), Collections.emptySet(), VAZIO);

        boolean[][] vis = new boolean[tam][tam]; // Matriz de "visitados" para a busca
        Deque<int[]> q = new ArrayDeque<>();     // Fila para a busca (BFS)
        Set<Point> grupo = new HashSet<>();      // Conjunto de peças (Points) no grupo
        Set<Point> libs  = new HashSet<>();      // Conjunto de liberdades (Points) do grupo

        // Inicia a busca a partir da peça (x, y)
        q.add(new int[]{x,y});
        vis[x][y] = true;

        while(!q.isEmpty()){
            int[] p = q.poll(); // Pega o próximo da fila
            int px = p[0], py = p[1];
            grupo.add(new Point(px,py)); // Adiciona ao grupo

            // Verifica todos os vizinhos da peça atual
            for (int[] n: vizinhos(px,py)){
                int nx = n[0], ny = n[1];
                if (get(nx,ny) == VAZIO) {
                    // Se o vizinho é VAZIO, é uma liberdade.
                    libs.add(new Point(nx,ny));
                } else if (!vis[nx][ny] && get(nx,ny) == cor) {
                    // Se o vizinho é da MESMA COR e não foi visitado,
                    // marca como visitado e adiciona na fila para explorar.
                    vis[nx][ny] = true;
                    q.add(new int[]{nx,ny});
                }
            }
        }
        return new GroupInfo(grupo, libs, cor);
    }

    /**
     * Remove um grupo de peças do tabuleiro (define suas posições como VAZIO).
     * @param grupo O conjunto de 'Points' (coordenadas) a serem removidos.
     * @param removed Uma lista (passada por referência) onde as coordenadas [x,y]
     * das peças removidas serão adicionadas (para animação/info).
     * @return O número de peças efetivamente removidas.
     */
    private int removerGrupo(Set<Point> grupo, List<int[]> removed){
        int count = 0;
        for (Point p: grupo){
            if (posicoes[p.x][p.y] != VAZIO){
                posicoes[p.x][p.y] = VAZIO; // Remove a peça
                count++;
                if (removed != null) removed.add(new int[]{p.x,p.y});
            }
        }
        return count;
    }

    /**
     * Coloca uma peça no tabuleiro e, em seguida, verifica todos os grupos
     * Oponentes vizinhos. Se algum grupo oponente ficar sem liberdades
     * após esta jogada, ele é capturado e removido.
     *
     * @param x Posição X da jogada.
     * @param y Posição Y da jogada.
     * @param cor Cor da peça jogada.
     * @param removed Lista para acumular as coordenadas das peças capturadas.
     * @return O número total de peças oponentes capturadas nesta jogada.
     */
    private int aplicarJogadaComCaptura(int x, int y, int cor, List<int[]> removed){
        posicoes[x][y] = cor; // Coloca a peça
        int oponente = (cor == PRETO) ? BRANCO : PRETO;
        int capturadas = 0;

        // Verifica todos os 4 vizinhos da peça recém-colocada
        for (int[] n: vizinhos(x,y)){
            // Se o vizinho é um oponente...
            if (get(n[0],n[1]) == oponente){
                // ...calcula o grupo e as liberdades DESSE oponente.
                GroupInfo gi = grupoELiberdades(n[0], n[1]);
                // Se o grupo do oponente não tem mais liberdades...
                if (gi.liberdades.isEmpty()){
                    // ...captura o grupo!
                    capturadas += removerGrupo(gi.grupo, removed);
                }
            }
        }
        return capturadas;
    }

    /**
     * A função PÚBLICA e principal para validar e executar uma jogada.
     * Esta é a "API" do tabuleiro, chamada pela classe 'Jogo'.
     * Ela verifica todas as regras de posição:
     * 1. Se a casa está ocupada ou fora do tabuleiro.
     * 2. Se a jogada resulta em captura de peças oponentes.
     * 3. Se a jogada é "suicídio" (ilegal).
     * 4. Se a jogada viola a regra de "Ko" (repetição de tabuleiro).
     *
     * @param x Coordenada X da jogada.
     * @param y Coordenada Y da jogada.
     * @param cor Cor do jogador que está tentando jogar.
     * @param ultimoHashKo O hash do tabuleiro *antes* da jogada anterior, para
     * verificar a regra do Ko.
     * @return Um objeto MoveResult, indicando se a jogada foi 'legal' (ok) ou
     * 'ilegal', e por quê.
     */
    public MoveResult tentarJogada(int x, int y, int cor, String ultimoHashKo){
        // Verificação 1: Jogada em local válido e vazio
        if (!posicaoValida(x,y)) return MoveResult.illegal("Fora do tabuleiro");
        if (get(x,y) != VAZIO)   return MoveResult.illegal("Interseção ocupada");

        // Preparação: Salva o estado anterior
        String antes = hashTabuleiro(); // "Foto" do tabuleiro ANTES da jogada (para Ko/Suicídio)
        List<int[]> removed = new ArrayList<>();
        int capturadas;

        // Execução: Coloca a peça e tenta capturar
        // (Este método é temporário, pode ser desfeito)
        posicoes[x][y] = cor;
        capturadas = aplicarJogadaComCaptura(x,y,cor, removed);

        // Verificação 2: Suicídio
        // Após colocar a peça e capturar oponentes, verificamos o *nosso* grupo.
        GroupInfo meu = grupoELiberdades(x,y);
        if (meu.liberdades.isEmpty() && capturadas == 0){
            // Se o nosso grupo não tem liberdades E não capturamos ninguém,
            // é suicídio.
            setFromHash(antes); // Desfaz a jogada
            return MoveResult.illegal("Suicídio não permitido");
        }

        // Verificação 3: Regra do Ko
        String depois = hashTabuleiro(); // "Foto" do tabuleiro DEPOIS da jogada
        if (ultimoHashKo != null && ultimoHashKo.equals(depois)){
            // Se o estado do tabuleiro *depois* da minha jogada é IDÊNTICO
            // ao estado que estava *antes* da última jogada do meu oponente,
            // é uma violação do Ko.
            setFromHash(antes); // Desfaz a jogada
            return MoveResult.illegal("Ko: repetir posição anterior é proibido");
        }

        // A jogada é legal!
        return MoveResult.ok(capturadas, antes, depois, x, y, removed);
    }


    /**
     * Cria uma "impressão digital" (hash) única do estado atual do tabuleiro.
     * Converte a grade 2D em uma string simples (ex: "001201...").
     * Usado para detectar a regra do Ko e para reverter jogadas ilegais.
     */
    public String hashTabuleiro(){
        StringBuilder sb = new StringBuilder(tam*tam);
        for (int i=0;i<tam;i++)
            for (int j=0;j<tam;j++)
                sb.append((char)('0'+posicoes[i][j]));
        return sb.toString();
    }

    /**
     * Converte uma string de hash (criada por hashTabuleiro) de volta
     * em um array 2D int[][].
     */
    private int[][] hashToBoard(String h){
        int[][] m = new int[tam][tam]; int k=0;
        for (int i=0;i<tam;i++)
            for (int j=0;j<tam;j++)
                m[i][j] = h.charAt(k++) - '0';
        return m;
    }

    /**
     * Restaura o estado do tabuleiro para um estado anterior, com base em um hash.
     * Usado para "desfazer" jogadas de suicídio ou Ko.
     */
    public void setFromHash(String h){
        copiarDe(hashToBoard(h));
    }

    /**
     * Copia o conteúdo de um array 2D externo ('m') para o array 'posicoes'
     * interno desta classe.
     */
    public void copiarDe(int[][] m){
        for (int i=0;i<tam;i++)
            System.arraycopy(m[i], 0, posicoes[i], 0, tam);
    }


    /**
     * Classe interna de conveniência para representar uma coordenada (x, y).
     * Usada em Sets e Deques para facilitar a busca em largura (BFS).
     * Inclui hashCode() e equals() para funcionar corretamente em Sets.
     */
    private static class Point {
        final int x,y;
        Point(int x,int y){ this.x=x; this.y=y; }
        @Override public boolean equals(Object o){ if(!(o instanceof Point)) return false; Point p=(Point)o; return p.x==x&&p.y==y; }
        @Override public int hashCode(){ return (x*397)^y; } // Um hash simples
    }

    /**
     * Classe interna de conveniência para agrupar os resultados da
     * função 'grupoELiberdades'.
     */
    private static class GroupInfo {
        final Set<Point> grupo, liberdades;
        final int cor;
        GroupInfo(Set<Point> g, Set<Point> l, int c){ grupo=g; liberdades=l; cor=c; }
    }

    /**
     * Classe pública estática que encapsula o resultado de uma 'tentarJogada'.
     * A classe 'Jogo' lê este objeto para saber o que aconteceu no tabuleiro.
     * Contém informações sobre legalidade, capturas e hashes para o Ko.
     */
    public static class MoveResult implements Serializable {
        public final boolean legal;        // A jogada foi permitida?
        public final int capturadas;       // Quantas peças capturou?
        public final String antesHash;    // Hash do tabuleiro ANTES da jogada
        public final String depoisHash;   // Hash do tabuleiro DEPOIS da jogada
        public final int lastX, lastY;     // Coordenadas da jogada
        public final String reason;       // Motivo se for ilegal
        public final List<int[]> removed; 

        // Construtor privado, usado pelas fábricas 'ok' e 'illegal'
        private MoveResult(boolean legal, int c, String a, String d, int lx, int ly, String r, List<int[]> rem){
            this.legal=legal; this.capturadas=c; this.antesHash=a; this.depoisHash=d;
            this.lastX=lx; this.lastY=ly; this.reason=r; this.removed = rem;
        }

        /** Método de fábrica para um resultado de jogada ILEGAL */
        public static MoveResult illegal(String reason){
            return new MoveResult(false,0,null,null,-1,-1,reason, List.of());
        }

        /** Método de fábrica para um resultado de jogada LEGAL */
        public static MoveResult ok(int c,String a,String d,int lx,int ly, List<int[]> rem){
            return new MoveResult(true,c,a,d,lx,ly,null, rem);
        }
    }
}