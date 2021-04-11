Codex {
	classvar <directory, quark, cache;
	var <moduleSet, <modules, <>know = true;

	*initClass {
		quark = Main.packages.asDict.at(\Codices);
		Class.initClassTree(CodexStorage);
		try {
			directory = File.readAllString(quark+/+"directory.txt");
		}{
			directory = quark+/+"scmodules";
			File.use(
				quark+/+"directory.txt",
				"w",
				{ | file | file.write(directory) };
			);
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
		).getModules(from).initCodex;
	}

	initCodex { }

	getModules { | from |
		modules = this.class.loadModules(moduleSet, from);
	}

	*loadModules { | set, from |
		var dict = this.cache ?? {
			cache.add(this.name -> Dictionary.new)[this.name];
		};
		var path = this.classFolder+/+set;

		dict[set] ?? {
			if(path.exists){
				this.loadScripts(set);
			} {
				path.mkdir;
				if(from.isNil){
					this.makeTemplates(CodexTemplater(path));
					this.loadScripts(set);
				} {
					dict.add(set -> this.getModules(from));
					fork { (this.classFolder+/+from).copyScriptsTo(path) };
				}
			};
		};

		^dict[set].deepCopy;
	}

	*classFolder { ^(this.directory+/+this.name) }

	*makeTemplates { | templater | }

	*loadScripts { | set |
		this.cache.add(set -> CodexModules(this.classFolder+/+set)
			.loadAll(this.name++"_"++set++"_"));
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
		directory = newPath;
		File.use(
			quark+/+"directory.txt",
			"w",
			{ | file | file.write(directory) };
		);
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
					this.add(this.getKeyFrom(file) -> file.compileFile);
				};
			});
		}
	}

	getKeyFrom { | input |
		var string = PathName(input).fileNameWithoutExtension;
		^(string[0].toLower++string[1..]).asSymbol;
	}

	add { | anAssociation |
		this.put(
			anAssociation.key,
			CodexObject(anAssociation.key, anAssociation.value, this);
		);
	}

	clear {
		this.removeSynthDefs;
		super.clear;
	}

	loadAll { | label |
		//Selecting SynthDefs will evaluate/replace modules.
		var synthDefs = this.synthDefs;
		//However, the selection function will return CodexObjects.
		if(synthDefs.isEmpty.not){
			synthDefs.do { | synthDef |
				synthDef = synthDef.value;
				synthDef.name = (label++synthDef.name).asSymbol;
			};
			fork{
				semaphore.wait;
				synthDefs.do { | synthDef | synthDef.value.add };
				semaphore.signal;
			};
		}
	}

	removeSynthDefs {
		var synthDefs = this.synthDefs;
		if(synthDefs.isEmpty.not){
			fork {
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
	var <>key, <>function, <>envir;

	*new { | key, function, envir |
		^super.newCopyArgs(key, function, envir);
	}

	value { | ... args |
		^envir.use({
			if(envir[key].isNil or: { envir[key] == this }){
				envir[key] = function.value(*args);
			};
			envir[key];
		});
	}

	doesNotUnderstand { | selector ... args |
		^try { this.value(selector, *args).perform(selector, *args) }
		{ this.superPerformList(\doesNotUnderstand, selector, *args) }
	}
}