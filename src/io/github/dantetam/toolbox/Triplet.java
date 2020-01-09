package io.github.dantetam.toolbox;

public class Triplet<T> {

	public T first, second, third;
	
	public Triplet(T first, T second, T third) {
		this.first = first;
		this.second = second;
		this.third = third;
	}
	
	public String toString() {
		return "Triplet: [" + first.toString() + ", " + second.toString() + ", " + third.toString() + "]";
	}
	
}
