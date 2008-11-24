import com.sun.jna.NativeLong;

import com.sun.jna.examples.unix.X11;

import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;

public class X11ShowEvents {
	protected final X11 x11 = X11.INSTANCE;
	protected X11.Display display;
	protected X11.Window root;
	protected X11.XSetWindowAttributes attributes;

	public X11ShowEvents() {
		// make sure that the JAWT library is loaded
		// TODO: only 32-bit? System.loadLibrary("jawt");
		long mask = X11.KeyPressMask | X11.KeyReleaseMask |
			X11.ButtonPressMask | X11.ButtonReleaseMask |
			X11.PointerMotionMask;
		attributes = new X11.XSetWindowAttributes();
		attributes.event_mask = new NativeLong(mask);
	}

	public void listenOnWindow(X11.Window window) {
		X11.XWindowAttributes original = new X11.XWindowAttributes();
		long event_mask = attributes.event_mask.longValue();
		x11.XGetWindowAttributes(display, window, original);
		if ((original.all_event_masks.longValue() & X11.ButtonPressMask)
				!= 0)
			event_mask &= ~X11.ButtonPressMask;
		event_mask &= ~X11.SubstructureRedirectMask;
		x11.XSelectInput(display, window, new NativeLong(event_mask));
	}

	public void listenOnWindowRecursively(X11.Window window) {
		listenOnWindow(window);

		X11.WindowByReference root = new X11.WindowByReference();
		X11.WindowByReference parent = new X11.WindowByReference();
		PointerByReference children = new PointerByReference();
		IntByReference childrenCount = new IntByReference();
		if (x11.XQueryTree(display, window, root, parent,
				children, childrenCount) == 0)
			return;
		if (childrenCount.getValue() == 0)
			return;
		// we cheat here; we know that a "Window" is a "long".
		long[] longs = new long[childrenCount.getValue()];
		children.getValue().read(0, longs, 0, longs.length);
		for (int i = 0; i < longs.length; i++)
			listenOnWindowRecursively(new X11.Window(longs[i]));
	}

	public void listen() {
		display = x11.XOpenDisplay(null);
		root = x11.XDefaultRootWindow(display);

		listenOnWindowRecursively(root);
		// TODO: listen for Creation and Destroy events (and listen/unlisten)
		// TODO: add KeyboardListener and MouseListener listeners

		X11.XEvent event = new X11.XEvent();
		for (int i = 0; i < 100; i++) {
			x11.XNextEvent(display, event);
			switch (event.type) {
			case X11.KeyPress:
			case X11.KeyRelease:
				event.setType(X11.XKeyEvent.class);
				System.err.println("key " + (event.type == X11.KeyPress ? "press" : "release") + ": " + event.xkey.keycode + ", " + event.xkey.state + ", (" + event.xkey.x + ", " + event.xkey.y + "), ("  + event.xkey.x_root + ", " + event.xkey.y_root + ") " + x11.XKeycodeToKeysym(display, (byte)event.xkey.keycode, event.xkey.state));
				break;
			case X11.ButtonPress:
			case X11.ButtonRelease:
				event.setType(X11.XButtonEvent.class);
				System.err.println("button " + (event.type == X11.ButtonPress ? "press" : "release") + ": (" + event.xbutton.x + ", " + event.xbutton.y + "), (" + event.xbutton.x_root + ", " + event.xbutton.y_root + ")");
				break;
			case X11.MotionNotify:
				event.setType(X11.XMotionEvent.class);
				System.err.println("motion: (" + event.xmotion.x + ", " + event.xmotion.y + "), (" + event.xmotion.x_root + ", " + event.xmotion.y_root + ")");
				break;
			default:
				System.err.println("nope");
				break;
			}
		}
	}

	public static void main(String[] args) {
		new X11ShowEvents().listen();
	}
}
