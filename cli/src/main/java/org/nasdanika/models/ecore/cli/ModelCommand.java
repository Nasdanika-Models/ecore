package org.nasdanika.models.ecore.cli;

import org.nasdanika.cli.CommandGroup;

import picocli.CommandLine.Command;

@Command(
		description = "Model commands",
		name = "model",
		versionProvider = ModuleVersionProvider.class,
		subcommands = {
			ActionModelCommand.class,
			ModelDocSiteCommand.class
		},		
		mixinStandardHelpOptions = true)
public class ModelCommand extends CommandGroup {
	

}
