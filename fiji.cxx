#include <JavaVM/jni.h>
#include <stdlib.h>
#include <sys/stat.h>

#include <iostream>
using std::cerr;
using std::endl;

#include <string>
using std::string;

#ifdef MACOSX
#include <pthread.h>
#include <CoreFoundation/CoreFoundation.h>
#endif

#ifdef MINGW32
#include <process.h>
#define PATH_SEP ";"
#else
#define PATH_SEP ":"
#endif

static const char *relative_java_home = JAVA_HOME;
static const char *library_path = JAVA_LIB_PATH;
#ifdef JAVA_OSEXT
static const char *java_osext = JAVA_OSEXT;
#else
static const char *java_osext = NULL;
#endif
#ifdef JAVA_EXT
static const char *java_ext = JAVA_EXT;
#else
static const char *java_ext = NULL;
#endif

// memory allocation, defaults to 2/3 of the available memory
// -Xmx...m = <memory found (in bytes)> / memory_fraction / 1024
// Note: -Xmx expects memory in MB
static size_t memory_fraction = 1024 * 3 / 2;

// FIXME: these may need to change on Windows
#include <sys/types.h>
#include <dirent.h>

/* Dynamic library loading stuff */

#ifdef MINGW32
#include <windows.h>
#define RTLD_LAZY 0
static char *dlerror_value;

static void *dlopen(const char *name, int flags)
{
	void *result = LoadLibrary(name);
	DWORD error_code = GetLastError();
	LPSTR buffer;

	FormatMessage(FORMAT_MESSAGE_ALLOCATE_BUFFER |
			FORMAT_MESSAGE_FROM_SYSTEM |
			FORMAT_MESSAGE_IGNORE_INSERTS,
			NULL,
			error_code,
			MAKELANGID(LANG_NEUTRAL, SUBLANG_DEFAULT),
			(LPSTR)&buffer,
			0, NULL);
	dlerror_value = buffer;

	return result;
}

static char *dlerror(void)
{
	return dlerror_value;
}

static void *dlsym(void *handle, const char *name)
{
	void *result = (void *)GetProcAddress((HMODULE)handle, name);
	dlerror_value = result ? NULL : (char *)"function not found";
	return result;
}

static void sleep(int seconds)
{
	Sleep(seconds * 1000);
}
#else
#include <dlfcn.h>
#endif



/* Determining heap size */

#ifdef MACOSX
#include <mach/mach_init.h>
#include <mach/mach_host.h>

size_t get_memory_size(int available_only)
{
	host_priv_t host = mach_host_self();
	vm_size_t page_size;
	vm_statistics_data_t host_info;
	mach_msg_type_number_t host_count =
		sizeof(host_info) / sizeof(integer_t);

	host_page_size(host, &page_size);
	return host_statistics(host, HOST_VM_INFO,
			(host_info_t)&host_info, &host_count) ?
		0 : ((size_t)(available_only ? host_info.free_count :
				host_info.active_count +
				host_info.inactive_count +
				host_info.wire_count) * (size_t)page_size);
}
#elif defined(linux)
size_t get_memory_size(int available_only)
{
	ssize_t page_size = sysconf(_SC_PAGESIZE);
	ssize_t available_pages = sysconf(available_only ?
			_SC_AVPHYS_PAGES : _SC_PHYS_PAGES);
	return page_size < 0 || available_pages < 0 ?
		0 : (size_t)page_size * (size_t)available_pages;
}
#elif defined(WIN32)
#include <windows.h>

size_t get_memory_size(int available_only)
{
	MEMORYSTATUS status;

	GlobalMemoryStatus(&status);
	return available_only ? status.dwAvailPhys : status.dwTotalPhys;
}
#else
size_t get_memory_size(int available_only)
{
	fprintf(stderr, "Unsupported\n");
	return 0;
}
#endif



/* Java stuff */

#ifndef JNI_CREATEVM
#define JNI_CREATEVM "JNI_CreateJavaVM"
#endif

const char *fiji_dir;
char **main_argv;
int main_argc;
const char *main_class = "ij.ImageJ";


static char *get_fiji_dir(const char *argv0)
{
	const char *slash = strrchr(argv0, '/');
	static char buffer[PATH_MAX];
#ifdef WIN32
	const char *backslash = strrchr(argv0, '\\');

	if (backslash && slash < backslash)
		slash = backslash;
#endif

	if (slash)
		snprintf(buffer, slash - argv0 + 1, argv0);
	else
		sprintf(buffer, ".");

	return buffer;
}


