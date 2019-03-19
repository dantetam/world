package io.github.dantetam.world.ai;

import java.util.List;

/**
 * Created by Dante on 6/23/2016.
 */
public interface Traversable<T> {

    double dist(T t);
    List<T> validNeighbors(Object[] data);
    boolean equals(Object t);

}
