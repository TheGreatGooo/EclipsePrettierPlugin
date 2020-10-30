package com.thegreatgooo.eclipse.prettier.css.handlers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.commands.operations.IOperationHistoryListener;
import org.eclipse.core.commands.operations.ObjectUndoContext;
import org.eclipse.core.commands.operations.OperationHistoryEvent;
import org.eclipse.jface.text.Document;
import org.eclipse.ui.texteditor.IElementStateListener;
import org.eclipse.ui.texteditor.ITextEditor;

import com.thegreatgooo.eclipse.prettier.PrettierBridge;
import com.thegreatgooo.eclipse.prettier.css.Activator;
import com.thegreatgooo.eclipse.prettier.css.preferences.PreferenceConstants;

public class FormattingListener implements IOperationHistoryListener {
	private static AtomicReference<Path> TEMP_NPM_DATA_DIRECTORY = new AtomicReference<>();
	private static AtomicReference<Path> BRIDGE_DIRECTORY = new AtomicReference<Path>();
	private static AtomicReference<Path> NODE_PATH = new AtomicReference<>();
	private static AtomicReference<Path> NPM_PATH = new AtomicReference<>();

	private String[] envVars;
	private final AtomicLong lastUndoStamp;
	private final AtomicLong formattingSaveStamp;
	private final Map<ITextEditor, IElementStateListener> elementStateListeners;
	private final PrettierBridge prettierBridge;
	private AtomicReference<Path> windowsKillPath = new AtomicReference<>();

	public FormattingListener() {
		initializeTempNpmDirectory();
		checkForChangedProperties();
		lastUndoStamp = new AtomicLong(-1);
		formattingSaveStamp = new AtomicLong(-1);
		elementStateListeners = new ConcurrentHashMap<>();
		String appData = "APPDATA=" + TEMP_NPM_DATA_DIRECTORY.get().toString();
		String path = "PATH=" + System.getenv("PATH") + (isWindowsOs() ? ";" : ":")
				+ NODE_PATH.get().getParent().toString();
		envVars = new String[2];
		envVars[0] = appData;
		envVars[1] = path;
		prettierBridge = new PrettierBridge(BRIDGE_DIRECTORY, NODE_PATH.get(), NPM_PATH.get(), envVars, windowsKillPath,
				"com.thegreatgooo.eclipse.prettier.css");
	}

	public void registerEditor(ITextEditor textEditor) {
		textEditor.getDocumentProvider()
				.addElementStateListener(elementStateListeners.computeIfAbsent(textEditor,
						textEditorKey -> new ElementStateListener(textEditorKey, lastUndoStamp, formattingSaveStamp,
								prettierBridge)));
	}

	public void unRegisterEditor(ITextEditor textEditor) {
		IElementStateListener elementStateListener = elementStateListeners.remove(textEditor);
		if (elementStateListener != null) {
			textEditor.getDocumentProvider().removeElementStateListener(elementStateListener);
		}
	}

	@Override
	public void historyNotification(OperationHistoryEvent event) {
		if (OperationHistoryEvent.ABOUT_TO_UNDO == event.getEventType()) {
			lastUndoStamp.set(((Document) ((ObjectUndoContext) event.getOperation().getContexts()[0]).getObject())
					.getModificationStamp());
		}
	}

	private boolean isWindowsOs() {
		return System.getProperty("os.name").toLowerCase().contains("win");
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
		if (!NODE_PATH.get()
				.equals(Path.of(Activator.getDefault().getPreferenceStore().getString(PreferenceConstants.P_NODE_PATH)))
				|| !NPM_PATH.get().equals(Path
						.of(Activator.getDefault().getPreferenceStore().getString(PreferenceConstants.P_NPM_PATH)))) {
			NODE_PATH.set(
					Path.of(Activator.getDefault().getPreferenceStore().getString(PreferenceConstants.P_NODE_PATH)));
			NPM_PATH.set(
					Path.of(Activator.getDefault().getPreferenceStore().getString(PreferenceConstants.P_NPM_PATH)));
		}
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
