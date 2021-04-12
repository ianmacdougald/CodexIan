CodexExample : Codex {
	var player;

	//*contribute is passed a Dictionary instance for adding versions.
	//The key of each entry will become the name of the new module folder.
	//And the path points to where the original modules are stored.
	*contribute { | versions |
		var toQuark = Main.packages.asDict.at(\Codex);
		var toExample = toQuark+/+"Examples/Modules";

		//Add version as an as an Assocation
		versions.add(\example -> toExample);
	}

	*makeTemplates { | templater |
		templater.pattern( "sequence" );
		templater.synthDef( "synthDef" );
	}

	//initCodex is called immediately after modules are loaded into the class.
	//Initialize instance variables here if you don't want to rewrite the constructor.
	initCodex {}

	play { | clock(TempoClock.default) |
		if(player.isPlaying.not, {
			player = modules.sequence.play(clock, modules.asEvent);
		});
	}

	stop {
		if(player.isPlaying, {
			player.stop;
		});
	}
}
