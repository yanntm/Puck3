package calcul.refactor;

import static org.junit.Assert.*;
import org.junit.Test;

import calcul.refactor.matrix.IMatrice;
import calcul.refactor.matrix.impl.MatriceCreuse;
import calcul.refactor.matrix.impl.MatricePleine;

public class MatricesTest {
	@Test
	public void testerMatricePleine() {
		MatricePleine m = new MatricePleine(2, 3);
		assertEquals(2, m.getHauteur());
		assertEquals(3, m.getLargeur());

		for (int x = 1; x < m.getHauteur(); ++x)
			for (int y = 1; y < m.getLargeur(); ++y)
				assertEquals(m.get(x, y), 0);

		m.set(1, 1, 5);
		assertEquals(m.get(1, 1), 5);
		m.set(1, 1, 7);
		assertEquals(m.get(1, 1), 7);
	}

	@Test
	public void testerMatriceCreuse() {
		IMatrice m = new MatriceCreuse(2, 3);
		assertEquals(2, m.getHauteur());
		assertEquals(3, m.getLargeur());

		for (int x = 1; x < m.getHauteur(); ++x)
			for (int y = 1; y < m.getLargeur(); ++y)
				assertEquals(m.get(x, y), 0);

		m.set(1, 1, 5);
		assertEquals(m.get(1, 1), 5);
		m.set(1, 1, 7);
		assertEquals(m.get(1, 1), 7);
	}

	@Test
	public void testerTaux() {
		IMatrice m = new MatriceCreuse(2, 3);
		assertEquals(m.getTauxRemplissage(), 0.0, .001);
		m.set(1, 1, 5);
		m.set(1, 2, 5);
		m.set(1, 3, 5);
		assertEquals(m.getTauxRemplissage(), 0.5, .001);
		m.set(1, 3, 0);
		assertEquals(m.getTauxRemplissage(), 0.333, .001);
	}
}