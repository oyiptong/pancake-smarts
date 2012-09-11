package models;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Map;

import com.google.common.hash.Hashing;
import com.google.common.hash.HashFunction;
/**
 * Created with IntelliJ IDEA.
 * User: oyiptong
 * Date: 2012-09-07
 * Time: 2:12 PM
 */
public class RandomProjection {

    final static int HASH_BYTES = 4;
    final static double MAX_HASH_VALUE = Math.pow(2, 8*HASH_BYTES);

    public static double[] project(Map<Integer, Double> input, int dimensions)
    {
        double[] projection = new double[dimensions];

        Arrays.fill(projection, 0.0);

        for(Map.Entry<Integer, Double> pair : input.entrySet())
        {
            int key = pair.getKey().intValue();
            double value = pair.getValue();

            double[] randomRow = randomRow(key, dimensions, 0);

            // multiply by scalar
            for(int i = 0; i < randomRow.length; i++)
            {
                randomRow[i] = randomRow[i] * value;
            }

            // add to projection
            for(int i = 0; i < projection.length; i++)
            {
                projection[i] += randomRow[i];
            }
        }

        return projection;
    }

    public static double[] project(double[] input, int dimensions)
    {
        double[] projection = new double[dimensions];

        Arrays.fill(projection, 0.0);

        for(int index=0; index < input.length; index++)
        {
            double value = input[index];
            double[] randomRow = randomRow(index, dimensions, 0);

            // multiply by scalar
            for(int i = 0; i < randomRow.length; i++)
            {
                randomRow[i] = randomRow[i] * value;
            }

            // add to projection
            for(int i = 0; i < projection.length; i++)
            {
                projection[i] += randomRow[i];
            }
        }

        return projection;
    }

    public static BitSet projectBinaryBytes(double[] input, int dimensions)
    {
        double[] projection = project(input, dimensions);

        BitSet bs = new BitSet(dimensions);
        for(int i = 0; i < projection.length; i++)
        {
            if(projection[i] > 0)
            {
                bs.set(i, true);
            }
        }

        return bs;
    }

    public static double[] randomRow(int key, int dimensions, int randomSeed) {
        double[] values = new double[dimensions];

        for(int i=0; i < dimensions; i++)
        {
            String matrixEntryName = String.format("%d-%d-%d", key, i, randomSeed);
            double randomValue = deterministicRandom(matrixEntryName);
            double gaussianValue = gaussianFromProbability(randomValue);
            values[i] = gaussianValue;
        }
        return values;
    }

    public static double deterministicRandom(String value)
    {
        // obtain a hash function that will generate a 32-bit long hash code
        HashFunction hf = Hashing.goodFastHash(32);

        long i = Hashing.padToLong(hf.hashString(value));

        return 1.0 * i / MAX_HASH_VALUE;
    }

    public static double gaussianFromProbability(double value)
    {
        double value2 = deterministicRandom(String.format("%1.10f-x2", value));
        return gaussianFromTwoProbabilities(value, value2);
    }

    public static double gaussianFromTwoProbabilities(double value1, double value2)
    {
        double theta = value1 * 2 * Math.PI;
        double rsq = (-1.0 / 0.5) * Math.log(value2);
        double y = Math.sqrt(rsq) * Math.cos(theta);
        return y;
    }
}