static int create_java_vm(JavaVM **vm, void **env, JavaVMInitArgs *args)
{
	char java_home[PATH_MAX], buffer[PATH_MAX];
	void *handle;
	char *err;
	static jint (*JNI_CreateJavaVM)(JavaVM **pvm, void **penv, void *args);

	snprintf(java_home, sizeof(java_home), "JAVA_HOME=%s/%s",
			fiji_dir, relative_java_home);
	putenv(java_home);
	snprintf(buffer, sizeof(buffer), "%s/%s", java_home + 10, library_path);

	handle = dlopen(buffer, RTLD_LAZY);
	if (!handle) {
		const char *error = dlerror();
		if (!error)
			error = "(unknown error)";
		cerr << "Could not load Java library '" <<
			buffer << "': " << error << endl;
		return 1;
	}
	dlerror(); /* Clear any existing error */

	JNI_CreateJavaVM = (typeof(JNI_CreateJavaVM))dlsym(handle,
			JNI_CREATEVM);
	err = dlerror();
	if (err) {
		cerr << "Error loading libjvm: " << err << endl;
		return 1;
	}

	return JNI_CreateJavaVM(vm, env, args);
}

static int headless;

int build_classpath(string &result, string jar_directory, int no_error) {
	if (result == "") {
		result = "-Djava.class.path=";
		if (headless)
			result += string(fiji_dir) + "/misc/headless.jar:";
		result += fiji_dir;
		result += "/ij.jar";
	}
	DIR *directory = opendir(jar_directory.c_str());
	if (!directory) {
		if (no_error)
			return 0;
		cerr << "Failed to open: " << jar_directory << endl;
		return 1;
	}
	string extension(".jar");
	unsigned int extension_length = extension.size();
	struct dirent *entry;
	while (NULL != (entry = readdir(directory))) {
		string filename(entry->d_name);
		unsigned int n = filename.size();
		if (n <= extension_length)
			continue;
		unsigned int extension_start = n - extension_length;
		if (!filename.compare(extension_start,
					extension_length,
					extension))
			result += PATH_SEP + jar_directory + "/" + filename;
		else {
			if (filename != "." && filename != ".." &&
					build_classpath(result, jar_directory
						+ "/" + filename, 1))
				return 1;
			continue;
		}

	}
	return 0;
}

struct string_array {
	char **list;
	int nr, alloc;
};

static void append_string(struct string_array& array, char *str)
{
	if (array.nr >= array.alloc) {
		array.alloc = 2 * array.nr + 16;
		array.list = (char **)realloc(array.list,
				array.alloc * sizeof(str));
	}
	array.list[array.nr++] = str;
}

static void prepend_string(struct string_array& array, char *str)
{
	if (array.nr >= array.alloc) {
		array.alloc = 2 * array.nr + 16;
		array.list = (char **)realloc(array.list,
				array.alloc * sizeof(str));
	}
	memmove(array.list + 1, array.list, array.nr * sizeof(str));
	array.list[0] = str;
	array.nr++;
}

static void prepend_string(struct string_array& array, const char *str)
{
	prepend_string(array, strdup(str));
}

static void append_string_array(struct string_array& target,
		struct string_array &source)
{
	if (target.alloc - target.nr < source.nr) {
		target.alloc += source.nr;
		target.list = (char **)realloc(target.list,
				target.alloc * sizeof(target.list[0]));
	}
	memcpy(target.list + target.nr, source.list,
			source.nr * sizeof(target.list[0]));
	target.nr += source.nr;
}

static JavaVMOption *prepare_java_options(struct string_array& array)
{
	JavaVMOption *result = (JavaVMOption *)calloc(array.nr,
			sizeof(JavaVMOption));

	for (int i = 0; i < array.nr; i++)
		result[i].optionString = array.list[i];

	return result;
}

static jobjectArray prepare_ij_options(JNIEnv *env, struct string_array& array)
{
	jstring jstr;
	jobjectArray result;

	if (!(jstr = env->NewStringUTF(array.nr ? array.list[0] : ""))) {
fail:
		cerr << "Failed to create ImageJ option array" << endl;
		exit(1);
	}

	result = env->NewObjectArray(array.nr,
			env->FindClass("java/lang/String"), jstr);
	if (!result)
		goto fail;
	for (int i = 1; i < array.nr; i++) {
		if (!(jstr = env->NewStringUTF(array.list[i])))
			goto fail;
		env->SetObjectArrayElement(result, i, jstr);
	}
	return result;
}

