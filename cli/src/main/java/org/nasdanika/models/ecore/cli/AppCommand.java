package org.nasdanika.models.ecore.cli;

import org.nasdanika.cli.CommandGroup;

import picocli.CommandLine.Command;

@Command(
		description = "HTML Application model commands",
		name = "app",
		versionProvider = ModuleVersionProvider.class,
		mixinStandardHelpOptions = true,
		subcommands = {
			ModelCommand.class,	
			SiteCommand.class
			// TODO - serve command to serve a site over HTTP instead of generating. Netty, caching, ...
		})
public class AppCommand extends CommandGroup {
	

}
