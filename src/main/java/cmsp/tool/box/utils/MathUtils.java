
package cmsp.tool.box.utils;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Utility class for simple summary statistics.
 */
public class MathUtils {

    /**
     * Calculate standard deviation of input array.
     *
     * @param array ArrayList Double of numerical values
     * @return Double standard deviation number
     */
    public static Double calculateStandardDeviation(ArrayList<Double> array) {

        // Get the sum of array.
        double sum = 0.0;
        for (double i : array) sum += i;

        // Get the mean of array.
        int length = array.size();
        double mean = sum / length;

        // Calculate the standard deviation.
        double standardDeviation = 0.0;
        for (double num : array) standardDeviation += Math.pow(num - mean, 2);

        return Math.sqrt(standardDeviation / length);
    }

    /**
     * Calculate arithmetic mean of input Double ArrayList.
     *
     * @param array ArrayList Double of numerical values
     * @return Double arithmetic mean number
     */
    public static Double calculateAverage(ArrayList<Double> array) {

        return array.stream()
                .mapToDouble(d -> d)
                .average()
                .orElse(0.0);
    }

    /**
     * Calculate median of input Double ArrayList.
     *
     * @param array ArrayList Double of numerical values
     * @return Double median number.
     */
    public static Double calculateMedian(ArrayList<Double> array) {

        Collections.sort(array);
        int middle = array.size() / 2;
        middle = middle > 0 && middle % 2 == 0 ? middle - 1 : middle;

        if(middle != 0){
            return array.get(middle);
        } else {
            return 0.0;
        }
    }

    /**
     * Calculate relative standard deviation as percent of Double ArrayList.
     *
     * @param array ArrayList Double of numerical values
     * @return Double relative standard deviation (as percent) value.
     */
    public static Double calculateRelativeStandardDeviation(ArrayList<Double> array) {

        return calculateStandardDeviation(array) / calculateAverage(array) * 100;
    }

    /**
     * Log2 transform Double value.
     *
     * @param N Double value
     * @return Log2 transformed Double
     */
    public static Double log2(Double N) {

        return (Math.log(N) / Math.log(2));
    }

    /**
     * Test if string is integer
     *
     * @param str String input
     * @return Boolean true if string is integer.
     */
    public static boolean isInteger(String str) {

        if (str == null || str.isEmpty()) {
            return false;
        }

        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
