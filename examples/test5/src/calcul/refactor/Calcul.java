package calcul.refactor;

import calcul.refactor.matrix.IMatrice;
import calcul.refactor.matrix.MatriceFactory;

public class Calcul {
	// sous ce seuil la matrice devrait etre creuse
	private static final double seuilCreuse = 0.2; 
	
	private boolean creuse;
	private IMatrice mat;

	public Calcul(int hauteur, int largeur) {
		creuse = true;
		mat = MatriceFactory.createMatriceCreuse(hauteur, largeur);
	}

	public void set(int x, int y, int valeur) {
		mat.set(x, y, valeur);
	}

	public int get(int x, int y) {
		return mat.get(x, y);
	}

	public boolean estCreuse() {
		return creuse;
	}

	void optimiser() {
		if (creuse && mat.getTauxRemplissage() > seuilCreuse) {
			creuse = false;
			mat = MatriceFactory.createMatricePleine(mat);
		} else if (!creuse && mat.getTauxRemplissage() <= seuilCreuse) {
			creuse = true;
			mat = MatriceFactory.createMatriceCreuse(mat);
		}
	}
}
