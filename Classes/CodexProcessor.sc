CodexRoutinizer {
	var <>server, routine, <synthDefList;

	*new { | server(Server.default) |
		^super.newCopyArgs(server).initRoutinizer;
	}

	initRoutinizer {
		synthDefList = List.new;
		ServerBoot.add({
			if(synthDefList.notEmpty, {
				routine = this.makeRoutine;
			});
		});
	}

	run {
		if(server.hasBooted, {
			this.stop;
			routine = this.makeRoutine;
		});
	}

	stop {
		if(routine.isPlaying, { 
			routine.stop;
		});
	}

	load { | ... synthDefs |
		synthDefs.flat.do{ | item |
			this.addSynthDef(item);
		};
	}

	process { | synthDef |
		this.load(synthDef);
		this.run;
	}

	addSynthDef { | synthDef |
		if(synthDef.isKindOf(SynthDef), {
			synthDefList.add(synthDef);
		});
	}

	pop { 
		try { ^synthDefList.removeAt(0) }
		{ ^nil };
	}

	popAction {
		var def = this.pop;
		def !? { this.action(def) };
		^def;
	}

	recheckAction { | synthDef |
		var bool = this.bool(synthDef);
		if(bool){ this.action(synthDef) };
		^bool.not;
	}

	bool { | synthDef | this.subclassResponsibility(thisMethod) }

	action{ | synthDef | this.subclassResponsibility(thisMethod) }

	makeRoutine {
		^forkIfNeeded({
			while({ synthDefList.isEmpty.not }, {
				this.popAction;
			});
		});
	}
}

CodexAdder : CodexRoutinizer {
	action { | synthDef | synthDef.add;  }

	bool { | synthDef | ^synthDef.isAdded.not }
}

CodexSender : CodexAdder {
	action { | synthDef | synthDef.send(server) }
}

CodexRemover : CodexRoutinizer {
	action { |synthDef|
		synthDef !? { SynthDef.removeAt(synthDef.name) }
	}

	bool { | synthDef | ^synthDef.isAdded }
}

CodexProcessor {
	var <server, adder, remover, sender;

	*new{ | server(Server.default) |
		^super.newCopyArgs(server).initProcessor;
	}

	initProcessor {
		adder = CodexAdder(server);
		remover = CodexRemover(server);
		sender = CodexSender(server);
	}

	add { | synthDefs | adder.process(synthDefs) }

	send { | synthDefs, targetServer(Server.default) |
		forkIfNeeded({
			var tmp = sender.server;
			sender.server = targetServer;
			sender.process(synthDefs);
			sender.server = tmp;
		});
	}

	remove { | synthDefs | remover.process(synthDefs) }

	server_{ | newServer |
		sender.server = remover.server = adder.server = server = newServer;
	}
}
