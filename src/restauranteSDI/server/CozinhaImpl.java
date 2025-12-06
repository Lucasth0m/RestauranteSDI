package restauranteSDI.server;

import java.rmi.server.UnicastRemoteObject;
import java.rmi.RemoteException;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import restauranteSDI.interfaces.*;

public class CozinhaImpl extends UnicastRemoteObject implements Cozinha {
    private ConcurrentHashMap<Integer, String[]> pedidosEmPreparo;

    protected CozinhaImpl() throws RemoteException {
        super();
        pedidosEmPreparo = new ConcurrentHashMap<>();
    }

    @Override
    public int novoPreparo(int comanda, String[] pedido) throws RemoteException {
        System.out.println("Cozinha: Recebido pedido da comanda " + comanda);
        int codigoPreparo = new Random().nextInt(10000);
        pedidosEmPreparo.put(codigoPreparo, pedido);
        System.out.println("Cozinha: Pedido " + codigoPreparo + " em preparo.");
        return codigoPreparo;
    }

    @Override
    public int tempoPreparo(int preparo) throws RemoteException {
        int tempo = 5 + new Random().nextInt(11);
        System.out.println("Cozinha: Tempo estimado para o preparo " + preparo + " é de " + tempo + "s.");
        return tempo;
    }

    @Override
    public String[] pegarPreparo(int preparo) throws RemoteException {
        System.out.println("Cozinha: Entregando o preparo " + preparo);
        return pedidosEmPreparo.remove(preparo);
    }
}