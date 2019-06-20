package com.develorain;

import com.binance.api.client.BinanceApiClientFactory;
import com.binance.api.client.BinanceApiRestClient;
import com.binance.api.client.domain.general.Asset;
import com.binance.api.client.domain.market.BookTicker;
import org.jgrapht.alg.cycle.JohnsonSimpleCycles;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Main {
    private static final Double TRANSACTION_FEE_RATIO = 1 - 0.000750;

    private Main()  {
        // Set up binance client
        BinanceApiClientFactory factory = BinanceApiClientFactory.newInstance("My2zlMkv4yorboQMABkUSqcNosJEqVZNi6JzPEvQovzbiVusGrf0ZkLF9rHkQAe7", "nUecuN1O33QAYXLdY76s12BME3fLafphBhj0kUl67Cs3seYxp8xzJ8JqVD7mYwJr");
        BinanceApiRestClient client = factory.newRestClient();

        // Create graph
        SimpleDirectedWeightedGraph<String, CustomEdge> graph = new SimpleDirectedWeightedGraph<>(CustomEdge.class);
        createGraphNodes(client, graph);
        createGraphEdgesAPI(client, graph);
        generateCycles(graph);

        System.out.println("Done");
        System.exit(0);
    }

    private void generateCycles(SimpleDirectedWeightedGraph<String, CustomEdge> graph) {
        try {
            FileWriter fileWriter2 = new FileWriter("amounts.txt");

            Double balance = 100.0;

            ArrayList<Tuple> cycleMoneyTuples = new ArrayList<>();

            JohnsonSimpleCycles<String, CustomEdge> johnsonSimpleCycles = new JohnsonSimpleCycles<>(graph);
            for (List<String> cycle : johnsonSimpleCycles.findSimpleCycles()) {
                if (cycle.size() > 2 && cycle.size() < 5) {
                    String originalCurrency = cycle.get(0);
                    System.out.println(originalCurrency);

                    int size = cycle.size();

                    ArrayList<Double> conversions = new ArrayList<>();
                    ArrayList<Double> amounts = new ArrayList<>();

                    // Computes profit
                    for (int i = 0; i < size; i++) {
                        conversions.add(graph.getEdge(cycle.get(i), cycle.get((i+1) % size)).getPrice());
                        amounts.add(graph.getEdge(cycle.get(i), cycle.get((i+1) % size)).getAmount());
                    }

                    for (int i = 0; i < size; i++) {
                        balance = balance * conversions.get(i) * TRANSACTION_FEE_RATIO;
                    }

                    for (int i = 1; i < size; i++) {
                        for (int j = 1; j <= i; j++) {
                            amounts.set(j, amounts.get(j) * conversions.get(i));
                        }
                    }

                    Double minimumAmountToTrade = Double.MAX_VALUE;

                    for (Double num : amounts) {
                        minimumAmountToTrade = Math.min(minimumAmountToTrade, num);
                    }

                    fileWriter2.write(cycle + ", " + minimumAmountToTrade + "\n");


                    // Add to array list if valid price
                    if (balance != Double.POSITIVE_INFINITY && balance != Double.NEGATIVE_INFINITY && balance != 0.0 && !Double.isNaN(balance)) {
                        cycleMoneyTuples.add(new Tuple(cycle, balance));
                    }

                    balance = 100.0;
                }
            }

            fileWriter2.close();

            FileWriter fileWriter = new FileWriter("cycles.txt");

            // Sort tuples by price
            Object[] array = cycleMoneyTuples.toArray();
            Arrays.sort(array);

            for (Object a : array) {
                Tuple woo = (Tuple) a;
                fileWriter.write(woo.toString() + "\n");
            }

            fileWriter.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void createGraphNodes(BinanceApiRestClient client, SimpleDirectedWeightedGraph<String, CustomEdge> graph) {
        // Make nodes with each currency
        for (Asset asset : client.getAllAssets()) {
            // ONLY TAKES NODES WITH 3 LETTERS
            String assetCode = asset.getAssetCode();

            if (assetCode.length() == 3) {
                graph.addVertex(assetCode);
                //System.out.println("Added node: " + assetCode);
            }
        }
    }

    private void createGraphEdgesAPI(BinanceApiRestClient client, SimpleDirectedWeightedGraph<String, CustomEdge> graph) {
        for (BookTicker bookTicker : client.getBookTickers()) {
            String symbol = bookTicker.getSymbol();

            if (symbol.length() != 6) {
                continue;
            }

            // Get asset codes for base and quote
            // REPLACE THIS WITH REGEX
            System.out.println(symbol);
            String baseAssetCode = symbol.substring(0, 3);
            String quoteAssetCode = symbol.substring(3, 6);

            if (!graph.containsVertex(baseAssetCode)) {
                System.out.println("Node does not exist: " + baseAssetCode);
                continue;
            }

            if (!graph.containsVertex(quoteAssetCode)) {
                System.out.println("Node does not exist: " + quoteAssetCode);
                continue;
            }

            // Connect these currencies with their corresponding weights
            try {
                CustomEdge baseToQuoteEdge = new CustomEdge(Double.parseDouble(bookTicker.getAskPrice()), Double.parseDouble(bookTicker.getAskQty()));
                graph.addEdge(baseAssetCode, quoteAssetCode, baseToQuoteEdge);
                //graph.setEdgeWeight(baseToQuoteEdge, Double.parseDouble(bookTicker.getAskPrice()));

                CustomEdge quoteToBaseEdge = new CustomEdge(1.0/Double.parseDouble(bookTicker.getBidPrice()), Double.parseDouble(bookTicker.getBidQty()));
                graph.addEdge(quoteAssetCode, baseAssetCode, quoteToBaseEdge);
                //graph.setEdgeWeight(quoteToBaseEdge, 1.0/Double.parseDouble(bookTicker.getBidPrice()));
            } catch (NullPointerException e) {
                System.out.println("Problematic: " + symbol);
            }
        }
    }

    private void createGraphEdgesOffline(SimpleDirectedWeightedGraph<String, CustomEdge> graph) throws IOException {
        // Uses file to make all edges between nodes with weights
        BufferedReader bufferedReader = new BufferedReader(new FileReader("data.txt"));
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            String[] elements = line.split(", ");
            String baseAssetCode = elements[0].substring(0, 3);
            String quoteAssetCode = elements[0].substring(3, 6);

            CustomEdge baseToQuoteEdge = graph.addEdge(baseAssetCode, quoteAssetCode);
            graph.setEdgeWeight(baseToQuoteEdge, Double.parseDouble(elements[1]));

            CustomEdge quoteToBaseEdge = graph.addEdge(quoteAssetCode, baseAssetCode);
            graph.setEdgeWeight(quoteToBaseEdge, 1.0 / Double.parseDouble(elements[2]));
        }

        bufferedReader.close();
    }

    public static void main(String[] args) {
        new Main();
    }
}