CodexStorage  {
	classvar dictionary, path;

	*write { | item |
		var fd = File.open(path, "w+");
		fd.write(item.asYAMLString);
		fd.close;
	}

	*initClass {
		Class.initClassTree(Main);
		Class.initClassTree(Quarks);
		Class.initClassTree(Dictionary);
		Class.initClassTree(Collection);
		path = Main.packages.asDict.at('Codices')
		+/+format("%.yaml", this.name);
		this.getDictionary;
	}

	*getDictionary {
		dictionary = path.parseYAMLFile ?? { Dictionary.new };
		dictionary = dictionary.withSymbolKeys;
	}

	*add { | association |
		dictionary.add(association);
		this.write(dictionary);
	}

	*at { | key | ^dictionary[key] }

	*removeAt { | key |
		var object = dictionary.removeAt(key);
		this.write(dictionary);
		^object;
	}

	*keys { ^dictionary.keys }
}

+ String {
	*codexKnow_{ | bool(true) |
		if(bool.isKindOf(Boolean).not){
			Error("Can only set to Boolean").throw;
		};
		CodexStorage.add('__ENABLE_STRING_PSEUDOS__' -> bool);
	}

	*codexKnow {
		^(CodexStorage.at('__ENABLE_STRING_PSEUDOS__') ? false);
	}

	fromCodexStorage { | key |
		var toPrepend = CodexStorage.at(key);
		toPrepend !? { ^(toPrepend+/+this) };
		^this;
	}

	doesNotUnderstand { | selector ... args |
		var bool = CodexStorage.at('__ENABLE_STRING_PSEUDOS__');
		if(bool.notNil and: { try { bool.interpret }{ bool } }){
			if(selector.isSetter){
				selector = selector.asGetter;
				CodexStorage.add(selector -> args[0]);
			};
			^this.fromCodexStorage(selector);
		};
		^this.superPerformList(\doesNotUnderstand, selector, args);
	}
}