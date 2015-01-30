package viewer;

import java.io.File;

import javax.swing.filechooser.FileFilter;

public class DirFilter extends FileFilter {

	@Override
	public boolean accept(File f) {
		return f.isDirectory();
	}

	@Override
	public String getDescription() {
		return null;
	}

}
