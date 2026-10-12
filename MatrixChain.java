
import java.util.Arrays;

public class MatrixChain {
    private final int[] dimensions;
    private final int n;
    private final long[][] cost;
    private final int[][] split;

    public MatrixChain(int[] dimensions) {
        if (dimensions == null || dimensions.length < 2) {
            throw new IllegalArgumentException(
                "Enter at least two dimensions."
            );
        }

        for (int d : dimensions) {
            if (d <= 0) {
                throw new IllegalArgumentException(
                    "Dimensions must be positive integers."
                );
            }
        }

        this.dimensions = Arrays.copyOf(dimensions, dimensions.length);
        this.n = dimensions.length - 1;

        if (n > 500) {
            throw new IllegalArgumentException(
                "Maximum 500 matrices are supported."
            );
        }

        cost = new long[n][n];
        split = new int[n][n];

        calculateOptimalCosts();
    }

    private void calculateOptimalCosts() {
        for (int length = 2; length <= n; length++) {
            for (int i = 0; i <= n - length; i++) {
                int j = i + length - 1;
                cost[i][j] = Long.MAX_VALUE;

                for (int k = i; k < j; k++) {
                    long multiplicationCost =
                        (long) dimensions[i]
                        * dimensions[k + 1]
                        * dimensions[j + 1];

                    long candidate =
                        cost[i][k]
                        + cost[k + 1][j]
                        + multiplicationCost;

                    if (candidate < cost[i][j]) {
                        cost[i][j] = candidate;
                        split[i][j] = k;
                    }
                }
            }
        }
    }

    public long getMinimumCost() {
        return cost[0][n - 1];
    }

    public long getLeftToRightCost() {
        long rows = dimensions[0];
        long columns = dimensions[1];
        long total = 0;

        for (int i = 2; i <= n; i++) {
            long nextColumns = dimensions[i];
            total += rows * columns * nextColumns;
            columns = nextColumns;
        }

        return total;
    }

    public String getOptimalParenthesization() {
        StringBuilder result = new StringBuilder();
        buildParenthesization(0, n - 1, result);
        return result.toString();
    }

    private void buildParenthesization(
        int i, int j, StringBuilder result
    ) {
        if (i == j) {
            result.append("A").append(i + 1);
            return;
        }

        result.append("(");
        buildParenthesization(i, split[i][j], result);
        result.append(" x ");
        buildParenthesization(split[i][j] + 1, j, result);
        result.append(")");
    }

    public int getMatrixCount() {
        return n;
    }

    // Returns the DP cost table for the website.
    public long[][] getCostTable() {
        long[][] result = new long[n][n];

        for (int i = 0; i < n; i++) {
            Arrays.fill(result[i], -1);

            for (int j = i; j < n; j++) {
                result[i][j] = cost[i][j];
            }
        }

        return result;
    }

    // Returns the split table using 1-based matrix positions.
    public int[][] getSplitTable() {
        int[][] result = new int[n][n];

        for (int i = 0; i < n; i++) {
            Arrays.fill(result[i], -1);

            for (int j = i + 1; j < n; j++) {
                result[i][j] = split[i][j] + 1;
            }
        }

        return result;
    }

    public void printTables() {
        System.out.println("\nMinimum Cost Table:");

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (j < i) {
                    System.out.printf("%10s", "-");
                } else {
                    System.out.printf("%10d", cost[i][j]);
                }
            }
            System.out.println();
        }

        System.out.println("\nSplit Table:");

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (j <= i) {
                    System.out.printf("%8s", "-");
                } else {
                    System.out.printf("%8d", split[i][j] + 1);
                }
            }
            System.out.println();
        }
    }
}
