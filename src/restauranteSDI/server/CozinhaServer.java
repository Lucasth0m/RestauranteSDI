package restauranteSDI.server;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class CozinhaServer {
    public static final String RMI_URL = "rmi://localhost:1096/CozinhaService";
    public static final int RMI_PORT = 1096;

    public static void main(String[] args) {
        try {
            CozinhaImpl objCozinha = new CozinhaImpl();
            Registry registry = LocateRegistry.getRegistry(RMI_PORT);
            registry.rebind("CozinhaService", objCozinha);
            System.out.println("Servidor da Cozinha online em: " + RMI_URL);
        } catch (Exception e) {
            System.err.println("Erro no servidor da Cozinha: " + e.toString());
            e.printStackTrace();
        }
    }
}
