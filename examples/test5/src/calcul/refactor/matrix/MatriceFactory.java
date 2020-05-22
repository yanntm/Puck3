package calcul.refactor.matrix;

import calcul.refactor.matrix.impl.MatriceCreuse;
import calcul.refactor.matrix.impl.MatricePleine;

public class MatriceFactory {

	public static IMatrice createMatriceCreuse(int hauteur, int largeur) {
		return new MatriceCreuse(hauteur, largeur);
	}

	public static IMatrice createMatricePleine(int hauteur, int largeur) {
		return new MatricePleine(hauteur, largeur);
	}

	
	public static IMatrice createMatriceCreuse(IMatrice mat) {
		IMatrice ret = createMatriceCreuse(mat.getHauteur(), mat.getLargeur());
		copyTo(ret, mat);		
		return ret;
	}

	public static IMatrice createMatricePleine(IMatrice mat) {
		IMatrice ret = createMatricePleine(mat.getHauteur(), mat.getLargeur());
		copyTo(ret, mat);		
		return ret;
	}

	
	private static void copyTo(IMatrice target, IMatrice source) {
		for (int x = 1, hauteur=source.getHauteur(); x <= hauteur; ++x)
			for (int y = 1, largeur=source.getLargeur(); y <= largeur; ++y)
				target.set(x, y, source.get(x, y));
	}

	
}
