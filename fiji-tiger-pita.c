#include <stdio.h>
#include <sys/types.h>
#include <sys/sysctl.h>
#include <mach/machine.h>
#include <errno.h>
#include <unistd.h>
#include <sys/param.h>
#include <sys/sysctl.h>
#include <string.h>
#include <stdlib.h>

/*
 * Them stupid Apple software designers -- in their infinite wisdom -- added
 * 64-bit support to Tiger without really supporting it.
 *
 * As a consequence, a universal binary will be executed in 64-bit mode on
 * a x86_64 machine, even if neither CoreFoundation nor Java can be linked,
 * and sure enough, the executable will crash.
 *
 * It does not even reach main(), so we have to provide an _extra_ executable
 * to detect if we're on Tiger, and even worse!  We have to have _yet another_
 * executable, namely a universal binary containing only 32-bit versions,
 * since the Apple software designers -- again, in their infinite wisdom --
 * did not provide a way to force execution of the i386 part of a universal
 * binary if we run on x86_64.
 *
 * So we wrote this program to detect whether we are really running on
 * a MacOSX that can rightfully claim 64-bit support, and hope that we meet
 * the Apple software designers some night, with a baseball bat in our hands.
 */
static int is_leopard(void)
{
	int mib[2] = { CTL_KERN, KERN_OSRELEASE };
	char os_release[128];
	size_t len = sizeof(os_release);;

	return sysctl(mib, 2, os_release, &len, NULL, 0) != -1 &&
		atoi(os_release) > 8;
}

int main(int argc, char **argv)
{
	const char *match = "-tiger-pita";
	int offset = strlen(argv[0]) - strlen(match);

	if (offset < 0 || strcmp(argv[0] + offset, match)) {
		fprintf(stderr, "Could not determine suffix.\n");
		exit(1);
	}
	strcpy(argv[0] + offset, is_leopard() ? "-macosx" : "-tiger");

	if (argc > 1) {
		cpu_type_t cpu_type = CPU_TYPE_I386, result;
		size_t size = sizeof(cpu_type);

		if (sysctlbyname("sysctl.proc_exec_affinity",
				&result, &size, &cpu_type, sizeof(cpu_type))) {
			fprintf(stderr, "Error: %d(%s)\n",
					errno, strerror(errno));
			return 1;
		}
	}

	execv(argv[0], argv);
	fprintf(stderr, "Could not execute %s: %d(%s)\n",
		argv[0], errno, strerror(errno));
	return 1;
}
