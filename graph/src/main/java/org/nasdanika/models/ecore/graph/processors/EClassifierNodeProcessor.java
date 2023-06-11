package org.nasdanika.models.ecore.graph.processors;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EReference;
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
import org.nasdanika.html.model.app.graph.Registry;
import org.nasdanika.html.model.app.graph.WidgetFactory;
import org.nasdanika.html.model.app.graph.emf.OutgoingReferenceBuilder;

public class EClassifierNodeProcessor<T extends EClassifier> extends ENamedElementNodeProcessor<T> {

	public EClassifierNodeProcessor(
			NodeProcessorConfig<Object, WidgetFactory, WidgetFactory, Registry<URI>> config,
			Context context,
			java.util.function.Function<ProgressMonitor, Action> prototypeProvider) {
		super(config, context, prototypeProvider);
	}	
		
	@Override
	protected boolean isCallOutgoingReferenceLabelsSuppliers(EReference eReference) {
		if (eReference == EcorePackage.Literals.ECLASSIFIER__ETYPE_PARAMETERS) {
			return true;
		}
		return super.isCallOutgoingReferenceLabelsSuppliers(eReference);
	}	
	
	private Map<Integer,WidgetFactory> eTypeParametersWidgetFactories = new TreeMap<>();
	
	@OutgoingEndpoint("reference.name == 'eTypeParameters'")
	public final void setETypeParameterEndpoint(EReferenceConnection connection, WidgetFactory eTypeParameterWidgetFactory) {
		eTypeParametersWidgetFactories.put(connection.getIndex(), eTypeParameterWidgetFactory);
	}	
	
	/**
	 * Returns attributes action, creates if necessary. Matches by location.
	 * @param parent
	 * @return
	 */
	protected Action getTypeParametersAction(Action parent) {
		Action pAction = (Action) parent;
		return pAction.getNavigation()
			.stream()
			.filter(e -> e instanceof Action && "type-parameters.html".equals(((Action) e).getLocation()))
			.findFirst()
			.map(Action.class::cast)
			.orElseGet(() -> {
				Action typeParametersAction = AppFactory.eINSTANCE.createAction();
				typeParametersAction.setText("Type Parameters");
				typeParametersAction.setIcon("https://cdn.jsdelivr.net/gh/Nasdanika-Models/ecore@master/graph/web-resources/icons/ETypeParameter.gif");
				typeParametersAction.setLocation("type-parameters.html");
				pAction.getNavigation().add(typeParametersAction);
				return typeParametersAction;
			});
	}
	
	@OutgoingReferenceBuilder(EcorePackage.ECLASSIFIER__ETYPE_PARAMETERS)
	public void buildETypeParametersOutgoingReference(
			List<Entry<EReferenceConnection, WidgetFactory>> referenceOutgoingEndpoints, 
			Collection<Label> labels,
			Map<EReferenceConnection, Collection<Label>> outgoingLabels, 
			ProgressMonitor progressMonitor) {
		
		for (Label tLabel: labels) {
			if (tLabel instanceof Action) {
				Action typeParametersAction = getTypeParametersAction((Action) tLabel);
				EList<Action> tAnonymous = typeParametersAction.getAnonymous();
				for (Entry<EReferenceConnection, Collection<Label>> re: outgoingLabels.entrySet()) {
					for (Label childLabel: re.getValue()) {
						if (childLabel instanceof Action && !((Action) childLabel).getContent().isEmpty()) {
							tAnonymous.add((Action) childLabel);
						}
					}
				}
				
				DynamicTableBuilder<Entry<EReferenceConnection, WidgetFactory>> typeParametersTableBuilder = new DynamicTableBuilder<>("nsd-ecore-doc-table");
				buildNamedElementColumns(typeParametersTableBuilder, progressMonitor);
				// TODO - bounds
				
				org.nasdanika.html.model.html.Tag operationsTable = typeParametersTableBuilder.build(
						referenceOutgoingEndpoints,  
						"eclassifier-type-parameters", 
						"type-parameters-table", 
						progressMonitor);
				typeParametersAction.getContent().add(operationsTable);
			}
		}
	}				
		
	// === Uses ===
	
	// TODO - type references in structural features, operation and parameter types, generic types?
	
	
	@Override
	protected void buildIncomingReference(
			EReference eReference,
			List<Entry<EReferenceConnection, WidgetFactory>> referenceIncomingEndpoints, Collection<Label> labels,
			Map<EReferenceConnection, Collection<Label>> incomingLabels, ProgressMonitor progressMonitor) {
		
		System.out.println("*** " + eReference.getEContainingClass().getName() + "." + eReference.getName());
		
		// TODO Auto-generated method stub
		super.buildIncomingReference(eReference, referenceIncomingEndpoints, labels, incomingLabels, progressMonitor);
	}	
 
	@Override
	protected boolean isCreateActionForUndocumented() {
		return true;
	}
	
}
