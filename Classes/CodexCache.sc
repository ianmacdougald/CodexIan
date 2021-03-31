CodexCache {
	var dictionary;

	*new { ^super.new.initCache }

	initCache { dictionary = Dictionary.new }

	notAt { | key, subkey |
		this.newDictionary(key);
		^dictionary[key][subkey].isNil;
	}

	newDictionary { | key |
		dictionary[key] ?? {
			dictionary.add(key -> Dictionary.new);
		};
	}

	removeModules {  | key, subkey |
		try { ^dictionary[key].removeAt(subkey) }{ ^nil };
	}

	removeAt { | key |
		^dictionary.removeAt(key);
	}

	at { | key | ^dictionary[key.asSymbol] }

	clear { dictionary.clear; }

	modulesAt { | key, subkey |
		^dictionary[key][subkey].deepCopy;
	}

	copyEntry { | key, prevEntry, newEntry |
		dictionary[key].add(
			newEntry -> this.modulesAt(key, prevEntry)
		);
	}

	keysAt { | key | ^dictionary[key].keys }

	keys { ^dictionary.keys }

	printOn { | stream |
		if (stream.atLimit) { ^this };
		stream << this.class.name << "[ " ;
		dictionary.printItemsOn(stream);
		stream << " ]" ;
	}
}
