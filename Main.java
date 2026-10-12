
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main {

    private static final int MAX_MATRICES = 500;

    public static void main(String[] args) {

        System.out.println("==============================================");
        System.out.println(" Matrix Chain Multiplication Optimization Tool");
        System.out.println("==============================================");

        List<TestCase> testCases;

        try {
            testCases = loadDataset(
                Paths.get("matrix_dataset.csv")
            );
        } catch (IOException e) {
            System.out.println(
                "Cannot read matrix_dataset.csv."
            );
            System.out.println(
                "Keep it in the same folder as Main.java."
            );
            return;
        }

        if (testCases.isEmpty()) {
            System.out.println("No valid test cases found.");
            return;
        }

        System.out.println("\n========== AVAILABLE TEST CASES ==========");

        for (int i = 0; i < testCases.size(); i++) {
            TestCase tc = testCases.get(i);

            System.out.printf(
                "%d. %s | Dimensions: %s | %s%n",
                i + 1,
                tc.id,
                tc.dimensionsText,
                tc.description
            );
        }

        System.out.println(
            "\nEnter a test case number, or 0 for custom dimensions."
        );
        System.out.print("Your choice: ");

        try (Scanner scanner = new Scanner(System.in)) {

            if (!scanner.hasNextInt()) {
                System.out.println("Please enter a valid number.");
                return;
            }

            int choice = scanner.nextInt();
            int[] dimensions;

            if (choice == 0) {

                scanner.nextLine();

                System.out.println(
                    "\nEnter dimensions separated by spaces."
                );
                System.out.println(
                    "Example: 40 20 30 10"
                );
                System.out.print("Dimensions: ");

                try {
                    dimensions = parseDimensions(
                        scanner.nextLine()
                    );
                } catch (IllegalArgumentException e) {
                    System.out.println(
                        "Input error: " + e.getMessage()
                    );
                    return;
                }

            } else if (
                choice >= 1 && choice <= testCases.size()
            ) {

                dimensions =
                    testCases.get(choice - 1).dimensions;

            } else {

                System.out.println("Invalid test case number.");
                return;
            }

            try {
                runOptimization(dimensions);
            } catch (IllegalArgumentException e) {
                System.out.println(
                    "Input error: " + e.getMessage()
                );
            }
        }
    }

    private static void runOptimization(int[] dimensions) {

        MatrixChain optimizer = new MatrixChain(dimensions);

        long optimalCost = optimizer.getMinimumCost();
        long leftCost = optimizer.getLeftToRightCost();
        long savings = leftCost - optimalCost;

        double percentage = 0.0;

        if (leftCost > 0) {
            percentage = (savings * 100.0) / leftCost;
        }

        System.out.println("\n==============================================");
        System.out.println("              OPTIMIZATION RESULTS");
        System.out.println("==============================================");

        System.out.println(
            "Number of matrices: "
            + optimizer.getMatrixCount()
        );

        System.out.println(
            "Dimensions: " + joinDimensions(dimensions)
        );

        System.out.println(
            "Optimal parenthesization: "
            + optimizer.getOptimalParenthesization()
        );

        System.out.println("\n============== COST COMPARISON ==============");

        System.out.printf(
            "%-35s %15s%n",
            "Multiplication Method",
            "Scalar Cost"
        );

        System.out.println(
            "---------------------------------------------------"
        );

        System.out.printf(
            "%-35s %,15d%n",
            "Left-to-right multiplication",
            leftCost
        );

        System.out.printf(
            "%-35s %,15d%n",
            "Optimal DP multiplication",
            optimalCost
        );

        System.out.println(
            "---------------------------------------------------"
        );

        System.out.printf(
            "%-35s %,15d%n",
            "Scalar multiplications saved",
            savings
        );

        System.out.printf(
            "Percentage reduction: %.2f%%%n",
            percentage
        );

        System.out.println(
            "\n============== DYNAMIC PROGRAMMING TABLES =============="
        );

        if (optimizer.getMatrixCount() <= 10) {
            optimizer.printTables();
        } else {
            System.out.println(
                "Tables are hidden for chains above 10 matrices."
            );
            System.out.println(
                "This keeps the output readable for larger inputs."
            );
        }

        System.out.println("\n============== COMPLEXITY ANALYSIS ==============");

        System.out.println(
            "Time complexity: O(n^3)"
        );

        System.out.println(
            "Space complexity: O(n^2)"
        );

        System.out.println("\nOptimization completed successfully.");
    }

    private static List<TestCase> loadDataset(Path path)
            throws IOException {

        List<TestCase> cases = new ArrayList<>();

        try (BufferedReader reader =
                 Files.newBufferedReader(path)) {

            String line;
            boolean firstLine = true;

            while ((line = reader.readLine()) != null) {

                if (line.trim().isEmpty()) {
                    continue;
                }

                if (firstLine) {
                    firstLine = false;

                    if (
                        line.toLowerCase().startsWith("testcase,")
                    ) {
                        continue;
                    }
                }

                String[] columns = line.split(",", 4);

                if (columns.length < 4) {
                    System.out.println(
                        "Skipping invalid CSV row: " + line
                    );
                    continue;
                }

                String id = columns[0].trim();

                String dimensionText =
                    columns[1].trim().replace("\"", "");

                String description = columns[3].trim();

                try {

                    int[] dimensions =
                        parseDimensions(dimensionText);

                    cases.add(
                        new TestCase(
                            id,
                            dimensionText,
                            description,
                            dimensions
                        )
                    );

                } catch (IllegalArgumentException e) {

                    System.out.println(
                        "Skipping " + id + ": " + e.getMessage()
                    );
                }
            }
        }

        return cases;
    }

    private static int[] parseDimensions(String input) {

        if (input == null || input.trim().isEmpty()) {
            throw new IllegalArgumentException(
                "Please enter matrix dimensions."
            );
        }

        String[] parts = input.trim().split("[\\s,]+");

        if (parts.length < 2) {
            throw new IllegalArgumentException(
                "Enter at least two dimensions for one matrix."
            );
        }

        if (parts.length - 1 > MAX_MATRICES) {
            throw new IllegalArgumentException(
                "A maximum of 500 matrices is supported."
            );
        }

        int[] dimensions = new int[parts.length];

        for (int i = 0; i < parts.length; i++) {

            try {
                dimensions[i] = Integer.parseInt(parts[i]);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(
                    parts[i] + " is not a valid integer."
                );
            }

            if (dimensions[i] <= 0) {
                throw new IllegalArgumentException(
                    "All dimensions must be positive integers."
                );
            }
        }

        return dimensions;
    }

    private static String joinDimensions(int[] dimensions) {

        StringBuilder result = new StringBuilder();

        for (int i = 0; i < dimensions.length; i++) {

            if (i > 0) {
                result.append(" ");
            }

            result.append(dimensions[i]);
        }

        return result.toString();
    }

    private static class TestCase {

        String id;
        String dimensionsText;
        String description;
        int[] dimensions;

        TestCase(
            String id,
            String dimensionsText,
            String description,
            int[] dimensions
        ) {
            this.id = id;
            this.dimensionsText = dimensionsText;
            this.description = description;
            this.dimensions = dimensions;
        }
    }
}
