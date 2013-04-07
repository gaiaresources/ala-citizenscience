package au.com.gaiaresources.bdrs.db;

import au.com.gaiaresources.bdrs.model.record.Record;

import java.util.Comparator;

/**
 * Compares two Persistent objects on their weight.
 */
public class WeightComparator implements Comparator<Record> {

    /**
     * Implements a default ordering based on weight.
     * @param first the first Persistent object to compare.
     * @param second the second Persistent object to compare.
     * @return -1 if the first object has a lower weight than the other, 0 if the are the same, 1 if the first object has a
     * higher weight.
     */
    public int compare(Record first, Record second) {

        if (first == null && second == null) {
            return 0;
        }
        else if (first == null) {
            return -1;
        }
        else if (second == null) {
            return 1;
        }
        int firstWeight = first.getWeight() != null ? first.getWeight() : Integer.MIN_VALUE;
        int secondWeight = second.getWeight() != null ? second.getWeight() : Integer.MIN_VALUE;

        return (firstWeight < secondWeight) ? -1 : ((firstWeight == secondWeight) ? 0 : 1);
    }
}
