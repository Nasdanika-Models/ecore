package org.nasdanika.models.ecore.cli;

import java.util.function.Consumer;

import org.eclipse.emf.ecore.EObject;
import org.nasdanika.cli.ParentCommands;
import org.nasdanika.common.Context;
import org.nasdanika.common.Diagnostic;
import org.nasdanika.common.EObjectSupplier;
import org.nasdanika.common.ProgressMonitor;
import org.nasdanika.html.model.app.gen.cli.HtmlAppGeneratorCommand;
import org.nasdanika.html.model.app.graph.emf.HtmlAppGenerator;
import org.nasdanika.models.ecore.graph.processors.EcoreHtmlAppGenerator;

import picocli.CommandLine.Command;

@Command(
		description = "Generates Ecore model documentation html app model",
		versionProvider = ModuleVersionProvider.class,		
		mixinStandardHelpOptions = true,
		name = "ecore-html-app")
@ParentCommands(EObjectSupplier.class)
public class EcoreHtmlAppGeneratorCommand extends HtmlAppGeneratorCommand {
	
	@Override
	protected HtmlAppGenerator createHtmlAppGenerator(
			ProgressMonitor progressMonitor, 
			Context context, 
			EObject source,
			Consumer<Diagnostic> diagnosticConsumer) {
		
		return EcoreHtmlAppGenerator.loadEcoreHtmlAppGenerator(
				source, 
				context, 
				createPrototypeProvider(progressMonitor), 
				createFactoryPredicate(progressMonitor),
				createEPackagePredicate(progressMonitor),
				diagnosticConsumer, 
				progressMonitor);
	}		
	
}
