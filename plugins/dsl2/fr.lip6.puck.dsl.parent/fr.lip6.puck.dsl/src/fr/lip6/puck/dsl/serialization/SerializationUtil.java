package fr.lip6.puck.dsl.serialization;

import java.util.logging.Logger;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.resource.XtextResourceSet;

import com.google.inject.Guice;
import com.google.inject.Injector;

import fr.lip6.puck.dsl.PuckRuntimeModule;
import fr.lip6.puck.dsl.PuckStandaloneSetup;
import fr.lip6.puck.dsl.puck.PuckModel;

public class SerializationUtil {
	private static Logger getLog() { return Logger.getLogger("fr.lip6.puck"); }
	
	public static PuckModel fileToPuckModel(String filename)
	{
		if(! filename.endsWith(".gal"))
		{
			getLog().warning("Warning: filename '" + filename + "' should end with .gal extension ");
		}
		
		Resource res = loadResources(filename); 
		PuckModel system = (PuckModel) res.getContents().get(0);
		
		return system ;
	}

	
	
	private static Injector createInjector() {
		boolean isStandalone = false;
		if (isStandalone) {
			PuckStandaloneSetup gs = new PuckStandaloneSetup();
			return gs.createInjectorAndDoEMFRegistration();
		} else {
			return Guice.createInjector(new PuckRuntimeModule());
		}
	}

	
	private static Resource loadResources(String filename) 
	{
		
		Injector inj = createInjector(); 
		
		XtextResourceSet resourceSet = inj.getInstance(XtextResourceSet.class); 
		resourceSet.addLoadOption(XtextResource.OPTION_RESOLVE_ALL, Boolean.TRUE);
		URI uri = URI.createFileURI(filename) ; 
		Resource resource = resourceSet.getResource(uri, true);
		return resource ; 
	}

}
