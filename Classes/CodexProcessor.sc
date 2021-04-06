//Think about critical sections: Where we possibly add and remove synthDefs of the same name at the same time.

CodexRoutinizer {
	var <>server, routine, <list;

	*new { | server(Server.default) |
		^super.newCopyArgs(server).initRoutinizer;
	}

	initRoutinizer {
		list = List.new;
		ServerBoot.add({
			if(list.notEmpty, {
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
		synthDefs.select { | item |
			item.value.isKindOf(SynthDef);
		} .do { | item | list.add(item.value) }
	}

	process { | ... synthDefs |
		this.load(*synthDefs.flat);
		this.run;
	}

	pop {
		try { ^list.removeAt(0) }
		{ ^nil };
	}

	popAction {
		var synthDef = this.pop;
		synthDef !? { this.action(synthDef) };
		^synthDef;
	}

	action{ | synthDef | this.subclassResponsibility(thisMethod) }

	makeRoutine {
		^forkIfNeeded({
			while({ list.isEmpty.not }, {
				this.popAction;
			});
		});
	}
}

CodexAdder : CodexRoutinizer {
	action { | synthDef | synthDef.add }
}

CodexRemover : CodexRoutinizer {
	action { | synthDef | SynthDef.removeAt(synthDef.name) }
}

CodexProcessor {
	var <server, adder, remover;
	var <>label;

	*new{ | server(Server.default) |
		^super.newCopyArgs(server).initProcessor;
	}

	initProcessor {
		adder = CodexAdder(server);
		remover = CodexRemover(server);
	}

	getSynthDefs { | ... arguments |
		^arguments.select({ | item |
			item.isKindOf(SynthDef);
		});
	}

	addLabel { | synthDef |
		synthDef.name = (label++synthDef.name).asSymbol;
	}

	labelSynthDefs { | ... synthDefs |
		synthDefs = this.getSynthDefs(*synthDefs.flat);
		synthDefs.do({ | synthDef | this.addLabel(synthDef) });
		^synthDefs;
	}

	add { | ... synthDefs |
		adder.process(*this.labelSynthDefs(*synthDefs));
	}

	remove { | ... synthDefs | remover.process(*synthDefs) }

	server_{ | newServer |
		sender.server = remover.server = adder.server = server = newServer;
	}
}
