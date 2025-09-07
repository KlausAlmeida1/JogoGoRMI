package modelo;
import java.io.Serializable;
public class Ponto implements Serializable{
    private static final long serialVersionUID = 1L;
    public final int x;
    public final int y;
    
    public Ponto(int x, int y){
        this.x = x;
        this.y = y;
    }

    public String imprimir(){
        return "(" + x + ", " + y + ")";
    }
}
