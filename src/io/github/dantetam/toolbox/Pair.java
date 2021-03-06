package io.github.dantetam.toolbox;

public class Pair<T> {

	public T first, second;
	
	public Pair(T first, T second) {
		this.first = first;
		this.second = second;
	}
	
	public String toString() {
		return "Pair: [" + first.toString() + ", " + second.toString() + "]";
	}
	
}
