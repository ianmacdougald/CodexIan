CodexExampleHybrid : CodexComposite {
	var player;

	*contribute { | versions |
		var toQuark = Main.packages.asDict.at(\Codices);
		var toExample = toQuark+/+"Examples/Modules";

		versions.add(
			[\example, toExample]
		);
	}

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
