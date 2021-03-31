CodexHybrid : CodexComposite {
	classvar todo, <processor;
	var server;

	*initClass {
		Class.initClassTree(Server);
		Class.initClassTree(SynthDef);
		Class.initClassTree(CodexProcessor);
		processor = CodexProcessor.new;
	}

	*addModules { | key |
		super.addModules(key);
		this.processSynthDefs(key);
	}

	*copyModules { | to, from |
		super.copyModules(to, from);
		this.processSynthDefs(to);
	}

	*processSynthDefs { | key |
		processor.add(this.namedSynthDefs(key));
	}

	*removeSynthDefs { | key |
		processor.remove(this.findSynthDefs(key));
	}

	*selectSynthDefs { | collection |
		^collection.select({ | item |
			item.isKindOf(SynthDef);
		});
	}

	*findSynthDefs { | key |
		^this.cache[key].asArray.flat.select({ | item |
			item.isKindOf(SynthDef);
		});
	}

	*namedSynthDefs { | key |
		var synthDefs = this.findSynthDefs(key);
		synthDefs.do { | synthDef |
			synthDef.name = this.formatName(synthDef.name, key);
		};
		^synthDefs;
	}

	*formatName { | symbol, key |
		var string = this.stripTag(symbol.asString);
		^this.tag(this.name, this.tag(key, string)).asSymbol;
	}

	*stripTag { | string |
		var found = string.findAll($_);
		found !? { if(found.size>=2, { ^string[(found.last+1)..] }) } ;
		^string;
	}

	*tag { | tag, name |
		tag = tag.asString; name = name.asString;
		if(name.contains(tag).not, {
			name = format("%_%", tag, name);
		});
		^name;
	}

	initComposite {
		server = Server.default;
		this.initHybrid;
	}

	server_{ | newServer |
		server = newServer;
		this.class.processSynthDefs(moduleSet);
	}

	server {
		^(server ?? { server = Server.default; server } !? { server });
	}

	initHybrid {}

	reloadScripts {
		this.class.removeSynthDefs(moduleSet);
		super.reloadScripts;
	}
}
