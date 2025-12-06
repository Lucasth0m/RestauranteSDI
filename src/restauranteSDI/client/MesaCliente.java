package restauranteSDI.client;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import restauranteSDI.interfaces.*;
import restauranteSDI.server.*;

public class MesaCliente {

    private static Restaurante restaurante;
    private static Scanner scanner = new Scanner(System.in);
    private static String[] cardapio;
    private static List<String> pedidoAtual = new ArrayList<>();
    private static List<String> pedidosEmPreparo = new ArrayList<>();
    private static int minhaComanda;

    public static void main(String[] args) {
        try {
            // 1. Conecta ao servidor RMI do Restaurante
            Registry registry = LocateRegistry.getRegistry("localhost", RestauranteServer.RMI_PORT);
            restaurante = (Restaurante) registry.lookup("RestauranteService");
            System.out.println("✅ Cliente da Mesa conectado ao Restaurante!");

            minhaComanda = restaurante.novaComanda("Cliente Interativo", 10);
            System.out.println("=> Comanda aberta: #" + minhaComanda);

            cardapio = restaurante.consultarCardapio();
            if (cardapio.length == 0 || cardapio[0].contains("Falha ao carregar")) {
                System.out.println("❌ Erro: Não foi possível carregar o cardápio do restaurante. Encerrando.");
                return;
            }

            loopPrincipal();
            
            fecharConta();

        } catch (Exception e) {
            System.err.println("Erro fatal no cliente da Mesa: " + e.toString());
            e.printStackTrace();
        } finally {
            scanner.close();
            System.out.println("Cliente encerrado.");
        }
    }

    private static void loopPrincipal() throws Exception {
        while (true) {
            exibirMenuDeOpcoes();
            String input = scanner.nextLine().trim();

            if ("fim".equalsIgnoreCase(input)) {
                enviarPedido();
            } else if ("pagar".equalsIgnoreCase(input)) {
                if (!pedidoAtual.isEmpty()) {
                    System.out.println("\n⚠️ Você tem itens no pedido atual. Envie ('fim') ou cancele ('cancelar') antes de pagar.");
                } else {
                    break;
                }
            } else if ("cancelar".equalsIgnoreCase(input)) {
                pedidoAtual.clear();
                System.out.println("✅ Pedido atual cancelado.");
            } else {
                adicionarItemAoPedido(input);
            }
        }
    }

    private static void exibirMenuDeOpcoes() {
        System.out.println("\n\n+------------------------------------------+");
        System.out.println("|               CARDÁPIO                   |");
        System.out.println("+------------------------------------------+");
        for (String item : cardapio) {
            String[] partes = item.split(",");
            System.out.printf("| %-3s - %-25s R$ %-5s |\n", partes[0], partes[1], partes[2]);
        }
        System.out.println("+------------------------------------------+");

        if (!pedidoAtual.isEmpty()) {
            System.out.println("\nSeu pedido atual:");
            for (String item : pedidoAtual) {
                System.out.println("  - " + item.split(",")[1]);
            }
        }

        if (!pedidosEmPreparo.isEmpty()) {
            System.out.println("\n⏳ Pedidos em preparo: " + String.join(", ", pedidosEmPreparo));
        }

        System.out.println("\n--- Opções ---");
        System.out.print("Digite o CÓDIGO do produto para adicionar | 'fim' para enviar | 'pagar' para fechar a conta | 'cancelar' o pedido atual: ");
    }

    private static void adicionarItemAoPedido(String codigoInput) {
        try {
            int codigo = Integer.parseInt(codigoInput);
            String itemEncontrado = null;
            for (String itemDoCardapio : cardapio) {
                if (itemDoCardapio.startsWith(codigo + ",")) {
                    itemEncontrado = itemDoCardapio;
                    break;
                }
            }

            if (itemEncontrado != null) {
                pedidoAtual.add(itemEncontrado);
                System.out.println("=> Adicionado: " + itemEncontrado.split(",")[1]);
            } else {
                System.out.println("❌ Código inválido. Nenhum produto encontrado com este código.");
            }
        } catch (NumberFormatException e) {
            System.out.println("❌ Entrada inválida. Por favor, digite um código numérico ou uma das opções válidas.");
        }
    }

    private static void enviarPedido() throws Exception {
        if (pedidoAtual.isEmpty()) {
            System.out.println("⚠️ Nenhum item no pedido. Adicione algo antes de enviar.");
            return;
        }

        String statusPedido = restaurante.fazerPedido(minhaComanda, pedidoAtual.toArray(new String[0]));
        System.out.println("\n✅ " + statusPedido);

        try {
            String codigoPreparo = statusPedido.substring(statusPedido.indexOf("#"), statusPedido.indexOf(" enviado"));
            pedidosEmPreparo.add(codigoPreparo);
        } catch (Exception e) {
            System.out.println("Não foi possível extrair o código de preparo da resposta do servidor.");
        }

        pedidoAtual.clear();
    }

    private static void fecharConta() throws Exception {
        System.out.println("\n------------------------------------------");
        System.out.println("Encerrando a conta...");
        float total = restaurante.valorComanda(minhaComanda);

        if (total >= 0) {
            System.out.printf("=> Valor TOTAL da comanda: R$ %.2f\n", total);
        } else {
            System.out.println("=> Não foi possível obter o valor da comanda.");
        }
        
        if (restaurante.fecharComanda(minhaComanda)) {
            System.out.println("=> Comanda #" + minhaComanda + " fechada com sucesso. Obrigado e volte sempre!");
        } else {
            System.out.println("=> Erro ao tentar fechar a comanda no servidor.");
        }
    }
}