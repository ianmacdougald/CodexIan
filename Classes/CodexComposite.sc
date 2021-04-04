//No need for CodexHybrid or CodexProcessor
//Find a way to add and rename SynthDefs when evaluating a CodexModule
//Basically, just pass the class name and moduleset to the CodexModules
//To pass to each object

//This doesn't address the issue of removing SynthDefs...

CodexComposite {
	classvar <directory, id = 'scmodules', cache;
	var <moduleSet, <modules, <>know = true;

	*initClass {
		Class.initClassTree(Dictionary);
		Class.initClassTree(CodexStorage);
		Class.initClassTree(List);
		directory = CodexStorage.at(id) ?? {
			CodexStorage.setAt(
				id,
				Main.packages.asDict.at(\Codices)
				+/+"scmodules"
			);
		};
		cache = Dictionary.new;
		this.allSubclasses.do({ | class |
			Class.initClassTree(class);
			class.copyVersions;
		});
	}

	*basicNew { | moduleSet, from |
		^super.newCopyArgs(
			moduleSet ?? { Error("No module set specified").throw }
		);
	}

	*new { | moduleSet, from |
		^this.basicNew(moduleSet).initCodex(from);
	}

	initCodex { | from |
		this.loadModules(from).initComposite;
	}

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
		var versions = List.new;
		this.contribute(versions);
		versions.do { | entry |
			if(this.isVersion(entry), {
				var folder = this.classFolder+/+entry[0].asString;
				if(folder.exists.not, {
					entry[1].copyScriptsTo(folder.mkdir);
				})
			});
		}
	}

	*isVersion { | entry |
		^(
			entry.isCollection
			and: { entry.isString.not }
			and: {
				entry.select({ | item |
					item.isString or: { item.isKindOf(Symbol)}
				}).size >= 2
			}
		);
	}

	*contribute { | versions | }

	initComposite {}

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
		this.initComposite;
	}

	*moduleSets {
		^PathName(this.classFolder).folders
		.collectAs({ | m | m.folderName.asSymbol }, Set);
	}

	moduleSets { ^this.class.moduleSets }

	*directory_{ | newPath("~/".standardizePath) |
		directory = CodexStorage.setAt(id, newPath);
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
		if(\GnomeTerminal.asClass.notNil, {
			cmd.perform(\runInGnomeTerminal, shell);
		}, { cmd.perform(\runInTerminal, shell) });
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
	*allCaches { ^cache }

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

	addModule { | moduleName, templateType(\blank) |
		moduleName ?? { Error("No name specified").throw };
		if(modules[moduleName].isNil, {
			CodexTemplater(this.moduleFolder)
			.perform(templateType, moduleName);
			this.reloadScripts;
		}, { warn("Module already exists.") });
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
		}.collect { | key | this.loadModule(key) };
		if(modules.isEmpty.not){
			labels.do { | item |
				processor.label = processor.label++item++"_";
			};
			processor.add(*modules.asArray);
			processor.label = "";
		}
	}

	loadModule { | key ... args|
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

	load { | ... args |
		^envir.use({
			try { envir.loadModule(key, *args) }{
				func.value(*args);
			};
		});
	}

	value { | ... args |
		^func !? { func.value(*args) } ? this;
	}

	doesNotUnderstand { | selector ... args |
		^try { this.load(selector, *args) }
		{ DoesNotUnderstandError(this, selector, args).throw }
	}
}
