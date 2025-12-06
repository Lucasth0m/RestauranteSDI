package restauranteSDI.server;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import javax.xml.ws.Endpoint;
import java.text.DecimalFormat;
import java.nio.file.*;

public class Filial {

    // --- CORES PARA O TERMINAL (ANSI) ---
    public static final String RESET = "\u001B[0m";
    public static final String RED = "\u001B[31m";
    public static final String GREEN = "\u001B[32m";
    public static final String YELLOW = "\u001B[33m";
    public static final String BLUE = "\u001B[34m";
    public static final String CYAN = "\u001B[36m";
    public static final String WHITE_BOLD = "\033[1;37m";

    private enum State { FOLLOWER, CANDIDATE, LEADER }

    // Configurações
    private int id;
    private int portaP2P;
    private String enderecoSOAP;
    private Map<Integer, String> vizinhos;
    private String nomeArquivoEstoque;
    
    // Estado Raft
    private volatile State estadoAtual = State.FOLLOWER;
    private volatile int currentTerm = 0;
    private volatile int votedFor = -1;
    private volatile int liderId = -1;
    
    // Timers
    private long lastHeartbeatTime;
    private long electionTimeout;
    private boolean rodando = true;
    
    private Endpoint endpointSOAP;
    private Map<String, Double> estoqueLocal = new TreeMap<>();
    private ServerSocket serverSocket;
    private DecimalFormat df = new DecimalFormat("0.00");

    public Filial(int id, int portaP2P, String enderecoSOAP, Map<Integer, String> vizinhos) {
        this.id = id;
        this.portaP2P = portaP2P;
        this.enderecoSOAP = enderecoSOAP;
        this.vizinhos = vizinhos;
        
        this.nomeArquivoEstoque = "estoque_filial_" + id + ".csv";

        limparTela();
        System.out.println(WHITE_BOLD + ">>> INICIALIZANDO FILIAL " + id + " <<<" + RESET);
        
        if (!carregarEstoqueDoDisco()) {
            inicializarEstoqueAleatorio();
            salvarEstoqueNoDisco();
        }
        
        imprimirEstoque();
        
        resetElectionTimeout();
        this.lastHeartbeatTime = System.currentTimeMillis();

        iniciarServidorP2P();
        iniciarCicloRaft();
    }

    // --- PERSISTÊNCIA DE DADOS ---

    private boolean carregarEstoqueDoDisco() {
        File arquivo = new File(nomeArquivoEstoque);
        if (!arquivo.exists()) {
            return false;
        }

        try (BufferedReader br = new BufferedReader(new FileReader(arquivo))) {
            String linha;
            estoqueLocal.clear();
            while ((linha = br.readLine()) != null) {
                String[] partes = linha.split(",");
                if (partes.length >= 2) {
                    estoqueLocal.put(partes[0], Double.parseDouble(partes[1]));
                }
            }
            System.out.println(BLUE + ">> Estoque carregado do arquivo: " + nomeArquivoEstoque + RESET);
            return true;
        } catch (IOException e) {
            System.out.println(RED + "Erro ao ler arquivo de estoque: " + e.getMessage() + RESET);
            return false;
        }
    }

