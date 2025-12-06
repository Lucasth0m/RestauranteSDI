package restauranteSDI.server;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import restauranteSDI.client.*;
import restauranteSDI.interfaces.*;



public class RestauranteImpl extends UnicastRemoteObject implements Restaurante {

    private Cozinha cozinha;
    private MercadoCliente mercadoCliente;
    private ConcurrentHashMap<Integer, Float> comandas;
    private AtomicInteger contadorComandas = new AtomicInteger(0);
    private List<String> cardapio;

    protected RestauranteImpl() throws RemoteException {
        super();
        comandas = new ConcurrentHashMap<>();
        try {
            // Cozinha via RMI
            Registry registryCozinha = LocateRegistry.getRegistry("localhost", CozinhaServer.RMI_PORT);
            this.cozinha = (Cozinha) registryCozinha.lookup("CozinhaService");
            System.out.println("Restaurante: Conectado à cozinha com sucesso.");

            //Cliente para o Mercado
            this.mercadoCliente = new MercadoCliente("http://localhost:9000/mercado?wsdl");//mudar aqui
            System.out.println("Restaurante: Cliente do mercado inicializado.");

            this.cardapio = carregarCardapio("data/menu_restaurante.csv");
            System.out.println("Restaurante: Cardápio carregado com " + this.cardapio.size() + " itens.");

        } catch (Exception e) {
            System.err.println("Restaurante: Erro ao conectar com serviços externos: " + e.toString());
            e.printStackTrace();
            throw new RemoteException("Falha na inicialização do restaurante.", e);
        }
    }

    private List<String> carregarCardapio(String caminhoArquivo) {
        List<String> menu = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(caminhoArquivo))) {
            String linha;
            br.readLine();
            while ((linha = br.readLine()) != null) {
                menu.add(linha);
            }
        } catch (IOException e) {
            System.err.println("Restaurante: Erro ao carregar o cardápio do arquivo: " + e.getMessage());
            menu.add("0,Falha ao carregar cardapio,0.0");
        }
        return menu;
    }

    @Override
    public int novaComanda(String nome, int mesa) throws RemoteException {
        int idComanda = contadorComandas.incrementAndGet();
        comandas.put(idComanda, 0.0f);
        System.out.println("Restaurante: Nova comanda #" + idComanda + " aberta para " + nome + " na mesa " + mesa);
        return idComanda;
    }

    @Override
    public String[] consultarCardapio() throws RemoteException {
        return this.cardapio.toArray(new String[0]);
    }

    @Override
    public String fazerPedido(int comanda, String[] pedido) throws RemoteException {
        // Simula a necessidade de repor o estoque aleatoriamente
        if (new Random().nextInt(4) == 0 && pedido.length > 0) { // 25% de chance de precisar
            System.out.println("Restaurante: Estoque baixo detectado para o pedido da comanda " + comanda + ". Acionando mercado...");
            
            String[] produtosParaComprar = new String[pedido.length];
            for (int i = 0; i < pedido.length; i++) {
                produtosParaComprar[i] = pedido[i].split(",")[1];
            }
            
            String statusCompra = reporEstoque(produtosParaComprar);
            System.out.println("Restaurante: Status da compra no mercado: " + statusCompra);
        }

        System.out.println("Restaurante: Enviando pedido para a cozinha para a comanda " + comanda);
        int codigoPreparo = cozinha.novoPreparo(comanda, pedido);
        int tempoEspera = cozinha.tempoPreparo(codigoPreparo);

        float valorTotalDoPedido = 0;
        for (String item : pedido) {
            String[] partes = item.split(",");
            if (partes.length == 3) {
                try {
                    valorTotalDoPedido += Float.parseFloat(partes[2]);
                } catch (NumberFormatException e) {
                    System.err.println("Restaurante: Item com valor inválido no pedido: " + item);
                }
            }
        }
        
        final float valorASerAdicionado = valorTotalDoPedido;
        comandas.computeIfPresent(comanda, (k, v) -> v + valorASerAdicionado);
        
        return "Pedido #" + codigoPreparo + " enviado para a cozinha. Tempo de espera: " + tempoEspera + " segundos.";
    }
    
    @Override
    public float valorComanda(int comanda) throws RemoteException {
        return comandas.getOrDefault(comanda, -1.0f);
    }

    @Override
    public boolean fecharComanda(int comanda) throws RemoteException {
        if (comandas.remove(comanda) != null) {
            System.out.println("Restaurante: Comanda #" + comanda + " fechada.");
            return true;
        }
        return false;
    }

    @Override
    public String reporEstoque(String[] produtos) throws RemoteException {
        System.out.println("Restaurante: Solicitando compra de " + produtos.length + " produtos ao mercado.");
        boolean sucesso = mercadoCliente.comprar(produtos);
        return sucesso ? "Compra realizada com sucesso." : "Falha na comunicação com o mercado.";
    }
}