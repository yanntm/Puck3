package calcul.refactor.matrix.impl;

import java.util.HashMap;
import java.util.Map;

import calcul.refactor.matrix.IMatrice;

public class MatriceCreuse implements IMatrice {
	private Map<XY, Integer> elements;
	private int hauteur ;
	private int largeur;

	public MatriceCreuse(int hauteur, int largeur) {
		this.hauteur = hauteur;
		this.largeur = largeur;
		elements = new HashMap<>();
	}

	@Override
	public int getLargeur() {
		return largeur;
	}

	@Override
	public int getHauteur() {
		return hauteur;
	}

	@Override
	public void set(int x, int y, int value) {
		if (x < 1 || x > getHauteur() || y < 1 || y > getLargeur())
			throw new IndexOutOfBoundsException();
		XY xy = new XY(x, y);
		if (value != 0)
			elements.put(xy, value);
		else
			elements.remove(xy);
	}

	@Override
	public int get(int x, int y) {
		if (x < 1 || x > getHauteur() || y < 1 || y > getLargeur())
			throw new IndexOutOfBoundsException();
		XY xy = new XY(x, y);
		if (!elements.containsKey(xy))
			return 0;
		else
			return elements.get(xy).intValue();
	}

	@Override
	public double getTauxRemplissage() {
		return getNbNonNuls() / (double) (getLargeur() * getHauteur());
	}

	private int getNbNonNuls() {
		return elements.size();
	}

	private static class XY {
		public XY(int x, int y) {
			this.x = x;
			this.y = y;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || !(o instanceof XY))
				return false;
			else {
				XY xy = (XY) o;
				return (x == xy.x && y == xy.y);
			}
		}

		@Override
		public int hashCode() { // n�cessaire pour �tre dans une HashMap
			return (x * 101) ^ y; // par exemple
		}

		private int x;
		private int y;
	}
}
