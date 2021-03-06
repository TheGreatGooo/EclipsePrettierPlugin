package com.thegreatgooo.eclipse.prettier.css.preferences;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

import com.thegreatgooo.eclipse.prettier.css.Activator;

/**
 * Class used to initialize default preference values.
 */
public class PreferenceInitializer extends AbstractPreferenceInitializer {

	private static final String C_PROGRAM_FILES_NODEJS_NPM_CMD = "C:\\Program Files\\nodejs\\npm.cmd";
	private static final String C_PROGRAM_FILES_NODEJS_NODE_EXE = "C:\\Program Files\\nodejs\\node.exe";
	private static final String[] WHICH_NODE = { "which", "node" };
	private static final String[] WHICH_NPM = { "which", "npm" };

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer#
	 * initializeDefaultPreferences()
	 */
	public void initializeDefaultPreferences() {
		IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		if (!isWindowsOs()) {
			try {
				Process whichNode = Runtime.getRuntime().exec(WHICH_NODE);
				whichNode.waitFor(100, TimeUnit.MILLISECONDS);
				String nodePath = new String(whichNode.getInputStream().readAllBytes()).replaceAll("\n", "");
				if (nodePath.isBlank()) {
					store.setDefault(PreferenceConstants.P_NODE_PATH, C_PROGRAM_FILES_NODEJS_NODE_EXE);
				} else {
					store.setDefault(PreferenceConstants.P_NODE_PATH, nodePath);
				}
			} catch (IOException | InterruptedException e) {
				store.setDefault(PreferenceConstants.P_NODE_PATH, C_PROGRAM_FILES_NODEJS_NODE_EXE);
			}
			try {
				Process whichNpm = Runtime.getRuntime().exec(WHICH_NPM);
				whichNpm.waitFor(100, TimeUnit.MILLISECONDS);
				String npmPath = new String(whichNpm.getInputStream().readAllBytes()).replaceAll("\n", "");
				if (npmPath.isBlank()) {
					store.setDefault(PreferenceConstants.P_NPM_PATH, C_PROGRAM_FILES_NODEJS_NPM_CMD);
				} else {
					store.setDefault(PreferenceConstants.P_NPM_PATH, npmPath);
				}
			} catch (IOException | InterruptedException e) {
				store.setDefault(PreferenceConstants.P_NPM_PATH, C_PROGRAM_FILES_NODEJS_NPM_CMD);
			}
		} else {
			store.setDefault(PreferenceConstants.P_NODE_PATH, C_PROGRAM_FILES_NODEJS_NODE_EXE);
			store.setDefault(PreferenceConstants.P_NPM_PATH, C_PROGRAM_FILES_NODEJS_NPM_CMD);
		}
		store.setDefault(PreferenceConstants.P_TAB_WIDTH, "2");
	}

	private boolean isWindowsOs() {
		return System.getProperty("os.name").toLowerCase().contains("win");
	}
}
