Pbind(
	//The SynthDef is passed to the pattern 
	//when play is called by the instance
	\instrument, Pkey(\synthDef),
	\degree, Pxrand([0, 2, 3, 4, 5, 7], inf), 
	\pan, Pseq([0, Pwhite(-1.0, 1.0, 3)], inf), 
	\dur, 0.2,
	\timescale, Pseq([2, Pwhite(0.5, 1.0, 3)], inf) * Pkey(\dur), 
	\ampDB, Pseq([-12, Pwhite(-18, -24, 3)], inf);
);
