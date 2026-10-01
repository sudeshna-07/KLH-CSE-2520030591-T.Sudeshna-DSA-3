import java.util.Scanner;

public class MatrixChainOptimization {

    // Stores the minimum multiplication cost
    static long[][] cost;

    // Stores the split position for optimal parenthesization
    static int[][] split;

    // Dynamic Programming approach
    static long matrixChainDP(int[] dimensions, int n) {

        cost = new long[n + 1][n + 1];
        split = new int[n + 1][n + 1];

        // Cost of multiplying one matrix is zero
        for (int i = 1; i <= n; i++) {
            cost[i][i] = 0;
        }

        // chainLength = number of matrices in the chain
        for (int chainLength = 2; chainLength <= n; chainLength++) {

            for (int i = 1; i <= n - chainLength + 1; i++) {

                int j = i + chainLength - 1;

                cost[i][j] = Long.MAX_VALUE;

                // Try every possible split
                for (int k = i; k < j; k++) {

                    long currentCost =
                            cost[i][k]
                            + cost[k + 1][j]
                            + (long) dimensions[i - 1]
                            * dimensions[k]
                            * dimensions[j];

                    if (currentCost < cost[i][j]) {
                        cost[i][j] = currentCost;
                        split[i][j] = k;
                    }
                }
            }
        }

        return cost[1][n];
    }

    // Prints the optimal parenthesization
    static void printParenthesization(int i, int j) {

        if (i == j) {
            System.out.print("A" + i);
            return;
        }

        System.out.print("(");

        printParenthesization(i, split[i][j]);

        System.out.print(" X ");

        printParenthesization(split[i][j] + 1, j);

        System.out.print(")");
    }

    // Naïve recursive Matrix Chain Multiplication
    static long matrixChainRecursive(int[] dimensions, int i, int j) {

        if (i == j) {
            return 0;
        }

        long minimum = Long.MAX_VALUE;

        for (int k = i; k < j; k++) {

            long left = matrixChainRecursive(dimensions, i, k);

            long right = matrixChainRecursive(dimensions, k + 1, j);

            long multiplication =
                    (long) dimensions[i - 1]
                    * dimensions[k]
                    * dimensions[j];

            long total = left + right + multiplication;

            if (total < minimum) {
                minimum = total;
            }
        }

        return minimum;
    }

    // Displays DP cost table
    static void displayCostTable(int n) {

        System.out.println("\nMinimum Cost Table:");
        System.out.println();

        System.out.print("      ");

        for (int j = 1; j <= n; j++) {
            System.out.printf("%10s", "A" + j);
        }

        System.out.println();

        for (int i = 1; i <= n; i++) {

            System.out.printf("%-6s", "A" + i);

            for (int j = 1; j <= n; j++) {

                if (j < i) {
                    System.out.printf("%10s", "-");
                } else {
                    System.out.printf("%10d", cost[i][j]);
                }
            }

            System.out.println();
        }
    }

    // Displays split table
    static void displaySplitTable(int n) {

        System.out.println("\nSplit Table:");
        System.out.println();

        System.out.print("      ");

        for (int j = 1; j <= n; j++) {
            System.out.printf("%8s", "A" + j);
        }

        System.out.println();

        for (int i = 1; i <= n; i++) {

            System.out.printf("%-6s", "A" + i);

            for (int j = 1; j <= n; j++) {

                if (j <= i) {
                    System.out.printf("%8s", "-");
                } else {
                    System.out.printf("%8d", split[i][j]);
                }
            }

            System.out.println();
        }
    }

    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);

        System.out.println("==============================================");
        System.out.println("     MATRIX CHAIN MULTIPLICATION TOOL");
        System.out.println("==============================================");

        System.out.print("\nEnter number of matrices: ");
        int n = scanner.nextInt();

        // Validation
        if (n <= 0) {
            System.out.println("Number of matrices must be greater than 0.");
            scanner.close();
            return;
        }

        int[] dimensions = new int[n + 1];

        System.out.println(
                "\nEnter dimensions for the matrices:"
        );

        System.out.println(
                "Example: For A1 = 10x20, A2 = 20x30, enter dimensions accordingly."
        );

        for (int i = 0; i <= n; i++) {

            while (true) {

                System.out.print("Dimension " + (i + 1) + ": ");
                dimensions[i] = scanner.nextInt();

                if (dimensions[i] > 0) {
                    break;
                }

                System.out.println(
                        "Dimension must be a positive integer."
                );
            }
        }

        System.out.println("\n----------------------------------------------");

        // Display matrices
        System.out.println("\nMatrices:");

        for (int i = 1; i <= n; i++) {

            System.out.println(
                    "A" + i +
                    " = " +
                    dimensions[i - 1] +
                    " x " +
                    dimensions[i]
            );
        }

        // Dynamic Programming
        long startTime = System.nanoTime();

        long optimalCost = matrixChainDP(dimensions, n);

        long endTime = System.nanoTime();

        double dpTime = (endTime - startTime) / 1_000_000.0;

        // Results
        System.out.println("\n==============================================");
        System.out.println("              OPTIMIZATION RESULT");
        System.out.println("==============================================");

        System.out.println(
                "\nMinimum Scalar Multiplications: "
                + optimalCost
        );

        System.out.print(
                "Optimal Parenthesization: "
        );

        printParenthesization(1, n);

        System.out.println();

        System.out.printf(
                "\nDP Execution Time: %.4f ms%n",
                dpTime
        );

        // Display tables
        if (n <= 10) {
            displayCostTable(n);
            displaySplitTable(n);
        } else {
            System.out.println(
                    "\nTables hidden because the number of matrices is large."
            );
        }

        // Naive comparison only for small input
        if (n <= 10) {

            System.out.println(
                    "\n----------------------------------------------"
            );

            long recursiveStart = System.nanoTime();

            long recursiveCost =
                    matrixChainRecursive(dimensions, 1, n);

            long recursiveEnd = System.nanoTime();

            double recursiveTime =
                    (recursiveEnd - recursiveStart) / 1_000_000.0;

            System.out.println(
                    "\nNaïve Recursive Cost: "
                    + recursiveCost
            );

            System.out.printf(
                    "Naïve Recursive Time: %.4f ms%n",
                    recursiveTime
            );

            System.out.println(
                    "\nCost Verification: "
                    + (recursiveCost == optimalCost ? "PASSED" : "FAILED")
            );
        }

        System.out.println(
                "\n=============================================="
        );

        System.out.println("Complexity:");
        System.out.println("Dynamic Programming Time  : O(n^3)");
        System.out.println("Dynamic Programming Space : O(n^2)");
        System.out.println("Naïve Recursive Time      : O(2^n)");

        System.out.println(
                "=============================================="
        );

        scanner.close();
    }
}