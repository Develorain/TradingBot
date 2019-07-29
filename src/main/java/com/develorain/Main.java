package com.develorain;

import org.jgrapht.graph.SimpleDirectedWeightedGraph;

import java.io.FileWriter;
import java.util.Scanner;

public class Main {
    public static final Double TRANSACTION_FEE_RATIO = 1 - 0.000750;
    public static final int HOW_MANY_CYCLES_TO_OUTPUT = 100;

    private Main() {
        BinanceAPICaller.initialize();

        SimpleDirectedWeightedGraph<String, CustomEdge> graph = createGraph();

        Cycle[] sortedCyclesByMultiplier = GraphProcessing.getSortedCyclesByMultiplier(graph);

        writeCyclesToFile(sortedCyclesByMultiplier, graph);

        // Actually does the first trade of a cycle
        /*
        System.out.println("Preparing to do order");

        BinanceAPICaller.performCycle(sortedCyclesByMultiplier[0], true);

        promptEnterKey();

        System.out.println("Doing order");

        if (sortedCyclesByMultiplier[0].edges[0].sourceNode.equalsIgnoreCase("BNB")) {
            System.out.println("Cycle starts with BNB, actually doing cycle");
            BinanceAPICaller.performCycle(sortedCyclesByMultiplier[0], false);
        } else {
            System.out.println("Cycle does not start with BNB, cancelling order");
        }
        */
    }

    public void promptEnterKey(){
        System.out.println("Press \"ENTER\" to continue...");
        Scanner scanner = new Scanner(System.in);
        scanner.nextLine();
    }

    private SimpleDirectedWeightedGraph<String, CustomEdge> createGraph() {
        SimpleDirectedWeightedGraph<String, CustomEdge> graph = new SimpleDirectedWeightedGraph<>(CustomEdge.class);

        BinanceAPICaller.createGraphNodes(graph);
        BinanceAPICaller.createGraphEdges(graph);

        return graph;
    }

    private void writeCyclesToFile(Cycle[] sortedCyclesByMultiplier, SimpleDirectedWeightedGraph<String, CustomEdge> graph) {
        try {
            FileWriter fileWriter = new FileWriter("cycles.txt");

            fileWriter.write("\n\n\n\n\n");

            for (int i = 0; i < HOW_MANY_CYCLES_TO_OUTPUT; i++) {
                Cycle cycle = sortedCyclesByMultiplier[i];

                // Write cycle to file
                fileWriter.write(cycle.toString() + "\n");

                fileWriter.write("Start cycle with " + Tools.formatAmount(cycle.edges[0].tradeQuantity) + " " + cycle.cycleString.get(0) + "\n\n");

                // Write cycle data after cycle
                for (int j = 0; j < cycle.size; j++) {
                    String sourceNode = cycle.cycleString.get(j);
                    String targetNode = cycle.cycleString.get((j + 1) % cycle.size);

                    CustomEdge sourceToTargetEdge = graph.getEdge(sourceNode, targetNode);

                    if (sourceToTargetEdge.amISellingBaseCurrency()) {
                        fileWriter.write("Using bottom-half price\n");
                        fileWriter.write(sourceNode + "--->" + targetNode + " | " + "Worst case price >= " + Tools.formatPrice(cycle.edges[j].worstCaseTradePrice()) + " Average case price = " + Tools.formatPrice(cycle.edges[j].averageCaseTradePrice()) + " \n\n");
                    } else {
                        fileWriter.write("Using top-half price\n");
                        fileWriter.write(sourceNode + "--->" + targetNode + " | " + "Worst case price <= " + Tools.formatPrice(cycle.edges[j].worstCaseTradePrice()) + " Average case price = " + Tools.formatPrice(cycle.edges[j].averageCaseTradePrice()) + " \n\n");
                    }
                }

                fileWriter.write("\n\n\n");
            }

            fileWriter.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        new Main();

        System.out.println("Done");
        System.exit(0);
    }
}