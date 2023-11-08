package org.nasdanika.models.ecore.graph.processors;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Predicate;
import java.util.TreeMap;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.ETypeParameter;
import org.eclipse.emf.ecore.EcorePackage;
import org.nasdanika.common.Context;
import org.nasdanika.common.ProgressMonitor;
import org.nasdanika.graph.emf.EReferenceConnection;
import org.nasdanika.graph.processor.NodeProcessorConfig;
import org.nasdanika.graph.processor.OutgoingEndpoint;
import org.nasdanika.html.model.app.Action;
import org.nasdanika.html.model.app.AppFactory;
import org.nasdanika.html.model.app.Label;
import org.nasdanika.html.model.app.gen.DynamicTableBuilder;
import org.nasdanika.html.model.app.graph.WidgetFactory;
import org.nasdanika.html.model.app.graph.emf.OutgoingReferenceBuilder;

// <name> extends <bound 1> [& <bound 2> ...]
public class ETypeParameterNodeProcessor extends EModelElementNodeProcessor<ETypeParameter> implements EClassifierNodeProcessorProvider {

	public ETypeParameterNodeProcessor(
			NodeProcessorConfig<WidgetFactory, WidgetFactory> config,
			Context context,
			java.util.function.Function<ProgressMonitor, Action> prototypeProvider) {
		super(config, context, prototypeProvider);
	}	
	
	@Override
	protected boolean isCallOutgoingReferenceLabelsSuppliers(EReference eReference) {
		if (eReference == EcorePackage.Literals.ETYPE_PARAMETER__EBOUNDS) {
			return true;
		}
		return super.isCallOutgoingReferenceLabelsSuppliers(eReference);
	}	
	
	private Map<Integer,WidgetFactory> eBoundsWidgetFactories = Collections.synchronizedMap(new TreeMap<>());
	
	@OutgoingEndpoint("reference.name == 'eBounds'")
	public final void setETypeParameterEndpoint(EReferenceConnection connection, WidgetFactory eBoundWidgetFactory) {
		eBoundsWidgetFactories.put(connection.getIndex(), eBoundWidgetFactory);
	}	
	
	/**
	 * Returns attributes action, creates if necessary. Matches by location.
	 * @param parent
	 * @return
	 */
	protected Action getBoundsAction(Action parent) {
		Action pAction = (Action) parent;
		return pAction.getNavigation()
			.stream()
			.filter(e -> e instanceof Action && "bounds.html".equals(((Action) e).getLocation()))
			.findFirst()
			.map(Action.class::cast)
			.orElseGet(() -> {
				Action typeParametersAction = AppFactory.eINSTANCE.createAction();
				typeParametersAction.setText("Bounds");
				typeParametersAction.setIcon("https://cdn.jsdelivr.net/gh/Nasdanika-Models/ecore@master/graph/web-resources/icons/EGenericType.gif");
				typeParametersAction.setLocation("bounds.html");
				pAction.getNavigation().add(typeParametersAction);
				return typeParametersAction;
			});
	}
	
	@OutgoingReferenceBuilder(EcorePackage.ETYPE_PARAMETER__EBOUNDS)
	public void buildETypeParametersOutgoingReference(
			EReference eReference,
			List<Entry<EReferenceConnection, WidgetFactory>> referenceOutgoingEndpoints, 
			Collection<Label> labels,
			Map<EReferenceConnection, Collection<Label>> outgoingLabels, 
			ProgressMonitor progressMonitor) {
		
		// Own parameters 
		for (Label tLabel: labels) {
			if (tLabel instanceof Action) {
				Action parametersAction = getBoundsAction((Action) tLabel);
				EList<Action> tAnonymous = parametersAction.getAnonymous();
				for (Entry<EReferenceConnection, Collection<Label>> re: outgoingLabels.entrySet()) {
					for (Label childLabel: re.getValue()) {
						if (childLabel instanceof Action && !((Action) childLabel).getContent().isEmpty()) {
							tAnonymous.add((Action) childLabel);
						}
					}
				}
				
				DynamicTableBuilder<Entry<EReferenceConnection, WidgetFactory>> boundsTableBuilder = new DynamicTableBuilder<>("nsd-ecore-doc-table");
				boundsTableBuilder
					.addStringColumnBuilder("bound", true, false, "Bound", endpoint -> targetNameLink(endpoint.getKey(), endpoint.getValue(), progressMonitor)) 
					.addStringColumnBuilder("description", true, false, "Description", endpoint -> description(endpoint.getKey(), endpoint.getValue(), progressMonitor));
				
				org.nasdanika.html.model.html.Tag attributesTable = boundsTableBuilder.build(
						referenceOutgoingEndpoints,  
						"etypeparameter-bounds", 
						"type-parameter-bounds-table", 
						progressMonitor);
				getBoundsAction((Action) tLabel).getContent().add(attributesTable);
			}
		}
	}				

	@Override
	public Collection<EClassifierNodeProcessor<?>> getEClassifierNodeProcessors(int depth, Predicate<WidgetFactory> predicate, ProgressMonitor progressMonitor) {
		Collection<EClassifierNodeProcessor<?>> ret = new HashSet<>();
		Selector<Collection<EClassifierNodeProcessor<?>>> selector = EClassifierNodeProcessorProvider.createEClassifierNodeProcessorSelector(depth, predicate);
		for (WidgetFactory bwf: eBoundsWidgetFactories.values()) {
			ret.addAll(bwf.select(selector, progressMonitor));
		}
		return ret;
	}
	
}
