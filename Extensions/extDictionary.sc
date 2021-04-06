+ Dictionary {
	asYAMLString {
		var string = "";
		this.keysValuesDo({ | key, value, index |
			string = string++format("%: %\n", key, value);
		});
		^string;
	}

	withSymbolKeys {
		var dict = this.class.new;
		this.keysValuesDo({ | key, value |
			dict.add(key.asSymbol -> value);
		});
		^dict;
	}
}

+ Object {
	asYAMLString {
		var str = this.asString;
		^format("%: %\n", str, str);
	}
}


