package org.nasdanika.models.ecore.cli;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Collection;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.URIConverter;
import org.nasdanika.capability.CapabilityLoader;
import org.nasdanika.capability.CapabilityProvider;
import org.nasdanika.capability.ServiceCapabilityFactory;
import org.nasdanika.capability.ServiceCapabilityFactory.Requirement;
import org.nasdanika.capability.emf.EPackageResourceSetContributor;
import org.nasdanika.capability.emf.ResourceSetContributor;
import org.nasdanika.cli.CommandGroup;
import org.nasdanika.cli.ParentCommands;
import org.nasdanika.common.Context;
import org.nasdanika.common.DefaultConverter;
import org.nasdanika.common.Diagnostic;
import org.nasdanika.common.EModelElementSupplier;
import org.nasdanika.common.EObjectSupplier;
import org.nasdanika.common.MutableContext;
import org.nasdanika.common.ProgressMonitor;
import org.nasdanika.common.Util;
import org.nasdanika.emf.persistence.MarkerFactory;
import org.nasdanika.exec.content.ContentFactory;
import org.nasdanika.exec.content.Interpolator;
import org.nasdanika.exec.content.Markdown;
import org.nasdanika.exec.content.Text;
import org.nasdanika.graph.processor.NodeProcessorConfig;
import org.nasdanika.models.app.Action;
import org.nasdanika.models.app.AppFactory;
import org.nasdanika.models.app.Label;
import org.nasdanika.models.app.graph.WidgetFactory;
import org.nasdanika.models.app.util.LabelSupplier;
import org.nasdanika.models.ecore.graph.processors.EcoreHtmlAppGenerator;
import org.nasdanika.models.ecore.graph.processors.EcoreNodeProcessorFactory;
import org.nasdanika.ncore.util.NcoreUtil;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;


@Command(
		description = {
				"Generates html application model from an ecore model"
		},
		versionProvider = ModuleVersionProvider.class,		
		mixinStandardHelpOptions = true,
		name = "doc")
@ParentCommands(EModelElementSupplier.class)
//@Description()
public class EcoreDocGeneratorCommand extends CommandGroup implements LabelSupplier {
		
	private static final URI README_MD_URI = URI.createURI("readme.md");
	
	private ResourceSet resourceSet;

	public EcoreDocGeneratorCommand(CapabilityLoader capabilityLoader, ResourceSet resourceSet) {
		super(capabilityLoader);
		this.resourceSet = resourceSet;
	}
		
	@ParentCommand
	EObjectSupplier<EObject> eObjectSupplier;
	
	@Option(
			names = "--diagram", 
			description = "Diagram file to generate")	
	private File diagramFile;
		
	@Option(
			names = "--root-location", 
			description = {
				"Root action location",
				"Defaults to ${base-uri}index.html"
			})	
	private String rootLocation;	
	
	@Option(
			names = "--doc-stubs", 
			description = "Create documentation stubs")	
	private boolean docStubs;
	
	@Option(
			names = "--doc-dir", 
			description = "Documentation directory")	
	private File docDir;		
	
	@Option(
			names = "--layout-width", 
			description = {
				"Width for auto-layout of the generated diagram",
				"Defaults to ${DEFAULT-VALUE}"
			},
			defaultValue = "1400")
	private double layoutWidth;
	
	@Option(
			names = "--layout-height", 
			description = {
					"Height for auto-layout of the generated diagram",
					"Defaults to ${DEFAULT-VALUE}"
				},
			defaultValue = "800")	
	private double layoutHeight;	
		
	@Option(
		names = "--model-doc",
		description = {
			"What todo with the model documentation when",
			"documentation resource is available and not empty",
			"Valid values: ${COMPLETION-CANDIDATES}",
			"Default value: ${DEFAULT-VALUE}"
		})
	protected ModelDocAction modelDocAction = ModelDocAction.PREPEND;	
	

