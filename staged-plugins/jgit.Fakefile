all <- jgit.jar

BASE=org.spearce.jgit
CLASSPATH(jgit.jar)=$BASE/lib/jsch-0.1.37.jar:$BASE.pgm/lib/args4j-2.0.9.jar

MAINCLASS(jgit.jar)=$BASE.pgm.Main
jgit.jar <- $BASE/src/**/*.java $BASE.pgm/src/**/*.java \
	$BASE.pgm/src/META-INF/services/org.spearce.jgit.pgm.TextBuiltin
