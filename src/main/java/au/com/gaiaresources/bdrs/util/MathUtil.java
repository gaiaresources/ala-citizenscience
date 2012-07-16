package au.com.gaiaresources.bdrs.util;

public class MathUtil {

	/**
	 * Truncates a double to a number of decimal places.
	 * 
	 * @param x number to truncate.
	 * @param decimalPlaces number of decimal places to truncate to.
	 * @return double result.
	 */
	public static double truncate(double x, int decimalPlaces) {
		double truncateFactor = Math.pow(10, (double)decimalPlaces);
		return x > 0 ? (Math.floor(x * truncateFactor)) / truncateFactor : (Math.ceil(x * truncateFactor)) / truncateFactor;
	}
}
