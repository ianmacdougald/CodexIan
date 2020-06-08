ModularExample : Modular { 
	var routine, pattern;

	//The constructor should supply an initial moduleSet (and from argument) to Modular, otherwise \default will be supplied by default.
	*new {|moduleSet, from|
		^super.new(moduleSet, from);
	}

	//The only actual requirement when developing a Modular-typed class is that the the developer define what kinds of modules to use. 
	//This is done in the makeTemplates method by requesting templates in the following way.
	//Note that Modular instances a ModuleTemplater, storing it in the variable "templater".
	makeTemplates { 
		templater.pattern( "sequence1" ); 
		templater.pattern( "sequence2" ); 
		templater.pattern( "sequence3" );
	}

	//This is an example of a kind of behavior one can make—playing three patterns in a routine. 
	//Note that the class assumes that the modules defined in makeTemplates exist with the same names and with the same types. 
	//However, how they exist is entirely up to the user...
	play { 
		routine = fork{ 
			pattern = modules.sequence1.play;
			8.wait; 
			pattern.stop; 
			pattern = modules.sequence2.play;
			32.wait; 
			pattern.stop;
			pattern = modules.sequence3.play;
			32.wait; 
			pattern.stop;
		};
	}

	//This would stop the speculative routine defined above. 
	stop { 
		routine.stop; 
		pattern.stop; 
	}

}
