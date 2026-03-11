package org.nasdanika.models.ecore.cli;

import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Collection;
import java.util.Map;
import java.util.function.BiConsumer;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.resource.URIConverter;
import org.nasdanika.common.Context;
import org.nasdanika.common.NasdanikaException;
import org.nasdanika.common.ProgressMonitor;
import org.nasdanika.drawio.Connection;
import org.nasdanika.drawio.Document;
import org.nasdanika.drawio.Layer;
import org.nasdanika.drawio.Model;
import org.nasdanika.drawio.Node;
import org.nasdanika.drawio.Page;
import org.nasdanika.drawio.Root;
import org.nasdanika.graph.emf.EObjectNode;
import org.nasdanika.graph.processor.NodeProcessorConfig;
import org.nasdanika.models.app.Action;
import org.nasdanika.models.app.Label;
import org.nasdanika.models.app.graph.WidgetFactory;
import org.nasdanika.models.ecore.graph.processors.EClassNodeProcessor;
import org.nasdanika.models.ecore.graph.processors.EClassifierNodeProcessor;
import org.nasdanika.models.ecore.graph.processors.EClassifierNodeProcessorFactory;
import org.nasdanika.models.ecore.graph.processors.EDataTypeNodeProcessor;
import org.nasdanika.models.ecore.graph.processors.EEnumNodeProcessor;
import org.nasdanika.models.ecore.graph.processors.EPackageNodeProcessor;
import org.nasdanika.models.ecore.graph.processors.EPackageNodeProcessorFactory;

/**
 * Matches any EPackage. 
 */
@EPackageNodeProcessorFactory
public class EcoreGenProcessorsFactory {

	private Context context;
	private double layoutHeight;
	private double layoutWidth;
	private URI diagramURI;
	private URIConverter uriConverter;
	
	public EcoreGenProcessorsFactory(
			URI diagramURI, 
			URIConverter uriConverter,
			double layoutWidth, 
			double layoutHeight, 
			Context context) {
		
		this.diagramURI = diagramURI;
		this.uriConverter = uriConverter;
		this.layoutWidth = layoutWidth;
		this.layoutHeight = layoutHeight;
		this.context = context;		
	}
	
	@EPackageNodeProcessorFactory
	public EPackageNodeProcessor createEPackageProcessor(
			NodeProcessorConfig<WidgetFactory, WidgetFactory, Object> config, 
			java.util.function.BiFunction<EObject, ProgressMonitor, Action> prototypeProvider,
			BiConsumer<Label, ProgressMonitor> labelConfigurator,
			ProgressMonitor progressMonitor) {		
		return new EPackageNodeProcessor(config, context, prototypeProvider) {
			
			@Override
			public void configureLabel(Object source, Label label, ProgressMonitor progressMonitor) {
				super.configureLabel(source, label, progressMonitor);
				if (labelConfigurator != null) {
					labelConfigurator.accept(label, progressMonitor);
				}
			}
			
			/**
			 * Generating a Drawio diagram
			 */
			@Override
			protected void generateDiagramAndGraphActions(Collection<Label> labels, ProgressMonitor progressMonitor) {
				super.generateDiagramAndGraphActions(labels, progressMonitor);
				
				// TODO - choice of ELK layout and layout parameters.
				if (diagramURI != null && uriConverter != null) {
					try {
						Document document = Document.create(false, null);
						Page page = document.createPage();
						page.setName(getTarget().getName());
						
						Model model = page.getModel();
						Root root = model.getRoot();
						Layer<?> backgroundLayer = root.getLayers().get(0);
						
						generateDrawioDiagram(
							ep -> backgroundLayer,	
							false, 
							false, 
							progressMonitor);
						
						org.nasdanika.drawio.Util.forceLayout(root, layoutWidth, layoutHeight);
						
						try (Writer writer = new OutputStreamWriter(uriConverter.createOutputStream(diagramURI))) {
							writer.write(document.save(null));
						}
					} catch (Exception e) {
						e.printStackTrace();
						throw new NasdanikaException(e);
					}
				}
			}			
			
		};
	}
	
