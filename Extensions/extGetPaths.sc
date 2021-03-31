+ String{
	getPaths { ^PathName(this).getPaths }

	getAudioPaths {
		^this.getPaths.select({ | item |
			this.isValidAudioPath(item);
		});
	}

	getScriptPaths {
		^this.getPaths.select({ | item |
			this.isValidScript(item);
		});
	}

	isValidScript { | input | ^(PathName(input).extension=="scd") }

	isValidAudioPath { | input |
		^this.class.validAudioPaths
		.find([PathName(input).extension]).notNil;
	}

	*validAudioPaths {
		^[
			"wav",
			"aiff",
			"oof",
			"mp3"
		];
	}

	getBuffers { ^this.getAudioPaths.collect(_.asBuffer) }
}

+ PathName{
	getPaths {
		var entries;
		if(this.isFile, {
			^[fullPath];
		});
		^this.entries.getPaths;
	}

	getAudioPaths { ^fullPath.getAudioPaths }
}

+ Collection {
	getPaths {
		var strings = [];
		this.do{ | item, index |
			strings = strings++item.getPaths;
		};
		^strings.as(this.class);
	}

	getAudioPaths {
		var strings = [];
		this.do{ | item, index |
			strings = strings++item.getAudioPaths;
		};
		^strings.as(this.class);
	}
}

+ Object {
	getPaths{ ^nil }
}
