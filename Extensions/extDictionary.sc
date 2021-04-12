+ Dictionary {
	asYAMLString {
		var string = "";
		this.keysValuesDo { | key, value, index |
			string = string++format("%: %\n", key, value);
		};
		^string;
	}

	withSymbolKeys {
		var newDict = Dictionary.new;
		this.keysValuesDo { | key, value |
			newDict.add(key.asSymbol -> value);
		};
		^newDict;
	}
}