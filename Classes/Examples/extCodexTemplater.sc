+ CodexTemplater {
	hybridExampleFunction { | templateName("sequence") |
		var path = Main.packages.asDict.at(\Codices)
		+/+"Classes/Examples/hybridExampleFunction.scd";
		this.makeTemplate(
			templateName,
			path
		);
	}
}
