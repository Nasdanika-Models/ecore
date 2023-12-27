package org.nasdanika.models.ecore.graph.processors;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.nasdanika.html.model.app.graph.emf.ActionGenerator;

public class EcoreActionGenerator extends ActionGenerator<EcoreNodeProcessorFactory> {

	public EcoreActionGenerator(
			Collection<? extends EObject> sources,
			EcoreNodeProcessorFactory nodeProcessorFactory, 
			Collection<? extends EObject> references,
			Function<? super EObject, URI> uriResolver) {
		super(sources, nodeProcessorFactory, references, uriResolver);
	}
	
	public EcoreActionGenerator(
			EObject source,
			URI baseURI,
			Map<EPackage, URI> references,
			EcoreNodeProcessorFactory nodeProcessorFactory) {
		super(
			Collections.singleton(source),
			nodeProcessorFactory, 
			references.keySet(), 
			eObj -> {
				if (eObj == source) {
					return baseURI;
				}
				return references.get(eObj);
			});
	}
		
	public EcoreActionGenerator(
			EObject source, 
			Map<EPackage, URI> references,
			EcoreNodeProcessorFactory nodeProcessorFactory) {
		this(source,
				URI.createURI("tmp://" + UUID.randomUUID() + "/" + UUID.randomUUID() + "/"),
				references,
				nodeProcessorFactory);
	}
	
}
