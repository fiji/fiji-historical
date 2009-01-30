w = 512;
off = 0;
dy = 50;

newImage("Untitled", "32-bit Black", w w 1);
if (!true) {
	// cosine
} else {
	//sharp line
	makeLine(off, w / 2 - dy, w - off, w / 2 + dy);
	//makeLine(w / 2, off, w / 2, w - off);
	run("Colors...", "foreground=white");
	run("Draw");
}
run("FFT Line");

