CodexStorage  {
	classvar dictionary, <storagePath;

	*write { | item |
		var fd = File.open(storagePath, "w+");
		fd.write(item.asYAMLString);
		fd.close;
	}

	*initClass {
		Class.initClassTree(Main);
		Class.initClassTree(Quarks);
		Class.initClassTree(Dictionary);
		Class.initClassTree(Collection);
		storagePath = Main.packages.asDict.at('Codices')
		+/+format("%.yaml", this.name);
		this.getDictionary;
	}

	*getDictionary {
		dictionary = storagePath.parseYAMLFile ? Dictionary.new;
		dictionary.useSymbolKeys;
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
	*codexStorageEnabled_{ | bool(true) |
		CodexStorage.setAt('__ENABLE_STRING_PSEUDOS__', bool);
	}

	*codexStorageEnabled {
		^CodexStorage.at('__ENABLE_STRING_PSEUDOS__');
	}

	fromCodexStorage { | key |
		var toPrepend;
		//If the key is a number, use it to find a symbol
		if(key.isNumber){
			var symbols = CodexStorage.keys.asArray;
			key = symbols[key.asInteger.clip(0, symbols.size - 1)];
		};
		//Get the path associated with the key
		toPrepend = CodexStorage.at(key);
		//If it exists, return the completed string
		toPrepend !? {
			^(toPrepend+/+this);
		};
		//Otherwise, return the original string
		^this;
	}

	doesNotUnderstand { | selector ... args |
		var bool = CodexStorage.at('__ENABLE_STRING_PSEUDOS__');
		if(bool.notNil and: { bool.interpret }){
			if(selector.isSetter){
				selector = selector.asGetter;
				CodexStorage.add(selector -> args[0]);
			};
			^this.fromCodexStorage(selector);
		};
		^this.superPerformList(\doesNotUnderstand, selector, args);
	}

}