struct options {
	struct string_array java_options, ij_options;
	int debug, use_system_jvm;
};

static void add_option(struct options& options, char *option, int for_ij)
{
	append_string(for_ij ?
			options.ij_options : options.java_options,
			option);
}

static void add_option(struct options& options, const char *option, int for_ij)
{
	add_option(options, strdup(option), for_ij);
}

static void add_option(struct options& options, string &option, int for_ij)
{
	add_option(options, option.c_str(), for_ij);
}

static void show_commandline(struct options& options)
{
	cerr << "java";
	for (int j = 0; j < options.java_options.nr; j++)
		cerr << " " << options.java_options.list[j];
	cerr << " " << main_class;
	for (int j = 0; j < options.ij_options.nr; j++)
		cerr << " " << options.ij_options.list[j];
	cerr << endl;
}

/* the maximal size of the heap on 32-bit systems, in megabyte */
#define MAX_32BIT_HEAP 1920

/*
 * The signature of start_ij() is funny because on MacOSX, it has to be called
 * via pthread_create().
 */
static void *start_ij(void *dummy)
{
	JavaVM *vm;
	struct options options;
	JavaVMInitArgs args;
	JNIEnv *env;
	static string class_path;
	static string ext_path;
	static char plugin_path[PATH_MAX] = "";
	// static char ext_path[65536] = "";
	static char java_home_path[65536];
	int dashdash = 0;

	size_t memory_size = 0;
	
	memset(&options, 0, sizeof(options));

	
#ifdef MACOSX
	
	/* Reading the command line options from the Info.plist file in the Application bundle
	 This routine expects a separate dictionary for fiji with the options from the command
	 line as keys. Currently the --heap and --system is implemented. 
	 If Info.plist is not present (i.e. if started from the cmd-line), the whole thing will
	 be just skipped.
	 Example: Setting the java heap to 1024m
	 <key>fiji</key>
	 <dict>
		<key>heap</key>
		<string>1024</string>
	 </dict>
	 */
	
	CFBundleRef		fijiBundle;
	CFDictionaryRef bundleInfoDict;
	CFDictionaryRef fijiInfoDict;
    CFStringRef     propertyString;
	char c_propertyString[1024];
	
	// Get the main bundle for the app
	fijiBundle = CFBundleGetMainBundle();

	if ( fijiBundle != NULL ) {

		// Get an instance of the non-localized keys.
		bundleInfoDict = CFBundleGetInfoDictionary( fijiBundle );
		
		// If we succeeded, look for a fiji dictionary.
		if ( bundleInfoDict != NULL ) {
			
			fijiInfoDict = (CFDictionaryRef) CFDictionaryGetValue( bundleInfoDict, CFSTR("fiji") );
			
			// If we succeeded, look for fiji property.
			if ( fijiInfoDict != NULL ) {
		
				propertyString = (CFStringRef) CFDictionaryGetValue( fijiInfoDict, CFSTR("heap") );
				if ( propertyString != NULL ) {
					CFStringGetCString(propertyString, c_propertyString, 1024, kCFStringEncodingMacRoman);
					cerr << "heap = " << c_propertyString << "\n";
					size_t java_heap = atol(c_propertyString);
					if ( java_heap > 0 ) {
						memory_size = java_heap * 1024;
						memory_fraction = 1;
					}
				}
				propertyString = (CFStringRef) CFDictionaryGetValue( fijiInfoDict, CFSTR("system") );
				if ( propertyString != NULL ) {
					CFStringGetCString(propertyString, c_propertyString, 1024, kCFStringEncodingMacRoman);
					cerr << "system = " << c_propertyString << "\n";
					if ( atol(c_propertyString) > 0 ) {
						options.use_system_jvm++;
					}
				}
				propertyString = (CFStringRef) CFDictionaryGetValue( fijiInfoDict, CFSTR("ext") );
				if ( propertyString != NULL ) {
					CFStringGetCString(propertyString, c_propertyString, 1024, kCFStringEncodingMacRoman);
					cerr << "ext = " << c_propertyString << "\n";
					ext_path = c_propertyString;
				}
			}
		}
	}
	
#endif
	
	
	int count = 1;
	for (int i = 1; i < main_argc; i++)
		if (!strcmp(main_argv[i], "--"))
			dashdash = count;
		else if (!strcmp(main_argv[i], "--dry-run"))
			options.debug++;
		else if (!strcmp(main_argv[i], "--system"))
			options.use_system_jvm++;
		else if (!strncmp(main_argv[i], "--plugins=", 10))
			snprintf(plugin_path, sizeof(plugin_path),
					"-Dplugins.dir=%s", main_argv[i] + 10);
		else if (!strncmp(main_argv[i], "--ext=", 6))
			ext_path += main_argv[i] + 6;
		else if (!strncmp(main_argv[i], "--heap=", 7)) {
			/* This option sets the memory size (in mb) */
			memory_size = atol(main_argv[i] + 7) * 1024;
			memory_fraction = 1;
		}
		else if (!strcmp(main_argv[i], "--headless")) {
			headless = 1;
			/* handle "--headless script.ijm" gracefully */
			if (i + 2 == main_argc && main_argv[i + 1][0] != '-')
				dashdash = count;
		}
		else if (!strcmp(main_argv[i], "--jython"))
			main_class = "org.python.util.jython";
		else if (!strcmp(main_argv[i], "--jruby"))
			main_class = "org.jruby.Main";
		else if (!strncmp(main_argv[i], "--main-class=", 13))
			main_class = main_argv[i] + 13;
		else
			main_argv[count++] = main_argv[i];
	main_argc = count;

	// if arguments don't set the memory size, set it after available memory
	if ( memory_size == 0 ) {
		memory_size = get_memory_size(0);
	}
	static char heap_size[1024];
	
	// Dealing with the -Djava.ext.dirs option
	//
	cerr << "java_ext" << java_ext << "\n";
	// Only if command line or bundle hasn't set the ext_path 
	// set the ext_path to the compiler option (if JAVA_EXT compiler option was given)
	if ( ext_path.size() == 0 && java_ext != NULL ) {
		ext_path = fiji_dir;
		ext_path += "/";
		ext_path += relative_java_home;
		ext_path += "/";
		ext_path += java_ext;
	}
	// Add a -Djava.ext.dirs if ext_path has been set by bundle, command line option or compiler flag 
	if ( ext_path.size() != 0 ) {
		// if there are OS specific extensions, append them to the ext_path
		if ( java_osext != NULL ) {
			ext_path += ":";
			ext_path += java_osext;
		}
		ext_path.insert(0, "-Djava.ext.dirs=");
		add_option(options, ext_path.c_str(), 0);
	}
	
	
	if (build_classpath(class_path, string(fiji_dir) + "/plugins", 0))
		return NULL;
	if (build_classpath(class_path, string(fiji_dir) + "/jars", 0))
		return NULL;
	add_option(options, class_path, 0);

	if (!plugin_path[0])
		snprintf(plugin_path, sizeof(plugin_path),
				"-Dplugins.dir=%s", fiji_dir);
	add_option(options, plugin_path, 0);

	if (memory_size > 0) {
		memory_size = memory_size / memory_fraction / 1024;
		if (sizeof(void *) == 4 && memory_size > MAX_32BIT_HEAP)
			memory_size = MAX_32BIT_HEAP;
		snprintf(heap_size, sizeof(heap_size),
			"-Xmx%dm", (int)memory_size);
		add_option(options, heap_size, 0);
		cerr << "Java heap arg: " << heap_size << "\n";
	}

	if (headless)
		add_option(options, "-Djava.awt.headless=true", 0);

	if (dashdash) {
		if (headless)
			dashdash--;
		for (int i = 1; i < dashdash; i++)
			add_option(options, main_argv[i], 0);
		main_argv += dashdash;
		main_argc -= dashdash;
	}

	if (!strcmp(main_class, "ij.ImageJ"))
		add_option(options, "-port0", 1);

	/* handle "--headless script.ijm" gracefully */
	if (headless && !strcmp(main_class, "ij.ImageJ")) {
		if (main_argc < 2) {
			cerr << "--headless without a parameter?" << endl;
			exit(1);
		}
		if (*main_argv[1] != '-')
			add_option(options, "-batch", 1);
	}

	for (int i = 1; i < main_argc; i++)
		add_option(options, main_argv[i], 1);

	if (!headless &&
#ifdef MACOSX
			!getenv("SECURITYSESSIONID")
#elif defined(__linux__)
			!getenv("DISPLAY")
#else
			false
#endif
			) {
		cerr << "No GUI detected.  You might want to use the"
			<< " --headless option." << endl;
		exit(1);
	}

	if (options.debug) {
		show_commandline(options);
		exit(0);
	}
		
	memset(&args, 0, sizeof(args));
	args.version  = JNI_VERSION_1_2;
	args.options = prepare_java_options(options.java_options);
	args.nOptions = options.java_options.nr;
	args.ignoreUnrecognized = JNI_FALSE;

	if (options.use_system_jvm)
		env = NULL;
	else if (create_java_vm(&vm, (void **)&env, &args)) {
		cerr << "Warning: falling back to System JVM" << endl;
		env = NULL;
	} else {
		snprintf(java_home_path, sizeof(java_home_path),
				"-Djava.home=%s/%s",
				fiji_dir, relative_java_home);
		prepend_string(options.java_options, java_home_path);
	}

	if (env) {
		jclass instance;
		jmethodID method;
		jobjectArray args;

		string slashed(main_class);
		replace(slashed.begin(), slashed.end(), '.', '/');
		if (!(instance = env->FindClass(slashed.c_str()))) {
			cerr << "Could not find " << main_class << endl;
			exit(1);
		} else if (!(method = env->GetStaticMethodID(instance,
				"main", "([Ljava/lang/String;)V"))) {
			cerr << "Could not find main method" << endl;
			exit(1);
		}

		args = prepare_ij_options(env, options.ij_options);
		env->CallStaticVoidMethodA(instance,
				method, (jvalue *)&args);
		if (vm->DetachCurrentThread())
			cerr << "Could not detach current thread"
				<< endl;
		/* This does not return until ImageJ exits */
		vm->DestroyJavaVM();
	} else {
		/* fall back to system-wide Java */
#ifdef MACOSX
		/* 
		 MacOSX specific stuff for system java
		 -------------------------------------
		 Non-macosx works but places java into separate pid,
		 which causes all kinds of strange behaviours (app can launch multiple times, etc).
		 
		 Search for system wide java >= 1.5 
		 and if found, launch Fiji with the system wide java 
		 This is an adaptation from simple.c from Apple's simpleJavaLauncher code
		*/
		 
		CFStringRef targetJVM = CFSTR("1.5"); // Minimum Java version 1.5
		CFBundleRef JavaVMBundle;
		CFURLRef    JavaVMBundleURL;
		CFURLRef    JavaVMBundlerVersionsDirURL;
		CFURLRef    TargetJavaVM;
		UInt8 pathToTargetJVM [PATH_MAX] = "\0";
		struct stat sbuf;
		
		// Look for the JavaVM bundle using its identifier
		JavaVMBundle = CFBundleGetBundleWithIdentifier(CFSTR("com.apple.JavaVM") );
		
		if(JavaVMBundle != NULL) {
			// Get a path for the JavaVM bundle
			JavaVMBundleURL = CFBundleCopyBundleURL(JavaVMBundle);
			CFRelease(JavaVMBundle);
			
			if(JavaVMBundleURL != NULL) {
				// Append to the path the Versions Component
				JavaVMBundlerVersionsDirURL = CFURLCreateCopyAppendingPathComponent(kCFAllocatorDefault,JavaVMBundleURL,CFSTR("Versions"),true);
				CFRelease(JavaVMBundleURL);
				
				if(JavaVMBundlerVersionsDirURL != NULL) {
					// Append to the path the target JVM's Version
					TargetJavaVM = CFURLCreateCopyAppendingPathComponent(kCFAllocatorDefault,JavaVMBundlerVersionsDirURL,targetJVM,true);
					CFRelease(JavaVMBundlerVersionsDirURL);
					
					if(TargetJavaVM != NULL) {
						if(CFURLGetFileSystemRepresentation (TargetJavaVM,true,pathToTargetJVM,PATH_MAX )) {
							// Check to see if the directory, or a sym link for the target JVM directory exists, and if so set the
							// environment variable JAVA_JVM_VERSION to the target JVM.
							if(stat((char*)pathToTargetJVM,&sbuf) == 0) {
								// Ok, the directory exists, so now we need to set the environment var JAVA_JVM_VERSION to the CFSTR targetJVM
								// We can reuse the pathToTargetJVM buffer to set the environement var.
								if(CFStringGetCString(targetJVM,(char*)pathToTargetJVM,PATH_MAX,kCFStringEncodingUTF8))
									setenv("JAVA_JVM_VERSION", (char*)pathToTargetJVM,1);
							}
						}
						CFRelease(TargetJavaVM);
					}
				}
			}
		}
		
		cerr << "ext=" << ext_path << "\n";
				
		/* JNI_VERSION_1_4 is used on Mac OS X to indicate the 1.4.x and later JVM's */
		memset(&args, 0, sizeof(args));
		args.version  = JNI_VERSION_1_4;
		args.options = prepare_java_options(options.java_options);
		args.nOptions = options.java_options.nr;
		args.ignoreUnrecognized = JNI_TRUE;
		
		vm = NULL; env = NULL;
		
		/* start a VM session */    
		int result = JNI_CreateJavaVM(&vm, (void**)&env, &args);
		
		if ( result != 0 ) {
			fprintf(stderr, "[Fiji Java Error] Error starting up VM.\n");
			exit(result);
			return NULL;
		}		
		
		
		/* Find the main class */
		string slashed(main_class);
		replace(slashed.begin(), slashed.end(), '.', '/');
		jclass mainClass = env->FindClass(slashed.c_str());
		if ( mainClass == NULL ) {
			env->ExceptionDescribe();
			result = -1;
			goto leave;
		}
		
		/* Get the application's main method */
		jmethodID mainID = env->GetStaticMethodID(mainClass, "main",
													 "([Ljava/lang/String;)V");
		if (mainID == NULL) {
			if (env->ExceptionOccurred()) {
				env->ExceptionDescribe();
			} else {
				fprintf(stderr, "[Fiji Java Error] No main method found in specified class.\n");
			}
			result = -1;
			goto leave;
		}
		
		/* Build argument array */
		jobjectArray mainArgs = prepare_ij_options(env, options.ij_options);
		if (mainArgs == nil) {
			env->ExceptionDescribe();
			goto leave;
		}
		
		
		cerr << "About to call system JVM for OSX only...\n"; // debugging, should be in syslog file
		
		/* Invoke main method passing in the argument object. */
		env->CallStaticVoidMethod(mainClass, mainID, mainArgs);
		if (env->ExceptionOccurred()) {
			env->ExceptionDescribe();
			result = -1;
			goto leave;
		}
	
	leave:
		vm->DestroyJavaVM();
		exit(result);
		return NULL;

#else
		/*
		 Code for the remaining platforms
		 */
		
		add_option(options, "ij.ImageJ", 0);
		append_string_array(options.java_options, options.ij_options);
		append_string(options.java_options, NULL);
		prepend_string(options.java_options, "java");
		
		if (execvp("java", options.java_options.list))
			cerr << "Could not launch system-wide Java" << endl;
		exit(1);
		
#endif	
		
	}
	return NULL;
}

