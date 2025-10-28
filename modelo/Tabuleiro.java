package modelo;
import java.io.Serializable;
import java.util.*;

public class Tabuleiro implements Serializable {
    private static final long serialVersionUID = 1L;
    private final int tam;
    private final int[][] posicoes;

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

    private List<int[]> vizinhos(int x, int y){
        int[][] d = {{1,0},{-1,0},{0,1},{0,-1}};
        List<int[]> ns = new ArrayList<>(4);
        for (int[] v: d) {
            int nx = x + v[0], ny = y + v[1];
            if (posicaoValida(nx,ny)) ns.add(new int[]{nx,ny});
        }
        return ns;
    }

    private GroupInfo grupoELiberdades(int x, int y){
        int cor = get(x,y);
        if (cor == VAZIO) return new GroupInfo(Collections.emptySet(), Collections.emptySet(), VAZIO);
        boolean[][] vis = new boolean[tam][tam];
        Deque<int[]> q = new ArrayDeque<>();
        Set<Point> grupo = new HashSet<>();
        Set<Point> libs  = new HashSet<>();
        q.add(new int[]{x,y});
        vis[x][y] = true;
        while(!q.isEmpty()){
            int[] p = q.poll();
            int px = p[0], py = p[1];
            grupo.add(new Point(px,py));
            for (int[] n: vizinhos(px,py)){
                int nx = n[0], ny = n[1];
                if (get(nx,ny) == VAZIO) libs.add(new Point(nx,ny));
                else if (!vis[nx][ny] && get(nx,ny) == cor) { vis[nx][ny] = true; q.add(new int[]{nx,ny}); }
            }
        }
        return new GroupInfo(grupo, libs, cor);
    }

    private int removerGrupo(Set<Point> grupo, List<int[]> removed){
        int count = 0;
        for (Point p: grupo){
            if (posicoes[p.x][p.y] != VAZIO){
                posicoes[p.x][p.y] = VAZIO; count++;
                if (removed != null) removed.add(new int[]{p.x,p.y});
            }
        }
        return count;
    }

    private int aplicarJogadaComCaptura(int x, int y, int cor, List<int[]> removed){
        posicoes[x][y] = cor; // já validado
        int oponente = (cor == PRETO) ? BRANCO : PRETO;
        int capturadas = 0;
        for (int[] n: vizinhos(x,y)){
            if (get(n[0],n[1]) == oponente){
                GroupInfo gi = grupoELiberdades(n[0], n[1]);
                if (gi.liberdades.isEmpty()){
                    capturadas += removerGrupo(gi.grupo, removed);
                }
            }
        }
        return capturadas;
    }

    public MoveResult tentarJogada(int x, int y, int cor, String ultimoHashKo){
        if (!posicaoValida(x,y)) return MoveResult.illegal("Fora do tabuleiro");
        if (get(x,y) != VAZIO)   return MoveResult.illegal("Interseção ocupada");

        String antes = hashTabuleiro();
        List<int[]> removed = new ArrayList<>();
        int capturadas;

        posicoes[x][y] = cor;
        capturadas = aplicarJogadaComCaptura(x,y,cor, removed);

        GroupInfo meu = grupoELiberdades(x,y);
        if (meu.liberdades.isEmpty() && capturadas == 0){
            setFromHash(antes);
            return MoveResult.illegal("Suicídio não permitido");
        }

        String depois = hashTabuleiro();
        if (ultimoHashKo != null && ultimoHashKo.equals(depois)){
            setFromHash(antes);
            return MoveResult.illegal("Ko: repetir posição anterior é proibido");
        }

        return MoveResult.ok(capturadas, antes, depois, x, y, removed);
    }

    // ---- hash utilitário ----
    public String hashTabuleiro(){
        StringBuilder sb = new StringBuilder(tam*tam);
        for (int i=0;i<tam;i++) for (int j=0;j<tam;j++) sb.append((char)('0'+posicoes[i][j]));
        return sb.toString();
    }
    private int[][] hashToBoard(String h){
        int[][] m = new int[tam][tam]; int k=0;
        for (int i=0;i<tam;i++) for (int j=0;j<tam;j++) m[i][j] = h.charAt(k++) - '0';
        return m;
    }
    public void setFromHash(String h){ copiarDe(hashToBoard(h)); }
    public void copiarDe(int[][] m){ for (int i=0;i<tam;i++) System.arraycopy(m[i], 0, posicoes[i], 0, tam); }

    private static class Point { final int x,y; Point(int x,int y){ this.x=x; this.y=y; }
        @Override public boolean equals(Object o){ if(!(o instanceof Point)) return false; Point p=(Point)o; return p.x==x&&p.y==y; }
        @Override public int hashCode(){ return (x*397)^y; }
    }
    private static class GroupInfo { final Set<Point> grupo, liberdades; final int cor;
        GroupInfo(Set<Point> g, Set<Point> l, int c){ grupo=g; liberdades=l; cor=c; } }

    public static class MoveResult implements Serializable {
        public final boolean legal;
        public final int capturadas;
        public final String antesHash, depoisHash;
        public final int lastX, lastY;
        public final String reason;
        public final List<int[]> removed; // posições capturadas

        private MoveResult(boolean legal, int c, String a, String d, int lx, int ly, String r, List<int[]> rem){
            this.legal=legal; this.capturadas=c; this.antesHash=a; this.depoisHash=d;
            this.lastX=lx; this.lastY=ly; this.reason=r; this.removed = rem;
        }
        public static MoveResult illegal(String reason){ return new MoveResult(false,0,null,null,-1,-1,reason, List.of()); }
        public static MoveResult ok(int c,String a,String d,int lx,int ly, List<int[]> rem){ return new MoveResult(true,c,a,d,lx,ly,null, rem); }
    }
}
