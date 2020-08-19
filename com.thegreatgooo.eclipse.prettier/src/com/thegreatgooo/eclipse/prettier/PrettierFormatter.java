package com.thegreatgooo.eclipse.prettier;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.jdt.core.formatter.CodeFormatter;
import org.eclipse.jface.text.IRegion;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;

import com.thegreatgooo.eclipse.prettier.preferences.PreferenceConstants;

public class PrettierFormatter extends CodeFormatter {

	private static ConcurrentLinkedDeque<PrettierBridge> AVAILABLE_PRETTIER_BRIDGES = new ConcurrentLinkedDeque<>();
	private static AtomicReference<Path> TEMP_NPM_DATA_DIRECTORY = new AtomicReference<>();
	private static AtomicReference<Path> BRIDGE_DIRECTORY = new AtomicReference<Path>();
	private static AtomicReference<Path> NODE_PATH = new AtomicReference<>();
	private static AtomicReference<Path> NPM_PATH = new AtomicReference<>();
	private static AtomicReference<String> TAB_WIDTH = new AtomicReference<>();

	private String[] envVars;
	private AtomicReference<Path> windowsKillPath = new AtomicReference<>();

	public PrettierFormatter() {
		initializeTempNpmDirectory();
		checkForChangedProperties();
		String appData = "APPDATA=" + TEMP_NPM_DATA_DIRECTORY.get().toString();
		String path = "PATH=" + System.getenv("PATH") + (isWindowsOs() ? ";" : ":")
				+ NODE_PATH.get().getParent().toString();
		envVars = new String[2];
		envVars[0] = appData;
		envVars[1] = path;
	}

	@Override
	public TextEdit format(int kind, String source, int offset, int length, int indentationLevel,
			String lineSeparator) {
		checkForChangedProperties();
		boolean errorInPrettierProcess = false;
		PrettierBridge prettierBridge = AVAILABLE_PRETTIER_BRIDGES.poll();
		try {
			if (prettierBridge == null) {
				prettierBridge = new PrettierBridge(BRIDGE_DIRECTORY, NODE_PATH.get(), NPM_PATH.get(), envVars,
						windowsKillPath);
			}
			String formattedCode = prettierBridge.getFormattedCode(source);
			return new ReplaceEdit(0, source.length(), formattedCode);
		} catch (Exception e) {
			errorInPrettierProcess = true;
			prettierBridge.close();
			throw new RuntimeException(e);
		} finally {
			if (prettierBridge != null && errorInPrettierProcess == false)
				AVAILABLE_PRETTIER_BRIDGES.add(prettierBridge);
		}
	}

	@Override
	public TextEdit format(int kind, String source, IRegion[] regions, int indentationLevel, String lineSeparator) {
		return format(kind, source, 0, -1, -1, "");
	}

	private void checkForChangedProperties() {
		if (NODE_PATH.get() == null) {
			NODE_PATH.set(
					Path.of(Activator.getDefault().getPreferenceStore().getString(PreferenceConstants.P_NODE_PATH)));
		}
		if (NPM_PATH.get() == null) {
			NPM_PATH.set(
					Path.of(Activator.getDefault().getPreferenceStore().getString(PreferenceConstants.P_NPM_PATH)));
		}
		if (TAB_WIDTH.get() == null) {
			TAB_WIDTH.set(Activator.getDefault().getPreferenceStore().getString(PreferenceConstants.P_TAB_WIDTH));
		}
		if (!NODE_PATH.get()
				.equals(Path.of(Activator.getDefault().getPreferenceStore().getString(PreferenceConstants.P_NODE_PATH)))
				|| !TAB_WIDTH.get()
						.equals(Activator.getDefault().getPreferenceStore().getString(PreferenceConstants.P_TAB_WIDTH))
				|| !NPM_PATH.get().equals(Path
						.of(Activator.getDefault().getPreferenceStore().getString(PreferenceConstants.P_NPM_PATH)))) {
			NODE_PATH.set(
					Path.of(Activator.getDefault().getPreferenceStore().getString(PreferenceConstants.P_NODE_PATH)));
			TAB_WIDTH.set(Activator.getDefault().getPreferenceStore().getString(PreferenceConstants.P_TAB_WIDTH));
			NPM_PATH.set(
					Path.of(Activator.getDefault().getPreferenceStore().getString(PreferenceConstants.P_NPM_PATH)));
			// purge all cached instances of available bridges
			while (AVAILABLE_PRETTIER_BRIDGES.size() > 0) {
				AVAILABLE_PRETTIER_BRIDGES.poll().close();
			}
		}
	}

	private boolean isWindowsOs() {
		return System.getProperty("os.name").toLowerCase().contains("win");
	}

	private static void initializeTempNpmDirectory() {
		if (TEMP_NPM_DATA_DIRECTORY.get() == null) {
			synchronized (TEMP_NPM_DATA_DIRECTORY) {
				try {
					TEMP_NPM_DATA_DIRECTORY.set(Files.createTempDirectory("npm-data"));
				} catch (IOException e) {
					throw new RuntimeException("Could not create temporary directory", e);
				}
			}
		}
	}

}
