CodexExampleHybrid : CodexHybrid {
	var player;

	*contribute { | versions |
		var toQuark = Main.packages.asDict.at(\Codices);
		var toExample = toQuark+/+"Classes/Examples/Modules";

		versions.add(
			[\example, toExample]
		);
	}

	//initHybrid is called immediately after initComposite, 
	//which contains the code that makes CodexHybrid work.
	initHybrid {}

	*makeTemplates { | templater |
		templater.pattern( "sequence" );
		templater.synthDef( "synthDef" );
	}

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