    private void salvarEstoqueNoDisco() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(nomeArquivoEstoque))) {
            for (Map.Entry<String, Double> entry : estoqueLocal.entrySet()) {
                pw.println(entry.getKey() + "," + entry.getValue());
            }
            // System.out.println(BLUE + "(Persistência atualizada)" + RESET);
        } catch (IOException e) {
            System.out.println(RED + "Erro crítico ao salvar estoque: " + e.getMessage() + RESET);
        }
    }

    // --- VISUALIZAÇÃO E GERAÇÃO DE ESTOQUE ---
    
    private void inicializarEstoqueAleatorio() {
        String[] produtosPossiveis = {
            "Lasanha de Carne", "Tilápia Frita", "Pizza de Calabresa", "Macarronada", 
            "Cachorro-Quente Gourmet", "Moqueca de Peixe", "Panqueca de Frango", 
            "Risoto de Camarão", "Feijoada Completa", "Frango Xadrez", 
            "Salmão Grelhado", "Picanha na Chapa", "Costela no Bafo", 
            "Carne de Sol com Macaxeira", "Spaghetti à Bolonhesa", 
            "Filé Mignon ao Molho Madeira", "Estrogonofe de Frango", "Yakissoba",
            "Bacalhau à Brás", "Frango Grelhado com Legumes", "Kibe Assado",
            "Nhoque ao Sugo", "Escondidinho de Carne Seca", "Bife à Parmegiana",
            "Hambúrguer Artesanal", "Polenta com Ragu",
            "Suco de Uva Integral", "Cerveja IPA", "Água com Gás", 
            "Cerveja Artesanal", "Negroni", "Água de Coco", "Limonada Suíça",
            "Ice", "Água Mineral", "Milkshake de Morango", "Mate Leão",
            "Cerveja Long Neck", "Cerveja Lager", "Mojito", "Tequila Shot",
            "Rum com Coca", "Espumante Taça", "Caipirinha de Limão",
            "Suco Natural de Laranja", "Vinho Branco Suave Taça", "Refrigerante Lata",
            "Refrigerante Zero", "Suco de Maracujá", "Cappuccino", "Whisky",
            "Guaraná Antártica", "Gin Tônica", "Coca-Cola Zero", "Café com Leite"
        };
        
        Random rand = new Random();
        for (String p : produtosPossiveis) {
            if (rand.nextDouble() > 0.15) { 
                estoqueLocal.put(p, 20.0 + rand.nextDouble() * 60.0);
            }
        }
        System.out.println(YELLOW + ">> Novo estoque aleatório gerado." + RESET);
    }

    private void imprimirEstoque() {
        System.out.println("\n" + WHITE_BOLD + "+-----------------------------+");
        System.out.println("|      ESTOQUE LOCAL (RAM)    |");
        System.out.println("+-----------------------------+" + RESET);
        System.out.printf("| %-15s | %-9s |\n", "PRODUTO", "PREÇO");
        System.out.println("+-----------------------------+");
        
        for (Map.Entry<String, Double> entry : estoqueLocal.entrySet()) {
            System.out.printf("| %-15s | R$ %5s |\n", 
                entry.getKey().length() > 15 ? entry.getKey().substring(0,15) : entry.getKey(), 
                df.format(entry.getValue()));
        }
        System.out.println("+-----------------------------+\n");
    }

    // --- CICLO RAFT ---
    private void iniciarCicloRaft() {
        new Thread(() -> {
            logEstado(BLUE, "Iniciando como FOLLOWER (Aguardando Líder...)");
            while (rodando) {
                try {
                    synchronized (this) {
                        long now = System.currentTimeMillis();
                        switch (estadoAtual) {
                            case FOLLOWER:
                            case CANDIDATE:
                                if (now - lastHeartbeatTime > electionTimeout) {
                                    iniciarEleicao();
                                }
                                break;
                            case LEADER:
                                enviarHeartbeats();
                                Thread.sleep(2000); 
                                break;
                        }
                    }
                    if (estadoAtual != State.LEADER) Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }).start();
    }

    // --- ELEIÇÃO ---
    private void iniciarEleicao() {
        estadoAtual = State.CANDIDATE;
        currentTerm++;
        votedFor = this.id;
        int votosRecebidos = 1;
        resetElectionTimeout();
        lastHeartbeatTime = System.currentTimeMillis();
        
        logEstado(YELLOW, ">>> TIMEOUT! Iniciando Eleição (Termo " + currentTerm + ") <<<");

        for (Integer vizinhoId : vizinhos.keySet()) {
            String resp = enviarMensagem(vizinhos.get(vizinhoId), "VOTE_REQ:" + currentTerm + ":" + this.id);
            if (resp != null && resp.startsWith("VOTE_AGREE")) {
                votosRecebidos++;
                System.out.print(YELLOW + "." + RESET);
            } else if (resp != null && resp.startsWith("TERM_HIGHER")) {
                virarFollower(Integer.parseInt(resp.split(":")[1]));
                return;
            }
        }
        System.out.println("");

        if (votosRecebidos > (vizinhos.size() + 1) / 2) {
            virarLider();
        } else {
            System.out.println(RED + "--- Eleição falhou (Sem maioria)" + RESET);
        }
    }

    private void virarLider() {
        if (estadoAtual == State.LEADER) return;
        estadoAtual = State.LEADER;
        liderId = this.id;
        
        System.out.println("\n" + GREEN + "###################################");
        System.out.println("#      EU SOU O LÍDER (Termo " + currentTerm + ")    #");
        System.out.println("###################################" + RESET);
        
        publicarWebService();
        enviarHeartbeats();
    }

    private void virarFollower(int novoTermo) {
        boolean eraLider = (estadoAtual == State.LEADER);
        estadoAtual = State.FOLLOWER;
        currentTerm = novoTermo;
        votedFor = -1;
        resetElectionTimeout();
        
        if (eraLider) {
            System.out.println(RED + ">>> Deixando a Liderança..." + RESET);
            if (endpointSOAP != null && endpointSOAP.isPublished()) endpointSOAP.stop();
        }
    }

    private void enviarHeartbeats() {
        String msg = "HEARTBEAT:" + currentTerm + ":" + this.id;
        for (String endereco : vizinhos.values()) {
            new Thread(() -> {
                String resp = enviarMensagem(endereco, msg);
                if (resp != null && resp.startsWith("TERM_HIGHER")) {
                    int termRemoto = Integer.parseInt(resp.split(":")[1]);
                    synchronized (this) {
                        if (termRemoto > currentTerm) virarFollower(termRemoto);
                    }
                }
            }).start();
        }
    }

    // --- SOCKET P2P & TRANSAÇÃO ---
    private void iniciarServidorP2P() {
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(portaP2P);
                while (rodando) {
                    Socket client = serverSocket.accept();
                    new Thread(() -> tratarMensagem(client)).start();
                }
            } catch (IOException e) { e.printStackTrace(); }
        }).start();
    }

    private void tratarMensagem(Socket socket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
             
            String msg = in.readLine();
            if (msg == null) return;
            String[] partes = msg.split(":");
            String tipo = partes[0];

            synchronized (this) {
                if (tipo.equals("HEARTBEAT")) {
                    int termoRemoto = Integer.parseInt(partes[1]);
                    if (termoRemoto >= currentTerm) {
                        currentTerm = termoRemoto;
                        liderId = Integer.parseInt(partes[2]);
                        estadoAtual = State.FOLLOWER;
                        lastHeartbeatTime = System.currentTimeMillis();
                        out.println("OK");
                    } else out.println("TERM_HIGHER:" + currentTerm);
                }
                else if (tipo.equals("VOTE_REQ")) {
                    int termoCandidato = Integer.parseInt(partes[1]);
                    int idCandidato = Integer.parseInt(partes[2]);
                    if (termoCandidato > currentTerm) virarFollower(termoCandidato);
                    if (termoCandidato >= currentTerm && (votedFor == -1 || votedFor == idCandidato)) {
                        votedFor = idCandidato;
                        lastHeartbeatTime = System.currentTimeMillis();
                        out.println("VOTE_AGREE");
                    } else out.println("VOTE_DENY");
                }
                else if (tipo.equals("PRECO")) {
                    String prod = partes[1];
                    Double preco = estoqueLocal.getOrDefault(prod, -1.0);
                    if (preco > 0) System.out.println(CYAN + "Consultado: " + prod + " -> R$ " + df.format(preco) + RESET);
                    out.println(preco);
                }
                else if (tipo.equals("COMPRA")) {
                    String prod = partes[1];
                    if (estoqueLocal.containsKey(prod)) {
                        // REMOVE O PRODUTO DO ESTOQUE
                        estoqueLocal.remove(prod);
                        // SALVA IMEDIATAMENTE NO DISCO
                        salvarEstoqueNoDisco();
                        System.out.println(GREEN + "$$$ VENDA E PERSISTÊNCIA: " + prod + " $$$" + RESET);
                        out.println("OK");
                    } else {
                        System.out.println(RED + "Erro: Tentativa de compra de item inexistente (" + prod + ")" + RESET);
                        out.println("FALHA");
                    }
                }
            }
        } catch (Exception e) {}
    }

    // --- ORQUESTRAÇÃO (LÍDER) ---
    public boolean orquestrarCompra(String[] produtos) {
        if (estadoAtual != State.LEADER) return false;
        System.out.println(WHITE_BOLD + "\n>>> Processando Compra Distribuída..." + RESET);
        boolean sucessoTotal = true;

        for (String item : produtos) {
            String nomeProduto = item.contains(",") ? item.split(",")[1].trim() : item.trim();
            System.out.print("Cotando '" + nomeProduto + "'... ");
            
            MelhorOferta oferta = cotarPrecoGlobal(nomeProduto);

            if (oferta != null) {
                System.out.printf(GREEN + "Vencedor: Filial %d (R$ %s)\n" + RESET, oferta.filialId, df.format(oferta.preco));
                if (enviarOrdemCompra(oferta.filialId, nomeProduto)) {
                    System.out.println(CYAN + "   -> Transação Commitada na Filial " + oferta.filialId + RESET);
                    // Se a venda foi local, preciso atualizar meu arquivo aqui também
                    if (oferta.filialId == this.id) {
                        estoqueLocal.remove(nomeProduto);
                        salvarEstoqueNoDisco();
                    }
                } else {
                    System.out.println(RED + "   -> Falha no Commit!" + RESET);
                    sucessoTotal = false;
                }
            } else {
                System.out.println(RED + "Indisponível no cluster!" + RESET);
                sucessoTotal = false;
            }
        }
        return sucessoTotal;
    }

    private static class MelhorOferta {
        int filialId; double preco;
        MelhorOferta(int f, double p) { filialId=f; preco=p; }
    }

    private MelhorOferta cotarPrecoGlobal(String produto) {
        MelhorOferta melhor = null;
        if (estoqueLocal.containsKey(produto)) melhor = new MelhorOferta(this.id, estoqueLocal.get(produto));
        for (Map.Entry<Integer, String> vizinho : vizinhos.entrySet()) {
            double p = consultarFilialRemota(vizinho.getValue(), "PRECO:" + produto);
            if (p > 0 && (melhor == null || p < melhor.preco)) melhor = new MelhorOferta(vizinho.getKey(), p);
        }
        return melhor;
    }

    private double consultarFilialRemota(String endereco, String msg) {
        try {
            String resp = enviarMensagem(endereco, msg);
            return (resp == null) ? -1.0 : Double.parseDouble(resp);
        } catch (Exception e) { return -1.0; }
    }

    private boolean enviarOrdemCompra(int idDestino, String produto) {
        if (idDestino == this.id) {
            System.out.println(GREEN + "$$$ Venda Local: " + produto + " $$$" + RESET);
            return true;
        }
        return "OK".equals(enviarMensagem(vizinhos.get(idDestino), "COMPRA:" + produto));
    }

    // --- UTIL ---
    private void resetElectionTimeout() { this.electionTimeout = 5000 + new Random().nextInt(5000); }
    private String enviarMensagem(String end, String msg) {
        try (Socket s = new Socket()) {
            String[] parts = end.split(":");
            s.connect(new InetSocketAddress(parts[0], Integer.parseInt(parts[1])), 2000);
            s.setSoTimeout(2000); 
            PrintWriter out = new PrintWriter(s.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
            out.println(msg);
            return in.readLine();
        } catch (IOException e) { return null; }
    }
    private void publicarWebService() {
        new Thread(() -> {
            int tentativas = 0;
            while (endpointSOAP == null || !endpointSOAP.isPublished()) {
                try {
                    if (estadoAtual != State.LEADER) return;
                    MercadoServidorImpl impl = new MercadoServidorImpl();
                    impl.setFilialLocal(this);
                    endpointSOAP = Endpoint.publish(enderecoSOAP, impl);
                    System.out.println(GREEN + ">> SOAP Online: " + enderecoSOAP + " <<" + RESET);
                    return;
                } catch (Exception e) {
                    tentativas++;
                    System.out.println(RED + "Falha SOAP (Tentativa " + tentativas + ")..." + RESET);
                    try { Thread.sleep(2000); } catch (InterruptedException ex) {}
                }
            }
        }).start();
    }
    private void logEstado(String cor, String msg) { System.out.println(cor + msg + RESET); }
    private void limparTela() { System.out.print("\033[H\033[2J"); System.out.flush(); }
}