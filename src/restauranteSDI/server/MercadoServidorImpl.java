package restauranteSDI.server;

import javax.jws.WebService;
import restauranteSDI.interfaces.MercadoServidor;
import java.util.Random;

@WebService(endpointInterface = "restauranteSDI.interfaces.MercadoServidor",
            serviceName = "MercadoServidorImplService",
            targetNamespace = "http://implementacoes/")
public class MercadoServidorImpl implements MercadoServidor {

    private static Filial filialLocal;

    public void setFilialLocal(Filial f) {
        filialLocal = f;
    }

    @Override
    public int cadastrarPedido(String restaurante) {
        int id = new Random().nextInt(100000);
        System.out.println("SOAP: Recebido cadastro de pedido de " + restaurante + " -> ID Gerado: " + id);
        return id;
    }

    @Override
    public boolean comprarProdutos(int pedidoId, String[] produtos) {
        if (filialLocal == null) {
            System.err.println("Erro: Serviço SOAP recebeu requisição mas não tem Filial associada.");
            return false;
        }
        
        System.out.println("SOAP: Processando pedido #" + pedidoId + " com " + produtos.length + " itens.");
        
        return filialLocal.orquestrarCompra(produtos);
    }

    @Override
    public int tempoEntrega(int pedidoId) {
        return 15; 
    }

    @Override
    public void notificarLider(int termo, int liderId) {
        System.out.println("SOAP: Recebida notificação de líder (Compatibilidade Enzo): Termo " + termo + ", ID " + liderId);
        // Não precisamos fazer nada aqui, pois sua lógica de eleição é diferente
    }
}