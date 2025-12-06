package restauranteSDI.server;

import java.util.HashMap;
import java.util.Map;

public class MercadoPublisher {

    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("Uso: java MercadoPublisher <ID> <PORTA_P2P> <PORTA_SOAP>");
            System.out.println("Exemplo default (Filial 1): java MercadoPublisher 1 5001 9000");
            args = new String[]{"1", "5001", "9000"};
        }

        int id = Integer.parseInt(args[0]);
        int portaP2P = Integer.parseInt(args[1]);
        int portaSOAP = Integer.parseInt(args[2]);

        String soapUrl = "http://localhost:" + portaSOAP + "/mercado";

        Map<Integer, String> vizinhos = new HashMap<>();
        vizinhos.put(1, "localhost:5001");
        vizinhos.put(2, "localhost:5002");
        vizinhos.put(3, "localhost:5003");
        vizinhos.put(4, "localhost:5004");
        vizinhos.put(5, "localhost:5005");
        
        // Remove a si mesmo da lista de vizinhos
        vizinhos.remove(id);

        System.out.println("Iniciando Filial " + id);
        System.out.println("Porta P2P: " + portaP2P);
        System.out.println("URL SOAP Potencial: " + soapUrl);

        // Inicializa a Filial (que inicia as threads e eleição)
        new Filial(id, portaP2P, soapUrl, vizinhos);
    }
}