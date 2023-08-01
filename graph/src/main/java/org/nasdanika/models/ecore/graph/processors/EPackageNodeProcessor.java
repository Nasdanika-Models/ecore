package org.nasdanika.models.ecore.graph.processors;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.emf.ecore.ENamedElement;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EcorePackage;
import org.nasdanika.common.Context;
import org.nasdanika.common.ProgressMonitor;
import org.nasdanika.common.Supplier;
import org.nasdanika.graph.emf.EReferenceConnection;
import org.nasdanika.graph.processor.NodeProcessorConfig;
import org.nasdanika.html.model.app.Action;
import org.nasdanika.html.model.app.AppFactory;
import org.nasdanika.html.model.app.Label;
import org.nasdanika.html.model.app.gen.DynamicTableBuilder;
import org.nasdanika.html.model.app.graph.WidgetFactory;
import org.nasdanika.html.model.app.graph.emf.OutgoingReferenceBuilder;

public class EPackageNodeProcessor extends ENamedElementNodeProcessor<EPackage> {

	public EPackageNodeProcessor(
			NodeProcessorConfig<WidgetFactory, WidgetFactory> config, 
			Context context,
			java.util.function.Function<ProgressMonitor, Action> prototypeProvider) {
		super(config, context, prototypeProvider);
	}	
	
	@Override
	public Supplier<Collection<Label>> createLabelsSupplier() {
		return super.createLabelsSupplier().then(this::sortLabels);
	}
	
	protected Collection<Label> sortLabels(Collection<Label> labels) {
		return labels
			.stream()
			.sorted((a,b) -> a.getText().compareTo(b.getText()))
			.toList();		
	}
	
	@Override
	protected boolean isCallOutgoingReferenceLabelsSuppliers(EReference eReference) {
		if (eReference == EcorePackage.Literals.EPACKAGE__ECLASSIFIERS) {
			return true;
		}
		if (eReference == EcorePackage.Literals.EPACKAGE__ESUBPACKAGES) {
			return true;
		}
		return super.isCallOutgoingReferenceLabelsSuppliers(eReference);
	}
	 
	@Override
	protected boolean isCreateActionForUndocumented() {
		return true;
	}
	
	/**
	 * Returns classifiers action, creates if necessary. Matches by location.
	 * @param parent
	 * @return
	 */
	protected Action getClassifiersAction(Action parent) {
		Action pAction = (Action) parent;
		return pAction.getNavigation()
			.stream()
			.filter(e -> e instanceof Action && "classifiers.html".equals(((Action) e).getLocation()))
			.findFirst()
			.map(Action.class::cast)
			.orElseGet(() -> {
				Action classifiersAction = AppFactory.eINSTANCE.createAction();
				classifiersAction.setText("Classifiers");
//				attributesAction.setIcon("https://cdn.jsdelivr.net/gh/Nasdanika-Models/ecore@master/graph/web-resources/icons/EAttribute.gif");
				classifiersAction.setLocation("classifiers.html");
				pAction.getNavigation().add(classifiersAction);
				return classifiersAction;
			});
	}
	
	/**
	 * Returns sub-packages action, creates if necessary. Matches by location.
	 * @param parent
	 * @return
	 */
	protected Action getSubPackagesAction(Action parent) {
		Action pAction = (Action) parent;
		return pAction.getNavigation()
			.stream()
			.filter(e -> e instanceof Action && "sub-packages.html".equals(((Action) e).getLocation()))
			.findFirst()
			.map(Action.class::cast)
			.orElseGet(() -> {
				Action subPackagesAction = AppFactory.eINSTANCE.createAction();
				subPackagesAction.setText("Sub-packages");
				subPackagesAction.setIcon("https://cdn.jsdelivr.net/gh/Nasdanika-Models/ecore@master/graph/web-resources/icons/EPackage.gif");
				subPackagesAction.setLocation("sub-packages.html");
				pAction.getNavigation().add(subPackagesAction);
				return subPackagesAction;
			});
	}
	
	@OutgoingReferenceBuilder(EcorePackage.EPACKAGE__ECLASSIFIERS)
	public void buildEClassifiersOutgoingReference(
			List<Entry<EReferenceConnection, WidgetFactory>> referenceOutgoingEndpoints, 
			Collection<Label> labels,
			Map<EReferenceConnection, Collection<Label>> outgoingLabels, 
			ProgressMonitor progressMonitor) {

		List<Entry<EReferenceConnection, Collection<Label>>> sorted = outgoingLabels.entrySet().stream()
				.sorted((a,b) -> ((ENamedElement) a.getKey().getTarget().get()).getName().compareTo(((ENamedElement) b.getKey().getTarget().get()).getName()))
				.toList();		

		for (Label tLabel: labels) {
			for (Entry<EReferenceConnection, Collection<Label>> re: sorted) {
				tLabel.getChildren().addAll(re.getValue());
			}
			if (tLabel instanceof Action) {										
				DynamicTableBuilder<Entry<EReferenceConnection, WidgetFactory>> classifiersTableBuilder = new DynamicTableBuilder<>("nsd-ecore-doc-table");
				buildClassifierColumns(classifiersTableBuilder, progressMonitor);
				
				org.nasdanika.html.model.html.Tag attributesTable = classifiersTableBuilder.build(
						referenceOutgoingEndpoints.stream().sorted((a,b) -> {
							ENamedElement ane = (ENamedElement) a.getKey().getTarget().get();
							ENamedElement bne = (ENamedElement) b.getKey().getTarget().get();
							return ane.getName().compareTo(bne.getName());
						}).toList(),  
						"epackage-classifiers", 
						"classifiers-table", 
						progressMonitor);
				getClassifiersAction((Action) tLabel).getContent().add(attributesTable);
			}
		}
	}
	
	@OutgoingReferenceBuilder(EcorePackage.EPACKAGE__ESUBPACKAGES)
	public void buildESubPackagesOutgoingReference(
			List<Entry<EReferenceConnection, WidgetFactory>> referenceOutgoingEndpoints, 
			Collection<Label> labels,
			Map<EReferenceConnection, Collection<Label>> outgoingLabels, 
			ProgressMonitor progressMonitor) {

		// A page with a dynamic sub-packages table and links to sub-package pages.
		for (Label label: labels) {
			if (label instanceof Action) {										
				DynamicTableBuilder<Entry<EReferenceConnection, WidgetFactory>> subPackagesTableBuilder = new DynamicTableBuilder<>("nsd-ecore-doc-table");
				buildNamedElementColumns(subPackagesTableBuilder, progressMonitor);
				// TODO
				// getNsPrefix()
				// getNsURI()
				
				org.nasdanika.html.model.html.Tag subPackagesTable = subPackagesTableBuilder.build(
						referenceOutgoingEndpoints.stream().sorted((a,b) -> {
							ENamedElement ane = (ENamedElement) a.getKey().getTarget().get();
							ENamedElement bne = (ENamedElement) b.getKey().getTarget().get();
							return ane.getName().compareTo(bne.getName());
						}).toList(),  
						"epackage-sub-packages", 
						"sub-packages-table", 
						progressMonitor);
				getSubPackagesAction((Action) label).getContent().add(subPackagesTable);
			}
		}
	}	
	
}


