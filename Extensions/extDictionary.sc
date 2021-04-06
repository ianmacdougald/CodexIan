+ Dictionary {
	asYAMLString {
		var string = "";
		this.keysValuesDo({|key, value, index|
			string = string++format("%: %\n", key, value);
		});
		^string;
	}

	useSymbolKeys {
		forBy(0, this.array.size - 2, 2, { | index |
			if(array[index].isString){
				array[index] = array[index].asSymbol;
			};
		});
	}
}

+ Object {
	asYAMLString {
		var str = this.asString;
		^format("%: %\n", str, str);
	}
}


