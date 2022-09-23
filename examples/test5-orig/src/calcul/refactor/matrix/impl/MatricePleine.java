package calcul.refactor.matrix.impl;

import java.util.Arrays;

import calcul.refactor.matrix.IMatrice;

public class MatricePleine implements IMatrice {
	private int[][] tab;

	public MatricePleine(int hauteur, int largeur) {
		tab = new int[hauteur][largeur];
		for (int i = 0; i < hauteur; ++i)
			Arrays.fill(tab[i], 0);
	}

	@Override
	public int getHauteur() {
		return tab.length;
	}

	@Override
	public int getLargeur() {
		return tab[0].length;
	}

	@Override
	public void set(int x, int y, int valeur) {
		tab[x - 1][y - 1] = valeur;
	}

	@Override
	public int get(int x, int y) {
		return tab[x - 1][y - 1];
	}

	@Override
	public double getTauxRemplissage() {
		return getNbNonNuls() / (double) (getLargeur() * getHauteur());
	}

	private int getNbNonNuls() {
		int nonNuls = 0;
		for (int x = 1; x <= getHauteur(); ++x)
			for (int y = 1; y <= getLargeur(); ++y)
				if (get(x, y) != 0)
					++nonNuls;
		return nonNuls;
	}
}
