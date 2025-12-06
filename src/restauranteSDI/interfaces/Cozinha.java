package restauranteSDI.interfaces;

import java.rmi.*;

public interface Cozinha extends Remote {
    int novoPreparo(int comanda, String[] pedido) throws RemoteException;
    int tempoPreparo(int preparo) throws RemoteException;
    String[] pegarPreparo(int preparo) throws RemoteException;
}