#ifdef MACOSX
/* MacOSX needs to run Java in a new thread, AppKit in the main thread. */

static void dummy_call_back(void *info) {}

static void start_ij_macosx(void *dummy)
{
	/* set the Application's name */
	char name[32];
	sprintf(name, "APP_NAME_%ld", (long)getpid());
	setenv(name, "Fiji", 1);
	cerr << "AppName: " << name << "\n";

	/* set the Dock icon */
	string icon = "APP_ICON_";
	icon += (name + 9);
	string icon_path = fiji_dir;
	// check if we're launched from within an Application bundle or command line
	// If from a bundle, Fiji.app should be in the path. 
	if ( icon_path.find("Fiji.app") == string::npos ) {
		// TODO: use getcwd(ptr, ptr_size) to get absolute directory in this case
		icon_path += "/images/Fiji.icns";
	} else {
		icon_path += "/../Resources/Fiji.icns";
	}
	setenv(strdup(icon.c_str()), strdup(icon_path.c_str()), 1);
	cerr << "AppIcon: " << icon.c_str() << "=" << icon_path.c_str() << "\n";

	
	pthread_t thread;
	pthread_attr_t attr;
	pthread_attr_init(&attr);
	pthread_attr_setscope(&attr, PTHREAD_SCOPE_SYSTEM);
	pthread_attr_setdetachstate(&attr, PTHREAD_CREATE_DETACHED);

	/* Start the thread that we will start the JVM on. */
	pthread_create(&thread, &attr, start_ij, NULL);
	pthread_attr_destroy(&attr);

	CFRunLoopSourceContext context;
	memset(&context, 0, sizeof(context));
	context.perform = &dummy_call_back;

	CFRunLoopSourceRef ref = CFRunLoopSourceCreate(NULL, 0, &context);
	CFRunLoopAddSource (CFRunLoopGetCurrent(), ref, kCFRunLoopCommonModes); 
	CFRunLoopRun();
}
#define start_ij start_ij_macosx
#endif

int main(int argc, char **argv, char **e)
{
	fiji_dir = get_fiji_dir(argv[0]);
	main_argv = argv;
	main_argc = argc;
	start_ij(NULL);
	return 0;
}

