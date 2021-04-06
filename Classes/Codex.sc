Codex {
	classvar <directory, id = 'scmodules', cache;
	var <moduleSet, <modules, <>know = true;

	*initClass {
		Class.initClassTree(CodexStorage);
		directory = CodexStorage.at(id) ?? { 
			var path = Main.packages.asDict.at(\Codices)+/+id; 
			CodexStorage.add(id -> path);
			path;
		};
		cache = Dictionary.new;
		this.allSubclasses.do({ | class |
			Class.initClassTree(class);
			class.copyVersions;
		});
	}

	*new { | moduleSet, from |
		^super.newCopyArgs(
			moduleSet ?? { Error("No module set specified").throw }
		).loadModules(from).initCodex;
	}

	initCodex { }

	loadModules { | from |
		modules = this.class.getModules(moduleSet, from);
		modules.loadAll(this.class.name, moduleSet);
	}

	*getModules { | set, from |
		var dict = this.cache;
		var path = this.classFolder+/+set;

		dict ?? {
			dict = Dictionary.new;
			cache.add(this.name -> dict);
		};

		if(dict[set].isNil){
			if(path.exists){
				this.addModules(set);
			} {
				path.mkdir;
				if(from.isNil){
					this.makeTemplates(CodexTemplater(path));
					this.addModules(set);
				} {
					//Copy the modules from 'from' to 'set.'
					dict.add(set -> this.getModules(from));
					//Make the associated folders.
					fork { (this.classFolder+/+from).copyScriptsTo(path) }
					//Otherwise, load from templates.
				}
			};
		};

		^dict[set].deepCopy;
	}

	*classFolder { ^(this.directory +/+ this.name) }

	*makeTemplates { | templater | }

	*addModules { | set |
		this.cache.add(set -> CodexModules(this.classFolder+/+set));
	}

	*copyVersions {
		var versions = Dictionary.new;
		this.contribute(versions);
		versions = versions.asPairs;
		forBy(1, versions.size - 1, 2, { | index |
			var folder = this.classFolder+/+versions[index - 1];
			if(folder.exists.not){
				versions[index].copyScriptsTo(folder.mkdir);
			};
		});
	}

	*contribute { | versions | }

	moduleFolder { ^(this.class.classFolder+/+moduleSet) }

	removeModules {
		try { this.class.cache.removeAt(moduleSet) }
	}

	reloadScripts {
		this.removeModules;
		this.moduleSet = moduleSet;
	}

	reloadModules { this.moduleSet = moduleSet }

	moduleSet_{ | newSet, from |
		moduleSet = newSet;
		this.loadModules(from);
		this.initCodex;
	}

	*moduleSets {
		^PathName(this.classFolder).folders
		.collectAs({ | m | m.folderName.asSymbol }, Set);
	}

	moduleSets { ^this.class.moduleSets }

	*directory_{ | newPath("~/".standardizePath) |
		directory = CodexStorage.add(id -> newPath);
	}

	open { | ... keys |
		var ide = Platform.ideName;
		keys = keys.flat;
		case { ide=="scqt" }{ this.open_scqt(keys: keys) }
		{ ide=="scnvim" }{
			var shell = "echo $SHELL".unixCmdGetStdOut.split($/).last;
			shell = shell[..(shell.size - 2)];
			this.open_scvim(shell, true, true, keys: keys);
		}
		{ ide=="scvim" }{
			var shell = "echo $SHELL".unixCmdGetStdOut.split($/).last;
			shell = shell[..(shell.size - 2)];
			this.open_scvim(shell, false, true, keys: keys);
		};
	}

	open_scqt { | ... keys |
		var document = \Document.asClass;
		if(document.notNil, {
			keys.do{ | item |
				var file = this.moduleFolder+/+item.asString++".scd";
				if(File.exists(file), {
					document.perform(\open, file);
				});
			};
		});
	}

	open_scvim {
		| shell("sh"), neovim(false), vertically(false) ...keys |
		var cmd = "vim", paths = "";
		keys.do({ | item |
			var current = this.moduleFolder+/+item.asString++".scd";
			if(File.exists(current))
			{
				paths = paths++current++" ";
			};
		});
		if(neovim, { cmd = $n++cmd });
		if(vertically, { cmd = cmd++" -o "}, { cmd = cmd++" -O " });
		paths.do{ | path | cmd=cmd++path};
		if(cmd.runInGnome(shell).not){
			cmd.runInTerminal(shell);
		};
	}

	openModules { this.open(keys: modules.keys.asArray.sort) }

	closeModules {
		if(Platform.ideName=="scqt", {
			if(\Document.asClass.notNil, {
				\Document.asClass.perform(\allDocuments).do {
					| doc, index |
					if(doc.dir==this.moduleFolder, {
						doc.close;
					});
				}
			});
		})
	}

	*clearCache {
		var toClear = cache.removeAt(this.name);
		toClear !? { toClear.clear };
	}

	*cache { ^cache.at(this.name) }

	doesNotUnderstand { | selector ... args |
		if(know, {
			var module = modules[selector];
			module !? {
				^module.functionPerformList(
					\value,
					modules,
					args
				);
			};
			if(selector.isSetter, {
				if(args[0].isKindOf(modules[selector.asGetter].class), {
					^modules[selector.asGetter] = args[0];
				}, {
					warn(
						"Can only overwrite pseudo-variable"
						++"with object of the same type."
					);
					^this;
				});
			});
		});
		^this.superPerformList(\doesNotUnderstand, selector, args);
	}
}

CodexModules : Environment {
	var <processor;

	*new { | folder |
		^super.new.initModules(folder);
	}

	initModules { | folder |
		this.know_(true);
		processor = CodexProcessor.new;
		this.compileFolder(folder);
	}

	compileFolder { | folder |
		folder !? {
			PathName(folder).files.do { | file |
				this.compilePath(file.fullPath);
			};
		};
	}

	getKeyFrom { | input |
		var string = PathName(input).fileNameWithoutExtension;
		^(string[0].toLower++string[1..]).asSymbol;
	}

	compilePath { | path |
		var key = this.getKeyFrom(path);
		this.use({
			this.at(key) ?? {
				var func = thisProcess.interpreter.compileFile(path);
				this.addToEnvir(key, func);
			};
		});
	}

	addToEnvir { | key, func |
		this.add(key -> CodexModule(key, func));
	}

	loadAll { | ... labels |
		var modules = this.keys.select { | key |
			this.at(key).isKindOf(CodexModule);
		}.collect { | key | this.unpackModule(key) };
		if(modules.isEmpty.not){
			labels.do { | item |
				processor.label = processor.label++item++"_";
			};
			processor.add(*modules.asArray);
			processor.label = "";
		}
	}

	unpackModule { | key ... args|
		^this.use({
			this[key] = this[key].value(*args);
			this[key];
		});
	}

	clear {
		processor.remove(this.asArray);
		super.clear;
	}
}

CodexModule {
	var <>key, <>func, <>envir;

	*new { | key, func |
		^super.newCopyArgs(key, func, currentEnvironment);
	}

	unpack { | ... args |
		^envir.use({
			try { envir.unpackModule(key, *args) }{
				func.value(*args);
			};
		});
	}

	value { | ... args |
		^func !? { func.value(*args) } ? this;
	}

	doesNotUnderstand { | selector ... args |
		^try { this.unpack(selector, *args) }
		{ DoesNotUnderstandError(this, selector, args).throw }
	}
}
