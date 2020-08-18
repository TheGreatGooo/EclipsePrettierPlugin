package com.thegreatgooo.eclipse.prettier.preferences;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

import com.thegreatgooo.eclipse.prettier.Activator;

/**
 * Class used to initialize default preference values.
 */
public class PreferenceInitializer extends AbstractPreferenceInitializer {

	private static final String C_PROGRAM_FILES_NODEJS_NPM_CMD = "C:\\Program Files\\nodejs\\npm.cmd";
	private static final String C_PROGRAM_FILES_NODEJS_NODE_EXE = "C:\\Program Files\\nodejs\\node.exe";

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
				Process whichNode = Runtime.getRuntime().exec("which node");
				whichNode.waitFor(100, TimeUnit.MILLISECONDS);
				String nodePath = new String(whichNode.getInputStream().readAllBytes());
				if (nodePath.isBlank()) {
					store.setDefault(PreferenceConstants.P_NODE_PATH, C_PROGRAM_FILES_NODEJS_NODE_EXE);
				} else {
					store.setDefault(PreferenceConstants.P_NODE_PATH, nodePath);
				}
			} catch (IOException | InterruptedException e) {
				store.setDefault(PreferenceConstants.P_NODE_PATH, C_PROGRAM_FILES_NODEJS_NODE_EXE);
			}
			try {
				Process whichNpm = Runtime.getRuntime().exec("which npm");
				whichNpm.waitFor(100, TimeUnit.MILLISECONDS);
				String npmPath = new String(whichNpm.getInputStream().readAllBytes());
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
