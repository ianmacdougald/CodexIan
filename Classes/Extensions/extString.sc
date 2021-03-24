+ String {
	normalizePathAudio{ | level(1.0), server(Server.default),
		sampleFormat("int24") |
		var buffer, pathname = PathName(this);
		if(level <= 0.0, {level = level.dbamp});
		Buffer.read(server, this, action: { | buffer |
			buffer.normalize(level);
			File.delete(this);
			buffer.write(
				this,
				pathname.extension,
				sampleFormat,
			);
			buffer.free;
		});
	}

	asBuffer { | server(Server.default), startFrame(0),
		numFrames(-1), action, bufnum |
		^Buffer.read(server, this, startFrame,
			numFrames, action, bufnum);
	}

	getBuffers { | server, startFrame, numFrames, action |
		^this.getAudioPaths.collect(
			_.asBuffer(server, startFrame, numFrames, action)
		);
	}

	exists { ^this.pathMatch.isEmpty.not }

	increment {
		var path = PathName(this);
		^CodexIncrementer(
			path.fileName,
			path.pathOnly
		).increment;
	}

	getFilePaths { ^PathName(this).files.collect(_.fullPath) }

	getFileNames { ^PathName(this).files.collect(_.fileName) }

	copyFilesTo { | newDirectory |
		this.getFilePaths.do { | path |
			File.copy(path, newDirectory+/+PathName(path).fileName);
		};
	}

	copyScriptsTo { | newDirectory |
		this.getScriptPaths.do { | path |
			File.copy(path, newDirectory+/+PathName(path).fileName);
		};
	}

	copyFolder { | newFolder | this.copyFilesTo(newFolder.mkdir) }

	endNumber {	// turn consecutive digits at the end of fullPath into a number.
		^this[this.endNumberIndex + 1..].asInteger
	}

	endNumberIndex {
		var index = this.lastIndex;
		while({
			index > 0 and: { this.at(index).isDecDigit }
		}, {
			index = index - 1
		});
		^index
	}
}

+ Symbol {
	isPath { ^this.asString.isPath }
}
