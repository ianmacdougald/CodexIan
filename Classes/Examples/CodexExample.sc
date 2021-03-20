CodexExample : CodexComposite {
	var player;
	
	//initComposite is called immediately after modules are loaded into the class. 
	//Initialize instance variables here if you don't want to rewrite the constructor.
	initComposite {}

	*makeTemplates { | templater | 
		templater.pattern( "sequence" );
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
