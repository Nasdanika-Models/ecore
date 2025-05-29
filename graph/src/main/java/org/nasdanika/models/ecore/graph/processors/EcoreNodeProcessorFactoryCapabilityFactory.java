package org.nasdanika.models.ecore.graph.processors;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.CompletionStage;

import org.nasdanika.capability.CapabilityFactory;
import org.nasdanika.capability.CapabilityProvider;
import org.nasdanika.common.ProgressMonitor;
import org.nasdanika.models.app.graph.emf.HtmlAppGenerator.NodeProcessorFactoryRequirement;

import reactor.core.publisher.Flux;

public class EcoreNodeProcessorFactoryCapabilityFactory implements CapabilityFactory<NodeProcessorFactoryRequirement, Object> {
	
	/**
	 * Requirement for a {@link EcoreNodeProcessorFactoryCapabilityFactory} target
	 * @param <R>
	 * @param <P>
	 */
	public static record TargetRequirement(NodeProcessorFactoryRequirement requirement) {}		

	@Override
	public boolean canHandle(Object requirement) {
		return requirement instanceof NodeProcessorFactoryRequirement;
	}

	@Override
	public CompletionStage<Iterable<CapabilityProvider<Object>>> create(
			NodeProcessorFactoryRequirement requirement,
			Loader loader,
			ProgressMonitor progressMonitor) {
		
		TargetRequirement targetRequirement = new TargetRequirement(requirement);		
		CompletionStage<Iterable<CapabilityProvider<Object>>> targetsCS = loader.load(targetRequirement, progressMonitor);
		return targetsCS.thenApply(targets -> createFactory(requirement, loader, targets, progressMonitor));
	}
	
	protected Iterable<CapabilityProvider<Object>> createFactory(
			NodeProcessorFactoryRequirement requirement,
			Loader loader,
			Iterable<CapabilityProvider<Object>> targetCapabilityProviders, 
			ProgressMonitor progressMonitor) {
				
		Collection<Object> targets = new ArrayList<>();
		
		for (CapabilityProvider<Object> tcp: targetCapabilityProviders) {
			tcp.getPublisher().filter(Objects::nonNull).collectList().block().forEach(targets::add);
		}
		
		if (!targets.isEmpty()) {
			EcoreNodeProcessorFactory factory = new EcoreNodeProcessorFactory(
				requirement.context(), 
				requirement.prototypeProvider(),
				requirement.diagnosticConsumer(),
				targets.toArray()
			);
			
			if (requirement.factoryPredicate() == null || requirement.factoryPredicate().test(factory)) {
				CapabilityProvider<Object> capabilityProvider = new CapabilityProvider<Object>() {
					
					@Override
					public Flux<Object> getPublisher() {
						return Flux.just(factory);
					}
					
				};			
		
				return Collections.singleton(capabilityProvider);
			}
		}
		
		return Collections.emptyList();
	}
	
	protected boolean isParallel() {
		return false;
	}
	

}
