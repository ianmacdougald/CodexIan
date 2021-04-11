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
	}

	*getModules { | set, from |
		var dict = this.cache ?? {
			cache.add(this.name -> Dictionary.new)[this.name];
		};
		var path = this.classFolder+/+set;

		dict[set] ?? {
			if(path.exists){
				this.addModules(set);
			} {
				path.mkdir;
				if(from.isNil){
					this.makeTemplates(CodexTemplater(path));
					this.addModules(set);
				} {
					dict.add(set -> this.getModules(from));
					fork { (this.classFolder+/+from).copyScriptsTo(path) };
				}
			};
		};

		^dict[set].deepCopy.addAll(this.name++"_"++set++"_");
	}

	*classFolder { ^(this.directory+/+this.name) }

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

	moduleSet_{ | newSet, from |
		moduleSet = newSet;
		this.loadModules(from);
		this.initCodex;
	}

	reloadModules { this.moduleSet = moduleSet }

	reloadScripts {
		this.removeModules;
		this.reloadModules;
	}

	removeModules { this.class.cache.removeAt(moduleSet) }

	*moduleSets {
		^PathName(this.classFolder).folders
		.collectAs({ | m | m.folderName.asSymbol }, Set);
	}

	*directory_{ | newPath("~/".standardizePath) |
		CodexStorage.add(id -> (directory = newPath));
	}

	open { | ... keys |
		var ide = Platform.ideName;
		case
		{ ide=="scqt" }{ this.open_scqt(*keys) }
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

	open_scvim { | shell("sh"), neovim(false), vertically(false) ... keys |
		var cmd = "vim", paths = "";
		keys.do({ | key |
			var current = this.moduleFolder+/+key.asString++".scd";
			if(File.exists(current)){
				paths = paths++current++" ";
			};
		});
		if(neovim, { cmd = $n++cmd });
		if(vertically, { cmd = cmd++" -o "}, { cmd = cmd++" -O " });
		paths.do{ | path | cmd=cmd++path };
		if(cmd.runInGnome(shell).not){
			cmd.runInTerminal(shell);
		};
	}

	openModules { this.open(keys: modules.keys.asArray.sort) }

	closeModules {
		if(Platform.ideName=="scqt", {
			var document = \Document.asClass;
			if(document.notNil, {
				document.perform(\allDocuments).do { | doc, index |
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
	var semaphore;

	*new { | folder |
		^super.new.know_(true).initModules(folder);
	}

	initModules { | folder |
		semaphore = Semaphore.new(1);
		this.compileFolder(folder);
	}

	compileFolder { | folder |
		folder !? {
			this.use({
				PathName(folder).files.do { | file |
					file = file.fullPath;
					this.addToEnvir(
						this.getKeyFrom(file),
						thisProcess.interpreter.compileFile(file);
					);
				};
			});
		}
	}

	getKeyFrom { | input |
		var string = PathName(input).fileNameWithoutExtension;
		^(string[0].toLower++string[1..]).asSymbol;
	}

	addToEnvir { | key, func |
		this.add(key -> CodexObject(key, func));
	}

	clear {
		this.removeAll;
		super.clear;
	}

	addAll { | label |
		fork{
			//Selecting SynthDefs will evaluate/replace modules.
			var synthDefs = this.synthDefs;
			//However, the selection function will return CodexObjects.
			if(synthDefs.isEmpty.not){
				semaphore.wait;
				synthDefs.do { | synthDef |
					synthDef = synthDef.value;
					synthDef.name = (label++synthDef.name).asSymbol;
					synthDef.add;
				};
				semaphore.signal;
			}
		}
	}

	removeAll {
		fork {
			var synthDefs = this.synthDefs;
			if(synthDefs.isEmpty.not){
				semaphore.wait;
				synthDefs.do { | synthDef |
					SynthDef.removeAt(synthDef.value.name);
				};
				semaphore.signal;
			}
		}
	}

	synthDefs {
		^this.array.select { | object |
			object.value.isKindOf(SynthDef);
		};
	}
}

CodexObject {
	var <>key, <>object, <>envir;

	*new { | key, object, envir |
		^super.newCopyArgs(key, object, currentEnvironment);
	}

	value { | ... args |
		if(envir[key]==this){
			envir[key] = object.value(*args);
		};
		^envir[key];
	}

	doesNotUnderstand { | selector ... args |
		^try { this.value(selector, *args).perform(selector, *args) }
		{ this.superPerformList(\doesNotUnderstand, selector, *args) }
	}
}