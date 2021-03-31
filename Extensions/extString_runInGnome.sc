GnomeTerminal {
	classvar exists;

	*initClass { this.lookForTerminal }

	*lookForTerminal {
		if("which gnome-terminal".unixCmdGetStdOut.size>0, {
			exists = true;
		}, { exists = false });
	}

	*runInTerminal { | string, shell = "sh" |
		if(exists, {
			("gnome-terminal -- "+shell+" -i -c "+string.shellQuote).unixCmd;
		});
	}

}

+ String {
	runInGnomeTerminal { | shell = "sh" |
		GnomeTerminal.runInTerminal(this, shell);
	}
}
