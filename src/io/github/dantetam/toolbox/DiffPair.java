package io.github.dantetam.toolbox;

public class DiffPair<T, U> {

	public T first;
	public U second;
	
	public DiffPair(T first, U second) {
		this.first = first;
		this.second = second;
	}
	
	public String toString() {
		return "Pair: [" + first.toString() + ", " + second.toString() + "]";
	}
	
}
