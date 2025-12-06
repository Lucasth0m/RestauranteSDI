package restauranteSDI.server;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class RestauranteServer {
    public static final String RMI_URL = "rmi://localhost:1110/RestauranteService";
    public static final int RMI_PORT = 1110;

    public static void main(String[] args) {
        try {
            RestauranteImpl objRestaurante = new RestauranteImpl();

            // Conecta ao RMI Registry existente na porta 1100
            Registry registry = LocateRegistry.getRegistry(RMI_PORT);
            registry.rebind("RestauranteService", objRestaurante);

            System.out.println("Servidor do Restaurante está online em: " + RMI_URL);
        } catch (Exception e) {
            System.err.println("Erro no servidor do Restaurante: " + e.toString());
            e.printStackTrace();
        }
    }
}