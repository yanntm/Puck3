package fr.lip6.puck.dsl.serialization;

import java.util.logging.Logger;

import org.eclipse.core.resources.IResource;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.eclipse.xtext.resource.IResourceServiceProvider;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.resource.XtextResourceSet;
import org.eclipse.xtext.ui.resource.IResourceSetProvider;
import com.google.inject.Guice;
import com.google.inject.Injector;

import fr.lip6.puck.dsl.PuckRuntimeModule;
import fr.lip6.puck.dsl.PuckStandaloneSetup;
import fr.lip6.puck.dsl.puck.PuckFactory;
import fr.lip6.puck.dsl.puck.PuckModel;

public class SerializationUtil {
	private static Logger getLog() { return Logger.getLogger("fr.lip6.puck"); }
	
	public static PuckModel fileToPuckModel(String filename)
	{
		if(! filename.endsWith(".wld"))
		{
			getLog().warning("Warning: filename '" + filename + "' should end with .wld extension ");
		}
		
		Resource res = loadResources(filename); 
		PuckModel system = (PuckModel) res.getContents().get(0);
		
		return system ;
	}

	public static String toText (EObject model) {
		return ((XtextResource)model.eResource()).getSerializer().serialize(model);
	}
	
	public static PuckModel resourceToPuckModel(IResource file)
	{
		Injector inj = createInjector(); 
		org.eclipse.emf.common.util.URI uri = org.eclipse.emf.common.util.URI
			    .createPlatformResourceURI(file.getFullPath().toString(), true);
		
		IResourceSetProvider rs1 = IResourceServiceProvider.Registry.INSTANCE.getResourceServiceProvider(uri)
		        .get(IResourceSetProvider.class);
		ResourceSet resourceSet = rs1.get(file.getProject());
		//resourceSet.addLoadOption(XtextResource.OPTION_RESOLVE_ALL, Boolean.TRUE);
		
		Resource resource = resourceSet.getResource(uri, true);
		if (! resource.getContents().isEmpty()) {
			PuckModel system = (PuckModel) resource.getContents().get(0);			
			return system ;
		} else {
			return PuckFactory.eINSTANCE.createPuckModel();
		}
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
