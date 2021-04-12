CodexTemplater {
	classvar defaultPath;
	var <>folder;

	*initClass {
		Class.initClassTree(Collection);
		Class.initClassTree(Main);
		Class.initClassTree(Quarks);
		defaultPath = Main.packages.asDict
		.at(\Codex)+/+"Templates";
	}

	*new { | folder |
		folder ?? { Error("No folder set.").throw };
		^super.newCopyArgs(folder.asString);
	}

	synthDef { | templateName("synthDef") |
		this.makeTemplate(templateName, defaultPath+/+"synthDef.scd");
	}

	pattern { | templateName("pattern") |
		this.makeTemplate(templateName, defaultPath+/+"pattern.scd");
	}

	function { | templateName("function") |
		this.makeTemplate(templateName, defaultPath+/+"function.scd");
	}

	synth { | templateName("synth") |
		this.makeTemplate(templateName, defaultPath+/+"node.scd");
	}

	event { | templateName("event") |
		this.makeTemplate(templateName, defaultPath+/+"event.scd");
	}

	array { | templateName("array") |
		this.makeTemplate(templateName, defaultPath+/+"array.scd");
	}

	list { | templateName("list") |
		this.makeTemplate(templateName, defaultPath+/+"list.scd");
	}

	buffer { | templateName("buffer") |
		this.makeTemplate(templateName, defaultPath+/+"buffer.scd");
	}

	blank { | templateName("module") |
		this.makeTemplate(templateName, defaultPath+/+"blank.scd");
	}

	makeTemplate { | templateName, source |
		var fileName, fullPath, i = 0;
		fileName = folder+/+templateName;
		fullPath = fileName++".scd";
		while({ fullPath.exists }){
			i = i + 1;
			fullPath = fileName++i++".scd";
		};
		File.copy(source, fullPath);
	}
}
