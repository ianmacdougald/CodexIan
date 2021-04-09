CodexIncrementer {
	var fileTemplate, <>folder, <>extension;
	var <currentIncrement, previousFileName;

	*new { | fileTemplate = "some_file.wav", folder |
		^super.new
		.folder_(folder)
		.fileTemplate_(fileTemplate)
	}

	fileTemplate_{ | newTemplate |
		var patharr = newTemplate.splitext;
		extension = patharr[1];
		currentIncrement = patharr[0].endNumber;
		fileTemplate = patharr[0].noEndNumbers;
	}

	fileTemplate { ^(fileTemplate++"."++extension) }

	increment {
		if(previousFileName.isNil or: { previousFileName.exists }){
			previousFileName = this.nextFileName;
		};
		^previousFileName;
	}

	reset {
		currentIncrement = -1;
		previousFileName = nil;
	}

	formatFileName { | template |
		var return = folder+/+template;
		if(extension.notNil){
			return = return++"."++extension;
		};
		^return;
	}

	nextFileName {
		var tmpInc = currentIncrement + 1;
		var filename = this.formatFileName(fileTemplate++tmpInc);
		while({filename.exists}, {
			tmpInc = tmpInc + 1;
			filename = this.formatFileName(fileTemplate++tmpInc);
		});
		currentIncrement = tmpInc;
		^filename;
	}

	decrement {
		^this.formatFileName(fileTemplate++(currentIncrement -1));
	}
}