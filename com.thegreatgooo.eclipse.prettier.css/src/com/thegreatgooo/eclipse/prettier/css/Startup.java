package com.thegreatgooo.eclipse.prettier.css;

import org.eclipse.core.commands.operations.OperationHistoryFactory;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.IWindowListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.texteditor.ITextEditor;

import com.thegreatgooo.eclipse.prettier.css.handlers.FormattingListener;

public class Startup implements IStartup {

	@Override
	public void earlyStartup() {
		FormattingListener formattingListener = new FormattingListener();
		Display.getDefault().asyncExec(() -> {
			PlatformUI.getWorkbench().addWindowListener(new IWindowListener() {

				@Override
				public void windowOpened(IWorkbenchWindow window) {
					registerWindow(window, formattingListener);
				}

				@Override
				public void windowDeactivated(IWorkbenchWindow window) {
				}

				@Override
				public void windowClosed(IWorkbenchWindow window) {

				}

				@Override
				public void windowActivated(IWorkbenchWindow window) {
				}
			});
			OperationHistoryFactory.getOperationHistory().addOperationHistoryListener(formattingListener);
			IWorkbenchWindow[] windows = PlatformUI.getWorkbench().getWorkbenchWindows();
			for (IWorkbenchWindow iWorkbenchWindow : windows) {
				registerWindow(iWorkbenchWindow, formattingListener);
				for (IWorkbenchPage workbenchPage : iWorkbenchWindow.getPages()) {
					for (IEditorReference editorReference : workbenchPage.getEditorReferences()) {
						registerListenersForPart(formattingListener, editorReference.getEditor(false));
					}
				}
			}
		});
	}

	private void registerWindow(IWorkbenchWindow window, FormattingListener formattingListener) {
		IWorkbenchPage activePage = window.getActivePage();
		activePage.addPartListener(new IPartListener2() {
			public void partHidden(IWorkbenchPartReference partRef) {
				unRegisterListenersForPart(formattingListener, partRef.getPart(false));
			}

			public void partVisible(IWorkbenchPartReference partRef) {
				registerListenersForPart(formattingListener, partRef.getPart(false));
			}
		});
	}

	private void registerListenersForPart(FormattingListener formattingListener, IWorkbenchPart workbenchPart) {
		if (workbenchPart instanceof IEditorPart
				&& isSupportedExtension(((IEditorPart) workbenchPart).getEditorInput())) {
			IEditorPart editorPart = (IEditorPart) workbenchPart;
			IEditorInput input = editorPart.getEditorInput();
			if (editorPart instanceof ITextEditor && input instanceof FileEditorInput) {
				formattingListener.registerEditor((ITextEditor) editorPart);
			}
		}
	}

	private void unRegisterListenersForPart(FormattingListener formattingListener, IWorkbenchPart workbenchPart) {
		if (workbenchPart instanceof IEditorPart) {
			IEditorPart editorPart = (IEditorPart) workbenchPart;
			IEditorInput input = editorPart.getEditorInput();
			if (editorPart instanceof ITextEditor && input instanceof FileEditorInput) {
				formattingListener.unRegisterEditor((ITextEditor) editorPart);
			}
		}
	}

	private boolean isSupportedExtension(IEditorInput editorInput) {
		return editorInput instanceof FileEditorInput
				&& "css".equals(((FileEditorInput) editorInput).getFile().getFileExtension());
	}
}
