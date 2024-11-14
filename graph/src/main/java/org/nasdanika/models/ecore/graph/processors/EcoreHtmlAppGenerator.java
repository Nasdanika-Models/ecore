package org.nasdanika.models.ecore.graph.processors;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.nasdanika.capability.CapabilityLoader;
import org.nasdanika.common.Context;
import org.nasdanika.common.Diagnostic;
import org.nasdanika.common.ProgressMonitor;
import org.nasdanika.models.app.Label;
import org.nasdanika.models.app.graph.emf.HtmlAppGenerator;
import org.nasdanika.models.ecore.graph.EcoreGraphFactory;

public class EcoreHtmlAppGenerator extends HtmlAppGenerator {

	public EcoreHtmlAppGenerator(
			Collection<? extends EObject> sources,
			Collection<? extends EObject> references,
			Function<? super EObject, URI> uriResolver,
			EcoreNodeProcessorFactory nodeProcessorFactory) {
		super(sources, references, uriResolver, nodeProcessorFactory);
	}

	private EcoreHtmlAppGenerator(
			Collection<? extends EObject> sources,
			Collection<? extends EObject> references,
			Function<? super EObject, URI> uriResolver,
			Object[] nodeProcessorFactories) {
		super(sources, references, uriResolver, nodeProcessorFactories);
	}
	
	public EcoreHtmlAppGenerator(
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
		
	public EcoreHtmlAppGenerator(
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
	
	/**
	 * Loads mapping of {@link EPackage}s to doc URI's and node processor factories from {@link CapabilityLoader}. 
	 * @param sources
	 * @param references
	 * @param capabilityLoader
	 * @param progressMonitor
	 * @return
	 */
	public static EcoreHtmlAppGenerator loadEcoreHtmlAppGenerator(
			Collection<EObject> sources,
			Context context, 
			java.util.function.BiFunction<URI, ProgressMonitor, Label> prototypeProvider,			
			Predicate<Object> factoryPredicate,
			Predicate<EPackage> ePackagePredicate,
			Consumer<Diagnostic> diagnosticConsumer,
			ProgressMonitor progressMonitor) {

		return loadEcoreHtmlAppGenerator(
				sources,
				context, 
				prototypeProvider,
				factoryPredicate,
				ePackagePredicate,
				new CapabilityLoader(),
				diagnosticConsumer,
				progressMonitor);
	}	
	
	/**
	 * Loads mapping of {@link EPackage}s to doc URI's and node processor factories from {@link CapabilityLoader}. 
	 * @param sources
	 * @param references
	 * @param capabilityLoader
	 * @param progressMonitor
	 * @return
	 */
	public static EcoreHtmlAppGenerator loadEcoreHtmlAppGenerator(
			Collection<EObject> sources,
			Context context, 
			java.util.function.BiFunction<URI, ProgressMonitor, Label> prototypeProvider,			
			Predicate<Object> factoryPredicate,
			Predicate<EPackage> ePackagePredicate,
			CapabilityLoader capabilityLoader, 
			Consumer<Diagnostic> diagnosticConsumer,
			ProgressMonitor progressMonitor) {

		return load(
				sources,
				context, 
				prototypeProvider,			
				factoryPredicate,
				ePackagePredicate,
				capabilityLoader, 
				diagnosticConsumer,
				EcoreHtmlAppGenerator::new,
				progressMonitor);			
		
	}
	
	
}
