package org.imagearchive.lsm.toolbox;

import ij.ImagePlus;
import ij.macro.ExtensionDescriptor;
import ij.macro.MacroExtension;

public class LSMMacroExtension implements MacroExtension {
	private ExtensionDescriptor[] extensions = { ExtensionDescriptor
			.newDescriptor("lsm2xml", this, ARG_OUTPUT + ARG_NUMBER) };

	public ExtensionDescriptor[] getExtensionFunctions() {
		return extensions;
	}

	public String handleExtension(String name, Object[] args) {

		if (name.equals("lsmXml")) {
			if (args[0] == null)
				return null;
			String xml = new DomXmlExporter().getXML((String) args[0], false);
			return xml;
		}
		if (name.equals("lsmOpen")) {
			if (args[0] == null)
				return null;
			ImagePlus imp = new Reader().open((String) args[0], false);
			if (imp == null)
				return null;
			imp.setPosition(1, 1, 1);
			imp.show();
			return null;
		}
		if (name.equals("lsmLoad")) {
			if (args[0] == null)
				return null;
			new Reader().open((String) args[0], false);
			return null;
		}
		return null;
	}

}
