package calcul;

import java.util.HashMap;
import java.util.Map;

public class MatriceCreuse {
	private Map<XY, Integer> elements;
	private int hauteur ;
	private int largeur;

	public MatriceCreuse(int hauteur, int largeur) {
		this.hauteur = hauteur;
		this.largeur = largeur;
		elements = new HashMap<>();
	}

	public int getLargeur() {
		return largeur;
	}

	public int getHauteur() {
		return hauteur;
	}

	public void set(int x, int y, int value) {
		if (x < 1 || x > getHauteur() || y < 1 || y > getLargeur())
			throw new IndexOutOfBoundsException();
		XY xy = new XY(x, y);
		if (value != 0)
			elements.put(xy, value);
		else
			elements.remove(xy);
	}

	public int get(int x, int y) {
		if (x < 1 || x > getHauteur() || y < 1 || y > getLargeur())
			throw new IndexOutOfBoundsException();
		XY xy = new XY(x, y);
		if (!elements.containsKey(xy))
			return 0;
		else
			return elements.get(xy).intValue();
	}

	public MatricePleine versPleine() {
		MatricePleine mp = new MatricePleine(hauteur, largeur);
		for (int x = 1; x <= hauteur; ++x)
			for (int y = 1; y <= largeur; ++y)
				mp.set(x, y, get(x, y));
		return mp;
	}

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