	@EClassifierNodeProcessorFactory
	public EClassifierNodeProcessor<?> createEClassifierProcessor(
			NodeProcessorConfig<WidgetFactory, WidgetFactory, Object> config, 
			java.util.function.BiFunction<EObject, ProgressMonitor, Action> prototypeProvider,
			BiConsumer<Label, ProgressMonitor> labelConfigurator,
			ProgressMonitor progressMonitor) {
		
		EObject eClassifier = ((EObjectNode) config.getElement()).get();
				
		if (eClassifier instanceof EClass) {
			return new EClassNodeProcessor(config, context, prototypeProvider) {
				
				@Override
				public void configureLabel(Object source, Label label, ProgressMonitor progressMonitor) {
					super.configureLabel(source, label, progressMonitor);
					if (labelConfigurator != null) {
						labelConfigurator.accept(label, progressMonitor);
					}
				}	
				
				@Override
				protected EList<? super Action> getMembersActionCollection(Action parent) {
					return parent.getChildren();
				}
				
				@Override
				protected EList<? super Action> getMembersCollection(Action membersAction) {
					return membersAction.getChildren();
				}
				
				@Override
				protected void addDiagramAction(Action action, Action diagramAction) {
					action.getSections().add(diagramAction);
				}
				
				@Override
				protected int getDiagramNodeWidth() {
					return Math.max(getTarget().getName().length() * 5, super.getDiagramNodeWidth());
				}
				
				@Override
				protected void createDrawioConnection(
						URI base, 
						Layer<?> layer, 
						EClassifierNodeProcessor<?> dependency,
						Node diagramNode, Node targetNode) {
					
					EClassifier targetEClassifier = dependency.getTarget();
					
					// Supertype
					if (getTarget().getESuperTypes().contains(targetEClassifier)) {
						// TODO - connect top center of the sub-class to the bottom center of super-class
						Connection inheritance = layer.createConnection(diagramNode, targetNode);
						Map<String, String> style = inheritance.getStyle();
						style.put("edgeStyle", "orthogonalEdgeStyle");
						style.put("rounded", "0");
						style.put("orthogonalLoop", "1");
						style.put("jettySize", "auto");
						style.put("html", "1");
						style.put("endArrow", "block");
						style.put("endFill", "0");
					}
					
					// Reference
					for (EReference ref: getTarget().getEReferences()) {
						if (ref.getEType() == targetEClassifier) {
							Connection refConnection = layer.createConnection(diagramNode, targetNode);
							refConnection.setLabel(ref.getName());
							Map<String, String> style = refConnection.getStyle();
							style.put("rounded", "0");
							style.put("orthogonalLoop", "1");
							style.put("jettySize", "1");
							style.put("html", "1");
							if (ref.isMany()) {
								style.put("startArrow", "diamondThin");
								style.put("startFill", "1");
							}
							WidgetFactory refWidgetFactory = eReferenceWidgetFactories.get(ref.getName());
							if (refWidgetFactory != null) {
								Object refLink = refWidgetFactory.createLink(base, progressMonitor);
								if (refLink instanceof org.nasdanika.models.app.Link) {
									refConnection.setLink(((org.nasdanika.models.app.Link) refLink).getLocation());
								}								
							}
						}
					}
				}
				
			};
		}
		
		if (eClassifier instanceof EEnum) {
			return new EEnumNodeProcessor(config, context, prototypeProvider) {
				
				@Override
				public void configureLabel(Object source, Label label, ProgressMonitor progressMonitor) {
					super.configureLabel(source, label, progressMonitor);
					if (labelConfigurator != null) {
						labelConfigurator.accept(label, progressMonitor);
					}
				}	
				
			};
		}
		
		return new EDataTypeNodeProcessor<EDataType>(config, context, prototypeProvider) {
			
			@Override
			public void configureLabel(Object source, Label label, ProgressMonitor progressMonitor) {
				super.configureLabel(source, label, progressMonitor);
				if (labelConfigurator != null) {
					labelConfigurator.accept(label, progressMonitor);
				}
			}	
			
		};		
	}	
	
}