	@Override
	public Collection<Label> getEObjects(ProgressMonitor progressMonitor) {
		Collection<EObject> ecoreObjects = eObjectSupplier.getEObjects(progressMonitor);
		URIConverter uriConverter = ecoreObjects
			.stream()
			.map(EObject::eResource)
			.filter(Objects::nonNull)
			.map(Resource::getResourceSet)
			.filter(Objects::nonNull)
			.map(ResourceSet::getURIConverter)
			.findAny()
			.orElse(resourceSet.getURIConverter());
				
		MutableContext context = Context.EMPTY_CONTEXT.fork();
		Consumer<Diagnostic> diagnosticConsumer = d -> d.dump(System.out, 0);
		
		EcoreGenProcessorsFactory ecoreGenProcessorFactory = new EcoreGenProcessorsFactory(
				diagramFile == null ? null : URI.createFileURI(diagramFile.getAbsolutePath()),
				uriConverter,
				layoutWidth,
				layoutHeight,
				context);	
		
		EcoreNodeProcessorFactory ecoreNodeProcessorFactory = new EcoreNodeProcessorFactory(
				context, 
				null,
				diagnosticConsumer,
				ecoreGenProcessorFactory) {
			
			@Override
			protected BiFunction<EObject, ProgressMonitor, Action> getPrototypeProvider(NodeProcessorConfig<WidgetFactory, WidgetFactory, Object> config, String documentation) {
				return (eObj,pm) -> getAction(eObj, documentation, uriConverter);
			}
			
			@Override
			protected boolean shallAddDocumentation(Action action, String documentation) {
				return false;
			}
			
		};
		
		Predicate<ResourceSetContributor> contributorPredicate = contributor -> contributor instanceof EPackageResourceSetContributor;		
		Requirement<Predicate<ResourceSetContributor>, ResourceSetContributor> contributorRequirement = ServiceCapabilityFactory.createRequirement(
				ResourceSetContributor.class, 
				null,
				contributorPredicate);

		Map<EPackage, URI> references = new IdentityHashMap<EPackage, URI>();
		for (CapabilityProvider<Object> resourceSetContributorProvider: capabilityLoader.load(contributorRequirement, progressMonitor)) {
			resourceSetContributorProvider.getPublisher().filter(Objects::nonNull).collectList().block().forEach(contributor -> {
				if (contributor instanceof EPackageResourceSetContributor) {
					EPackageResourceSetContributor ePackageResourceSetContributor = (EPackageResourceSetContributor) contributor;
					URI docURI = ePackageResourceSetContributor.getDocumentationURI();
					if (docURI != null) {
						references.put(ePackageResourceSetContributor.getEPackage(), docURI);
					}
				}
			});
		}
		
		Map<EObject,URI> tmpURIs = new HashMap<>();
		for (EObject eObj: ecoreObjects) {
			tmpURIs.put(eObj, URI.createURI("tmp://" + UUID.randomUUID() + "/" + UUID.randomUUID() + "/"));
		}
				
		Function<? super EObject, URI> uriResolver = eObj -> {
			return tmpURIs.computeIfAbsent(eObj, references::get);
		};
				
		EcoreHtmlAppGenerator eCoreHtmlAppGenerator = new EcoreHtmlAppGenerator(
				ecoreObjects, 
				references.keySet(), // Verify 
				uriResolver,
				ecoreNodeProcessorFactory);
		
		Map<EObject, Collection<Label>> labelMap = eCoreHtmlAppGenerator.generateHtmlAppModel(diagnosticConsumer, progressMonitor);
		if (!Util.isBlank(rootLocation)) {
			labelMap
				.values()
				.stream()
				.filter(Action.class::isInstance)
				.map(Action.class::cast)				
				.forEach(a -> a.setLocation(rootLocation));
		}
		return labelMap
				.values()
				.stream()
				.flatMap(Collection::stream)
				.toList();
	}
		
	protected Action getAction(EObject eObj, String modelDoc, URIConverter uriConverter) {
		if (docDir == null) {
			return null;
		}
		URI docDirURI = URI.createFileURI(docDir.getAbsolutePath()).appendSegment("");
		String path = NcoreUtil.path(eObj);
		if (path == null) {
			return getDocAction(eObj, README_MD_URI.resolve(docDirURI), modelDoc, uriConverter);
		}		
		
		URI actionBaseURI = URI.createURI(path).appendSegment("").resolve(docDirURI);
		return getDocAction(eObj, README_MD_URI.resolve(actionBaseURI), modelDoc, uriConverter);
	} 
	
	protected Action getDocAction(EObject eObj, URI docURI, String modelDoc, URIConverter uriConverter) {
		if (uriConverter != null && uriConverter.exists(docURI, null)) {		
			try {
				String documentation = DefaultConverter.INSTANCE.toString(uriConverter.createInputStream(docURI));
				if (!Util.isBlank(documentation)) {
					if (Util.isBlank(modelDoc)) {
						return createDocAction(documentation, docURI);
					}
					
					String finalDocumentation = switch (modelDocAction) {
					case APPEND:
						yield documentation + System.lineSeparator() + modelDoc;
					case IGNORE:
						yield documentation;
					case INTERPOLATE:
						yield Util.interpolate(documentation, Map.of("model-doc", modelDoc)::get);
					case REPLACE:
						yield modelDoc;
					case PREPEND:
					default:
						yield modelDoc + System.lineSeparator() + documentation;
					};					
					
					return createDocAction(finalDocumentation, docURI);
				}
			} catch (IOException e) {
				return createErrorAction("Error reading documentation from " + docURI + ": " + e);
			}
		} else if (docStubs) {				
			try (Writer writer = new OutputStreamWriter(uriConverter.createOutputStream(docURI))) {
				writer.write("");
				return null;
			} catch (IOException e) {
				return createErrorAction("Error creating documentation stub at " + docURI + ": " + e);
			}							
		}

		return Util.isBlank(modelDoc) ? null : createDocAction(modelDoc, eObj.eResource() != null ? eObj.eResource().getURI() : null);
	}

	protected Action createDocAction(String documentation, URI docURI) {
		Markdown markdown = ContentFactory.eINSTANCE.createMarkdown();
		Interpolator interpolator = ContentFactory.eINSTANCE.createInterpolator();
		Text text = ContentFactory.eINSTANCE.createText();
		text.setContent(documentation);
		interpolator.setSource(text);
		markdown.setSource(interpolator);
		markdown.setStyle(true);
		
		if (docURI != null) {		
			org.nasdanika.ncore.Marker marker = MarkerFactory.INSTANCE.createMarker(docURI.toString(), null);
			markdown.getMarkers().add(marker); 
		}
		Action ret = AppFactory.eINSTANCE.createAction();
		ret.getContent().add(markdown);
		return ret;
	} 

	protected Action createErrorAction(String errorMessage) {
		Action errorAction = AppFactory.eINSTANCE.createAction();				
		Text text = ContentFactory.eINSTANCE.createText(); // Interpolate with element properties?
		text.setContent("<div class=\"nsd-error\">" + errorMessage +  "</div>");
		errorAction.getContent().add(text);
		return errorAction;
	}
	
}
