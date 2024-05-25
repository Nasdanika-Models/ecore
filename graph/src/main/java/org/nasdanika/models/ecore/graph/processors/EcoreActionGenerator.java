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
import org.nasdanika.models.ecore.graph.EcoreGraphFactory;

public class EcoreActionGenerator extends ActionGenerator {

	public EcoreActionGenerator(
			Collection<? extends EObject> sources,
			Collection<? extends EObject> references,
			Function<? super EObject, URI> uriResolver,
			EcoreNodeProcessorFactory nodeProcessorFactory) {
		super(sources, references, uriResolver, nodeProcessorFactory);
	}
	
	public EcoreActionGenerator(
			EObject source,
			URI baseURI,
			Map<EPackage, URI> references,
			EcoreNodeProcessorFactory nodeProcessorFactory) {
		super(
			Collections.singleton(source),
			references.keySet(), 
			eObj -> {
				if (eObj == source) {
					return baseURI;
				}
				return references.get(eObj);
			},
			nodeProcessorFactory);
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
	
	@Override
	protected Object createGraphFactory() {
		return new EcoreGraphFactory();
	}
	
}
