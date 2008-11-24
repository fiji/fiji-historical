import com.sun.jna.Callback;
import com.sun.jna.Library;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;

import com.sun.jna.examples.unix.X11;

import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;

public interface XRecord extends Library {
	final int XRecordBadContext = 0; /* Not a valid RC */

	/* Constants for arguments of various requests */
	final int XRecordFromServerTime = 0x01;
	final int XRecordFromClientTime = 0x02;
	final int XRecordFromClientSequence = 0x04;

	final int XRecordCurrentClients = 1;
	final int XRecordFutureClients = 2;
	final int XRecordAllClients = 3;

	final int XRecordFromServer = 0;
	final int XRecordFromClient = 1;
	final int XRecordClientStarted = 2;
	final int XRecordClientDied = 3;
	final int XRecordStartOfData = 4;
	final int XRecordEndOfData = 5;

	public class XRecordClientSpec extends NativeLong {
		public XRecordClientSpec() { }
		public XRecordClientSpec(long value) { super(value); }
	}

	public class XRecordContext extends NativeLong {
		public XRecordContext() { }
		public XRecordContext(long value) { super(value); }
	}

	public class XRecordRange8 extends Structure {
		byte first;
		byte last;
	}

	public class XRecordRange16 extends Structure {
		short first;
		short last;
	}

	public class XRecordExtRange extends Structure {
		XRecordRange8	ext_major;
		XRecordRange16 	ext_minor;
	}

	public class XRecordRange {
		XRecordRange8     core_requests; /* core X requests */
		XRecordRange8     core_replies;	/* core X replies */
		XRecordExtRange   ext_requests;	/* extension requests */
		XRecordExtRange   ext_replies;	/* extension replies */
		XRecordRange8     delivered_events; /* delivered core and ext events */
		XRecordRange8     device_events; /* all core and ext device events */
		XRecordRange8     errors;	/* core X and ext errors */
		boolean	      client_started;	/* connection setup reply */
		boolean              client_died; /* notice of client disconnect */
	}

	public class XRecordClientInfo extends Structure {
		XRecordClientSpec client;
		NativeLong nranges;
		Pointer ranges;
	}

	public class XRecordState extends Structure {
		boolean enabled;
		int datum_flags;
		NativeLong nclients;
		Pointer client_info;
	}

	public class XRecordInterceptData {
		NativeLong id_base;
		NativeLong server_time;
		NativeLong client_seq;
		int category;
		boolean client_swapped;
		Pointer data;
		NativeLong data_len;	/* in 4-byte units */
	}

	NativeLong XRecordIdBaseMask(X11.Display dpy);

	int XRecordQueryVersion(X11.Display dpy, IntByReference cmajor_return,
			IntByReference cminor_return);

	XRecordContext XRecordCreateContext(X11.Display dpy, int datum_flags,
			Pointer clients, int nclients, Pointer ranges,
			int nranges);

	Pointer XRecordAllocRange();

	int XRecordRegisterClients(X11.Display dpy, XRecordContext context,
			int datum_flags, Pointer clients, int nclients,
			Pointer ranges, int nranges);

	int XRecordUnregisterClients(X11.Display dpy, XRecordContext context,
			Pointer clients, int nclients);

	int XRecordGetContext(X11.Display dpy, XRecordContext context,
			Pointer state_return);

	void XRecordFreeState(Pointer state); 

	public interface XRecordInterceptProc extends Callback {
		void callback(Pointer closure, Pointer recorded_data);
	}

	int XRecordEnableContext(X11.Display dpy, XRecordContext context,
			XRecordInterceptProc callback, Pointer closure);

	int XRecordEnableContextAsync(X11.Display dpy, XRecordContext context,
			XRecordInterceptProc callback, Pointer closure);

	void XRecordProcessReplies(X11.Display dpy);

	void XRecordFreeData(Pointer data);

	int XRecordDisableContext(X11.Display dpy, XRecordContext context);

	int XRecordFreeContext(X11.Display dpy, XRecordContext context);
}
