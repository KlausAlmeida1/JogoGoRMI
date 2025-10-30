
# 🎮 Jogo de Go RMI (9x9)

Este é um projeto acadêmico que implementa o jogo de Go (também conhecido como Baduk) em uma arquitetura Cliente-Servidor, utilizando Java e RMI (Remote Method Invocation) para a comunicação em rede.

O projeto foi desenvolvido com foco em uma separação clara de responsabilidades (Modelo-Visão-Controle) e na implementação de regras complexas do jogo, como controle de tempo, captura de peças, regra de Ko e prevenção de suicídio.

---

## ✨ Funcionalidades Principais

### Funcionalidades de Jogo
* **Arquitetura Cliente-Servidor:** O Servidor centraliza toda a lógica do jogo. Múltiplos clientes podem se conectar para jogar.
* **Sincronização em Tempo Real:** As jogadas feitas por um jogador são refletidas na tela do oponente em tempo real (através de polling).
* **Controle de Tempo:** Cada jogador possui um cronômetro regressivo individual. Se o tempo de um jogador acabar, ele perde o jogo.
* **Lógica de Captura:** Implementação completa da lógica de captura de peças e grupos de peças.
* **Regra do Ko Simples:** O jogo impede jogadas que repitam o estado imediatamente anterior do tabuleiro.
* **Prevenção de Suicídio:** O jogo impede que um jogador faça uma jogada que resulte na captura imediata de seu próprio grupo (a menos que essa jogada capture um grupo oponente).
* **Ações do Jogador:** Além de jogar, os jogadores podem:
    * **Passar** o turno.
    * **Desistir** da partida.
    * **Reiniciar** o jogo (disponível para ambos os jogadores).

### Interface Gráfica (GUI)
* **Interface Customizada (Java Swing):** Todos os componentes visuais são desenhados do zero (JComponent/JPanel).
* **Pré-visualização de Jogada:** Uma "sombra" (preview) da peça é mostrada na cor do jogador atual, "grudando" na interseção mais próxima do mouse.
* **Marcação de Última Jogada:** Um ponto vermelho indica qual foi a última peça colocada no tabuleiro.
* **Animação de Captura:** Peças capturadas piscam em vermelho brevemente antes de desaparecerem.
* **Painel de Status Dinâmico:** Um painel de status customizado que exibe:
    * Quem é o jogador (Preto/Branco) e se é sua vez (com destaque visual).
    * Relógios individuais que são atualizados em tempo real.
    * Contagem de prisioneiros (peças capturadas) para cada jogador.
* **Barra Lateral de Ações:** Botões estilizados para as ações de "Passar", "Desistir" e "Novo Jogo".

---

## ⚙️ Tecnologias Utilizadas

* **Linguagem:** Java
* **Interface Gráfica:** Java Swing (com pintura customizada 2D)
* **Comunicação em Rede:** Java RMI (Remote Method Invocation)

---

## 🏗️ Arquitetura

O sistema é dividido em três pacotes principais, seguindo uma variação do padrão MVC:

* **`modelo` (Model):** Contém o "cérebro" do jogo.
    * `Jogo.java`: O "Gerente" da partida. Controla turnos, tempo, placar e chama o tabuleiro.
    * `Tabuleiro.java`: O "Especialista". Sabe calcular regras de posição (captura, Ko, suicídio).
    * `EstadoJogo.java`: O "Pacote de Dados" (DTO) enviado pela rede, contendo uma "foto" do jogo.
* **`rede` (Controller/Network):** Faz a ponte de comunicação.
    * `InterfaceJogoRemoto.java`: O "contrato" RMI, definindo quais métodos podem ser chamados remotamente.
    * `JogoRemotoImpl.java`: A implementação do contrato no lado do servidor. É ele quem "atende o telefone" e repassa as ordens para o `Jogo.java`.
    * `Servidor.java` e `Cliente.java`: Os pontos de entrada (main) que iniciam o sistema.
* **`visao` (View):** Contém todas as classes da interface gráfica (Swing).
    * `JanelaJogo.java`: A janela principal (`JFrame`), que monta os painéis e gerencia os eventos.
    * `PainelTabuleiro.java`: O painel customizado que desenha a grade, as peças, sombras e animações.
    * `PainelStatus.java`: O painel customizado que desenha os relógios, placares e ícones de turno.

### Diagrama de Comunicação

O fluxo de comunicação segue o modelo RMI: o `Servidor` registra um serviço (`JogoRemotoImpl`). O `Cliente` procura esse serviço. A partir daí, ambas as janelas (do Servidor e do Cliente) enviam comandos para esse *único* objeto central.

```mermaid
graph TD
    subgraph "Computador 1 (Servidor)"
        S[Servidor.java] -- inicia --> V1[JanelaJogo (Preto)]
        S -- "inicia e registra" --> JRI[JogoRemotoImpl (Serviço RMI)]
        RMI[RMI Registry (Porta 1099)]
        JRI -- contém --> M[Lógica do Jogo (Jogo.java)]
        S -- "registra em" --> RMI
        V1 -- "RMI Call" --> JRI
    end
    
    subgraph "Computador 2 (Cliente)"
        C[Cliente.java] -- inicia --> V2[JanelaJogo (Branco)]
        C -- "RMI Lookup" --> RMI
        V2 -- "RMI Call" --> JRI
    end

    style V1 fill:#f9f,stroke:#333,stroke-width:2px
    style V2 fill:#f9f,stroke:#333,stroke-width:2px
    style JRI fill:#f99,stroke:#333,stroke-width:2px
````

-----

## 🚀 Como Executar

**Pré-requisitos:**

  * JDK (Java Development Kit) 8 ou superior instalado e configurado no `PATH`.

### 1\. Compilação

Abra um terminal na pasta raiz do projeto (onde está a pasta `src`) e execute o comando de compilação. Isso irá compilar todos os arquivos `.java` para uma nova pasta `bin`.

```bash
# No Windows (CMD ou PowerShell)
javac -d bin src/modelo/*.java src/rede/*.java src/visao/*.java

# No Linux ou macOS
javac -d bin src/modelo/*.java src/rede/*.java src/visao/*.java
```

### 2\. Execução

Você precisará de **dois terminais** abertos na pasta raiz do projeto.

**Terminal 1: Iniciar o Servidor (Jogador Preto)**

O `Servidor.java` já inicia o `rmiregistry` automaticamente na porta 1099.

```bash
# No Windows
java -cp bin rede.Servidor

# No Linux ou macOS
java -cp bin rede.Servidor
```

> O terminal exibirá "Servidor pronto" e a janela do Jogador 1 (Preto) será aberta.

**Terminal 2: Iniciar o Cliente (Jogador Branco)**

```bash
# No Windows
java -cp bin rede.Cliente

# No Linux ou macOS
java -cp bin rede.Cliente
```

> A janela do Jogador 2 (Branco) será aberta e se conectará ao servidor. O jogo pode começar\!

```
