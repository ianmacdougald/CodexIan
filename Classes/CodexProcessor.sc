CodexRoutinizer {
	var <>server, <list;

	*new { | server(Server.default) |
		^super.newCopyArgs(server).initRoutinizer;
	}

	initRoutinizer {
		list = List.new;
		ServerBoot.add({ this.run });
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

	run {
		forkIfNeeded {
			while { list.isEmpty.not }{
				this.popAction;
			}
		}
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
	var <>label, semaphore;

	*new{ | server(Server.default) |
		^super.newCopyArgs(server).initProcessor;
	}

	initProcessor {
		semaphore = Semaphore.new(1);
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
		synthDefs = this.labelSynthDefs(*synthDefs);
		fork {
			semaphore.wait;
			adder.process(*synthDefs);
			semaphore.signal;
		};
	}

	remove { | ... synthDefs |
		fork {
			semaphore.wait;
			server.sync;
			remover.process(*synthDefs);
			semaphore.signal;
		};
	}

	server_{ | newServer |
		remover.sever = adder.server = server = newServer;
	}
}