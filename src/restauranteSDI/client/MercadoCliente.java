package restauranteSDI.client;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import restauranteSDI.interfaces.MercadoServidor;
import java.net.URL;

public class MercadoCliente {
    private String wsdlUrl;

    // O construtor agora recebe a URL completa do WSDL
    public MercadoCliente(String wsdlUrl) {
        this.wsdlUrl = wsdlUrl;
    }

    public boolean comprar(String[] produtos) {
        try {
            URL url = new URL(this.wsdlUrl);
            
            // 1. Namespace: "http://implementacoes/" (vem do targetNamespace da interface)
            // 2. Service Name: "MercadoServidorImplService" (nome padrão gerado)
            QName qname = new QName("http://implementacoes/", "MercadoServidorImplService");

            Service service = Service.create(url, qname);
            MercadoServidor mercado = service.getPort(MercadoServidor.class);
            
            int pedidoId = mercado.cadastrarPedido("Restaurante SDI");
            System.out.println("MercadoCliente: Pedido #" + pedidoId + " cadastrado no mercado.");
            
            // O método agora espera o ID do pedido (que chamamos de 'restaurante' na interface)
            return mercado.comprarProdutos(pedidoId, produtos);
            
        } catch (Exception e) {
            System.err.println("Erro ao conectar com o mercado: " + e.getMessage());
            e.printStackTrace(); // Ajuda a ver o erro completo
            return false;
        }
    }
}