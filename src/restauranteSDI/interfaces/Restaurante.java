package restauranteSDI.interfaces;

import java.rmi.*;

public interface Restaurante extends Remote {
    int novaComanda(String nome, int mesa) throws RemoteException;
    String[] consultarCardapio() throws RemoteException;
    String fazerPedido(int comanda, String[] pedido) throws RemoteException;
    float valorComanda(int comanda) throws RemoteException;
    boolean fecharComanda(int comanda) throws RemoteException;
    String reporEstoque(String[] produtos) throws RemoteException;
}