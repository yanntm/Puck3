package calcul;

public class Calcul {
	// sous ce seuil la matrice devrait etre creuse
	private static final double seuilCreuse = 0.2;

	private boolean creuse;
	private MatriceCreuse mc;
	private MatricePleine mp;

	public Calcul(int hauteur, int largeur) {
		creuse = true;
		mc = new MatriceCreuse(hauteur, largeur);
		mp = null;
	}

	public void set(int x, int y, int valeur) {
		if (creuse)
			mc.set(x, y, valeur);
		else
			mp.set(x, y, valeur);
	}

	public int get(int x, int y) {
		if (creuse)
			return mc.get(x, y);
		else
			return mp.get(x, y);
	}

	public boolean estCreuse() {
		return creuse;
	}

	void optimiser() {
		if (creuse && mc.getTauxRemplissage() > seuilCreuse) {
			creuse = false;
			mp = mc.versPleine();
			mc = null;
		} else if (!creuse && mp.getTauxRemplissage() <= seuilCreuse) {
			creuse = true;
			mc = mp.versCreuse();
			mp = null;
		}
	}
}
